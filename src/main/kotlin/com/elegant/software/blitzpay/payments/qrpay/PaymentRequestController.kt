package com.elegant.software.blitzpay.payments.qrpay

import com.elegant.software.blitzpay.payments.support.PaymentUpdateBus
import com.elegant.software.blitzpay.payments.truelayer.api.PaymentRequested
import com.elegant.software.blitzpay.payments.truelayer.api.PaymentResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@Tag(name = "QR Payments", description = "Operations related to QR payments")
@RestController
@RequestMapping("/payments")
class PaymentRequestController(
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentUpdateBus: PaymentUpdateBus
) {
    @Operation(
        summary = "Create a payment request",
        description = "Initiates a new payment request, publishes an event for TrueLayer processing, and returns the payment request ID"
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Payment request accepted"),
        ApiResponse(responseCode = "400", description = "Invalid request body")
    )
    @PostMapping("/request")
    fun createPaymentRequest(@RequestBody request: PaymentRequested): ResponseEntity<Map<String, String>> {
        val paymentRequestId = UUID.randomUUID()
        request.paymentRequestId = paymentRequestId
        eventPublisher.publishEvent(request)
        paymentUpdateBus.emit(paymentRequestId, PaymentResult(paymentRequestId = paymentRequestId, orderId = request.orderId))
        return ResponseEntity.accepted().body(mapOf("paymentRequestId" to paymentRequestId.toString()))
    }
}