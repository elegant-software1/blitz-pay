package com.elegant.software.blitzpay.payments.truelayer.outbound

import com.elegant.software.blitzpay.payments.qrpay.QrPaymentUpdateBus
import com.elegant.software.blitzpay.payments.truelayer.api.*
import com.elegant.software.blitzpay.payments.truelayer.support.QrCodeGenerator
import com.elegant.software.blitzpay.payments.truelayer.support.QrCodeProperties
import com.elegant.software.blitzpay.payments.truelayer.support.TrueLayerProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.truelayer.signing.Signer
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class QrPaymentService(
    private val qrCodeGenerator: QrCodeGenerator,
    private val qrPaymentUpdateBus: QrPaymentUpdateBus,
    private val trueLayerTokenService: TrueLayerTokenService,
    private val webClient: WebClient,
    private val trueLayerProperties: TrueLayerProperties,
    private val qrCodeProperties: QrCodeProperties
) {
    private val logger = KotlinLogging.logger {}
    private val qrPayments = ConcurrentHashMap<UUID, QrPaymentResponse>()

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private val PAYMENT_LINKS_URL = "https://api.truelayer-sandbox.com/v3/payment-links"

    fun initiateQrPayment(request: QrPaymentRequest): QrPaymentResponse {
        logger.info { "=== INITIATING QR PAYMENT ===" }
        logger.info { "Request: $request" }

        val paymentRequestId = UUID.randomUUID()

        return try {
            logger.info { "Step 1: Creating TrueLayer payment link..." }
            val paymentLink = try {
                createTrueLayerPaymentLink(paymentRequestId, request)
            } catch (e: Exception) {
                logger.error(e) { "FAILED in createTrueLayerPaymentLink: ${e.javaClass.simpleName} - ${e.message}" }
                throw e
            }

            logger.info { "✅ TrueLayer Payment Link Created: ID=${paymentLink.id}, URI=${paymentLink.uri}" }

            logger.info { "Step 2: Generating QR code..." }
            val qrResult = qrCodeGenerator.generatePaymentQRCode(
                paymentRequestId = paymentRequestId,
                amount = request.amount.toString(),
                merchant = request.merchant,
                currency = request.currency,
                paymentUrl = paymentLink.uri
            )
            logger.info { "✅ QR Code Generated" }

            val qrCodeUrl = "${getBaseUrl()}/api/qr-payments/$paymentRequestId/image"

            logger.info { "Step 3: Building response..." }
            val response = buildQrPaymentResponse(paymentRequestId, request, paymentLink, qrResult, qrCodeUrl)

            qrPayments[paymentRequestId] = response

            publishToBus(
                paymentRequestId, QrPaymentStatus.INITIATED, paymentLink.uri, createDeepLink(paymentLink.id)
            )

            logger.info { "✅ QR payment initiated: $paymentRequestId, TrueLayer ID: ${paymentLink.id}" }
            response

        } catch (e: Exception) {
            logger.error(e) { "❌ CRITICAL ERROR in initiateQrPayment" }
            createErrorResponse(
                paymentRequestId = paymentRequestId,
                error = "Failed to create payment link: ${e.message ?: "Unknown error"}"
            )
        }
    }

    private fun createTrueLayerPaymentLink(paymentRequestId: UUID, request: QrPaymentRequest): TrueLayerPaymentResponse {
        val accessToken = trueLayerTokenService.fetchToken()
        logger.debug { "TrueLayer access token -> $accessToken" }

        val trueLayerRequest = buildTrueLayerRequest(paymentRequestId, request)

        val mapper = jacksonObjectMapper()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        val requestJson = mapper.writeValueAsString(trueLayerRequest)

        val idempotencyKey = UUID.randomUUID().toString()

        logger.info { "Generating Tl-Signature for request..." }
        val pem = Files.readString(Path.of(trueLayerProperties.privateKeyPath))
        val tlSignature = Signer.from(trueLayerProperties.keyId, pem)
            .header("Idempotency-Key", idempotencyKey)
            .method("post")
            .path("/v3/payment-links")
            .body(requestJson)
            .sign()
        logger.info { "Request JSON (first 500 chars): ${requestJson.take(500)}" }
        logger.info { "Generated Tl-Signature: $tlSignature" }

        val response = webClient.post()
            .uri(PAYMENT_LINKS_URL)
            .header("Authorization", "Bearer $accessToken")
            .header("Idempotency-Key", idempotencyKey)
            .header("Tl-Signature", tlSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .retrieve()
            .onStatus({ it.is4xxClientError }) { clientResponse ->
                clientResponse.bodyToMono(String::class.java)
                    .doOnNext { body ->
                        logger.error {
                            "🔴 TrueLayer API Client Error (${clientResponse.statusCode()}): $body"
                        }
                    }
                    .flatMap { body ->
                        Mono.error(RuntimeException("TrueLayer API error ${clientResponse.statusCode()}: $body"))
                    }
            }
            .bodyToMono(TrueLayerPaymentResponse::class.java)
            .block()
            ?: throw RuntimeException("Null response from TrueLayer")

        logger.info { "Payment link created successfully: ${response.id}" }
        return response
    }

    private fun buildTrueLayerRequest(paymentRequestId: UUID, request: QrPaymentRequest): TrueLayerPaymentRequest {
        val amountInMinor = (request.amount * 100).toLong()
        val reference = generateReference(paymentRequestId, request)
        val expiresAt = Instant.now()
            .plusSeconds(24 * 60 * 60)
            .atZone(ZoneId.of("UTC"))
            .format(DATE_FORMATTER)
        val productItems = parseOrderItems(request.orderDetails, amountInMinor)

        return TrueLayerPaymentRequest(
            expiresAt = expiresAt,
            reference = reference,
            returnUri = qrCodeProperties.truelayer.redirectUri,
            paymentConfiguration = TrueLayerPaymentRequest.PaymentConfiguration(
                amountInMinor = amountInMinor,
                currency = request.currency.uppercase(),
                paymentMethod = TrueLayerPaymentRequest.PaymentMethod(
                    beneficiary = TrueLayerPaymentRequest.Beneficiary(
                        merchantAccountId = trueLayerProperties.merchantAccountId
                    )
                ),
                user = TrueLayerPaymentRequest.PaymentUser(
                    id = UUID.randomUUID().toString(),
                    name = "Coffee Shop Customer",
                    email = "customer@example.com"
                )
            ),
            productItems = productItems,
            type = "single_payment"
        )
    }

    private fun generateReference(paymentRequestId: UUID, request: QrPaymentRequest): String =
        "COFFEE_${paymentRequestId.toString().substring(0, 8).uppercase()}"

    private fun parseOrderItems(
        orderDetails: String, totalAmountInMinor: Long
    ): List<TrueLayerPaymentRequest.ProductItem> {
        val items = mutableListOf<TrueLayerPaymentRequest.ProductItem>()
        val parts = orderDetails.split(",").map { it.trim() }

        if (parts.isEmpty() || (parts.size == 1 && parts[0].isBlank())) {
            items.add(
                TrueLayerPaymentRequest.ProductItem(
                    name = "Coffee Order",
                    priceInMinor = totalAmountInMinor,
                    quantity = 1,
                    url = "https://your-coffee-shop.com"
                )
            )
        } else if (parts.size == 1 && !parts[0].contains(Regex("\\d"))) {
            val item = parts[0]
            items.add(
                TrueLayerPaymentRequest.ProductItem(
                    name = item,
                    priceInMinor = totalAmountInMinor,
                    quantity = 1,
                    url = "https://your-coffee-shop.com/menu/${item.lowercase().replace(" ", "-")}"
                )
            )
        } else {
            var remainingAmount = totalAmountInMinor
            parts.forEachIndexed { index, part ->
                val quantity = extractQuantity(part)
                val itemName = extractItemName(part)
                val pricePerItem = calculateItemPrice(itemName, quantity, index, parts.size, remainingAmount)
                items.add(
                    TrueLayerPaymentRequest.ProductItem(
                        name = itemName,
                        priceInMinor = pricePerItem,
                        quantity = quantity,
                        url = "https://your-coffee-shop.com/menu/${itemName.lowercase().replace(" ", "-")}"
                    )
                )
                remainingAmount -= pricePerItem * quantity
            }
        }

        return items
    }

    private fun extractQuantity(item: String): Int =
        Regex("(\\d+)\\s+.+").find(item)?.groupValues?.get(1)?.toIntOrNull() ?: 1

    private fun extractItemName(item: String): String =
        item.replace(Regex("^\\d+\\s*"), "").trim()

    private fun calculateItemPrice(
        itemName: String, quantity: Int, index: Int, totalItems: Int, remainingAmount: Long
    ): Long {
        if (index == totalItems - 1) return remainingAmount / quantity
        return when (itemName.lowercase()) {
            "latte" -> 400L
            "cappuccino" -> 350L
            "espresso" -> 250L
            "flat white" -> 380L
            "mocha" -> 420L
            "tea" -> 200L
            else -> 300L
        }
    }

    private fun buildQrPaymentResponse(
        paymentRequestId: UUID,
        request: QrPaymentRequest,
        paymentLink: TrueLayerPaymentResponse,
        qrResult: QrCodeGenerator.QrCodeResult,
        qrCodeUrl: String
    ): QrPaymentResponse = QrPaymentResponse(
        success = true,
        paymentRequestId = paymentRequestId,
        transactionId = paymentLink.id,
        status = QrPaymentStatus.INITIATED,
        qrCodeData = paymentLink.uri,
        qrCodeImage = qrResult.qrCodeImage,
        qrCodeUrl = qrCodeUrl,
        deepLink = createDeepLink(paymentLink.id),
        paymentUrl = paymentLink.uri,
        merchant = request.merchant,
        amount = request.amount,
        currency = request.currency,
        expiresAt = Instant.now().plusSeconds(24 * 60 * 60),
        message = "Scan QR code to pay ${request.amount} ${request.currency} at ${request.merchant}"
    )

    private fun createDeepLink(paymentLinkId: String): String = "truelayer://payment-link/$paymentLinkId"

    private fun getBaseUrl(): String = qrCodeProperties.server.baseUrl

    private fun createErrorResponse(paymentRequestId: UUID, error: String): QrPaymentResponse =
        QrPaymentResponse(
            success = false,
            paymentRequestId = paymentRequestId,
            transactionId = "ERROR-${paymentRequestId.toString().take(8)}",
            status = QrPaymentStatus.FAILED,
            error = error,
            expiresAt = Instant.now(),
            message = "Failed to create payment"
        )

    private fun publishToBus(
        paymentRequestId: UUID, status: QrPaymentStatus, qrCodeData: String? = null, deepLink: String? = null
    ) {
        val qrPaymentResult = QrPaymentResult(
            paymentRequestId = paymentRequestId,
            status = status.name,
            qrCodeData = qrCodeData,
            qrStatus = status.name,
            deepLink = deepLink,
            timestamp = Instant.now()
        )
        qrPaymentUpdateBus.emit(paymentRequestId, qrPaymentResult)
    }

    fun getQrImage(paymentRequestId: UUID): ByteArray? =
        qrPayments[paymentRequestId]?.qrCodeImage?.let { Base64.getDecoder().decode(it) }

    fun getQrPayment(paymentRequestId: UUID): QrPaymentResponse? = qrPayments[paymentRequestId]

    fun updatePaymentStatus(
        paymentRequestId: UUID, status: QrPaymentStatus, qrPaymentResult: QrPaymentResult? = null
    ) {
        val currentPayment = qrPayments[paymentRequestId] ?: return
        val updatedPayment = currentPayment.copy(
            status = status,
            message = when (status) {
                QrPaymentStatus.SCANNED -> "QR code scanned, processing payment..."
                QrPaymentStatus.PROCESSING -> "Payment being processed"
                QrPaymentStatus.SUCCESS -> "Payment successful"
                QrPaymentStatus.FAILED -> "Payment failed"
                QrPaymentStatus.EXPIRED -> "QR code expired"
                QrPaymentStatus.CANCELLED -> "Payment cancelled"
                else -> currentPayment.message
            }
        )
        qrPayments[paymentRequestId] = updatedPayment

        val busUpdate = qrPaymentResult ?: QrPaymentResult(
            paymentRequestId = paymentRequestId,
            status = status.name,
            qrCodeData = updatedPayment.qrCodeData,
            qrStatus = status.name,
            deepLink = updatedPayment.deepLink,
            timestamp = Instant.now()
        )
        qrPaymentUpdateBus.emit(paymentRequestId, busUpdate)
        logger.info { "Payment status updated: $paymentRequestId -> $status" }
    }

    fun handlePaymentResult(paymentRequestId: UUID, qrPaymentResult: QrPaymentResult) {
        val status = when (qrPaymentResult.status.lowercase()) {
            "executed", "settled", "completed", "success" -> QrPaymentStatus.SUCCESS
            "failed", "declined", "rejected", "cancelled" -> QrPaymentStatus.FAILED
            "pending", "processing" -> QrPaymentStatus.PROCESSING
            else -> QrPaymentStatus.PROCESSING
        }
        updatePaymentStatus(paymentRequestId, status, qrPaymentResult)
    }

    fun markQrScanned(paymentRequestId: UUID) = updatePaymentStatus(paymentRequestId, QrPaymentStatus.SCANNED)
    fun markQrProcessing(paymentRequestId: UUID) = updatePaymentStatus(paymentRequestId, QrPaymentStatus.PROCESSING)

    fun cleanupExpiredPayments() {
        val now = Instant.now()
        qrPayments.entries.removeAll { (paymentRequestId, payment) ->
            if (payment.expiresAt.isBefore(now)) {
                updatePaymentStatus(paymentRequestId, QrPaymentStatus.EXPIRED)
                true
            } else {
                false
            }
        }
    }

    companion object {
        fun createQuickPayment(
            merchant: String, amount: Double, orderDetails: String, currency: String = "GBP"
        ): QrPaymentRequest = QrPaymentRequest(
            merchant = merchant, amount = amount, currency = currency, orderDetails = orderDetails
        )
    }
}
