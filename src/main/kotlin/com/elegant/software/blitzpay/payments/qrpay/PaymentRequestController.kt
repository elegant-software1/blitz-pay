package com.elegant.software.blitzpay.payments.qrpay

import com.elegant.software.blitzpay.order.api.OrderGateway
import com.elegant.software.blitzpay.payments.push.api.PaymentStatusInitializationGateway
import com.elegant.software.blitzpay.payments.support.PaymentUpdateBus
import com.elegant.software.blitzpay.payments.truelayer.api.PaymentGateway
import com.elegant.software.blitzpay.payments.truelayer.api.PaymentRequested
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64
import java.util.*

@RestController
@RequestMapping("/{version:v\\d+(?:\\.\\d+)*}/payments", version = "1")

class PaymentRequestController(
    private val orderGateway: OrderGateway,
    private val paymentGateway: PaymentGateway,
    private val paymentUpdateBus: PaymentUpdateBus,
    private val paymentStatusInitializationGateway: PaymentStatusInitializationGateway,
) {
    @PostMapping("/request")
    fun createPaymentRequest(
        @RequestBody request: PaymentRequested,
        @RequestHeader(name = "Authorization", required = false) authorization: String?,
    ): ResponseEntity<Any> {
        val orderSummary = try {
            orderGateway.assertPayable(request.orderId)
        } catch (ex: NoSuchElementException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "Order not found")))
        } catch (ex: IllegalStateException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (ex.message ?: "Order is not payable")))
        }
        if (request.amountMinorUnits != orderSummary.totalAmountMinor || !request.currency.equals(orderSummary.currency, ignoreCase = true)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "amountMinorUnits and currency must match the order"))
        }

        val paymentRequestId = UUID.randomUUID()
        request.paymentRequestId = paymentRequestId
        request.payerRef = extractSubject(authorization)

        paymentStatusInitializationGateway.initialize(
            paymentRequestId = paymentRequestId,
            payerRef = request.payerRef,
            orderId = request.orderId,
            amountMinorUnits = request.amountMinorUnits,
            currency = request.currency,
        )
        orderGateway.linkPaymentAttempt(request.orderId, paymentRequestId, "TRUELAYER", null)

        val result = paymentGateway.startPayment(request)
        result.paymentId?.let {
            orderGateway.linkPaymentAttempt(request.orderId, paymentRequestId, "TRUELAYER", it)
        }
        paymentUpdateBus.emit(paymentRequestId, result)

        return ResponseEntity.accepted().body(
            mapOf(
                "paymentRequestId" to paymentRequestId.toString(),
                "paymentId" to result.paymentId,
                "resourceToken" to result.resourceToken,
                "redirectReturnUri" to (result.redirectReturnUri ?: request.redirectReturnUri)
            )
        )
    }

    private fun extractSubject(authorization: String?): String? {
        val bearerToken = authorization
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val parts = bearerToken.split('.')
        if (parts.size < 2) return null

        return runCatching {
            String(Base64.getUrlDecoder().decode(parts[1]))
        }.mapCatching { payload ->
            Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.getOrNull(1)
        }.getOrNull()?.takeIf { it.isNotBlank() }?.take(512)
    }
}
