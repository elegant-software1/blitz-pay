package com.elegant.software.blitzpay.payments.qrpay

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Tag(name = "QR Code Payments", description = "Generate and manage QR codes for payments")
@RestController
@RequestMapping("/api/qr-payments")
class QrCodeController(
    private val qrCodeGenerator: QrCodeGenerator
) {
    private val logger = KotlinLogging.logger {}
    private val qrPayments = ConcurrentHashMap<UUID, QrPaymentResponse>()

    @Operation(
        summary = "Generate a QR code payment",
        description = "Creates a new QR code payment with the given parameters and returns the QR code image as Base64"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "QR code generated successfully",
            content = [Content(schema = Schema(implementation = QrPaymentResponse::class))]),
        ApiResponse(responseCode = "500", description = "Failed to generate QR code")
    )
    @GetMapping("/quick")
    fun quickQrPayment(
        @Parameter(description = "Merchant name", required = true) @RequestParam merchant: String,
        @Parameter(description = "Payment amount", required = true) @RequestParam amount: Double,
        @Parameter(description = "Order reference", required = true) @RequestParam order: String,
        @Parameter(description = "Currency code (default GBP)") @RequestParam(required = false, defaultValue = "GBP") currency: String
    ): ResponseEntity<Any> {
        logger.info { "Quick QR payment: merchant=$merchant, amount=$amount, order=$order, currency=$currency" }

        return try {
            val paymentRequestId = UUID.randomUUID()
            val qrResult = qrCodeGenerator.generatePaymentQRCode(
                paymentRequestId = paymentRequestId,
                amount = amount.toString(),
                merchant = merchant,
                currency = currency
            )

            val qrCodeUrl = "/api/qr-payments/$paymentRequestId/image"
            val response = QrPaymentResponse(
                success = true,
                paymentRequestId = paymentRequestId,
                status = QrPaymentStatus.INITIATED,
                qrCodeImage = qrResult.qrCodeImage,
                qrCodeUrl = qrCodeUrl,
                deepLink = qrResult.deepLink,
                paymentUrl = qrResult.paymentUrl,
                merchant = merchant,
                amount = amount,
                currency = currency,
                expiresAt = Instant.now().plusSeconds(24 * 60 * 60),
                message = "Scan QR code to pay $amount $currency at $merchant"
            )

            qrPayments[paymentRequestId] = response
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create QR payment" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to create QR payment", "message" to e.message))
        }
    }

    @Operation(
        summary = "Get QR payment details",
        description = "Retrieve the details of a previously generated QR payment by its ID"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "QR payment found",
            content = [Content(schema = Schema(implementation = QrPaymentResponse::class))]),
        ApiResponse(responseCode = "404", description = "QR payment not found"),
        ApiResponse(responseCode = "400", description = "Invalid payment request ID")
    )
    @GetMapping("/{paymentRequestId}")
    fun getQrPayment(
        @Parameter(description = "Payment request UUID") @PathVariable paymentRequestId: String
    ): ResponseEntity<Any> {
        logger.info { "Get QR payment: $paymentRequestId" }
        return try {
            val uuid = UUID.fromString(paymentRequestId)
            qrPayments[uuid]?.let { ResponseEntity.ok(it as Any) }
                ?: ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Payment not found", "paymentRequestId" to paymentRequestId) as Any)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to "Invalid payment request ID") as Any)
        }
    }

    @Operation(
        summary = "Get QR code image",
        description = "Returns the QR code as a PNG image for the given payment request ID"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "QR code image",
            content = [Content(mediaType = "image/png")]),
        ApiResponse(responseCode = "404", description = "QR code image not found"),
        ApiResponse(responseCode = "400", description = "Invalid payment request ID")
    )
    @GetMapping("/{paymentRequestId}/image", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getQrImage(
        @Parameter(description = "Payment request UUID") @PathVariable paymentRequestId: String
    ): ResponseEntity<Any> {
        logger.info { "Get QR image: $paymentRequestId" }
        return try {
            val uuid = UUID.fromString(paymentRequestId)
            val payment = qrPayments[uuid]
            if (payment?.qrCodeImage != null) {
                val imageBytes = Base64.getDecoder().decode(payment.qrCodeImage)
                ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes as Any)
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "QR image not found", "paymentRequestId" to paymentRequestId) as Any)
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to "Invalid payment request ID") as Any)
        }
    }

    @Operation(
        summary = "QR payments health check",
        description = "Returns the health status of the QR payments service"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "qr-payments",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
