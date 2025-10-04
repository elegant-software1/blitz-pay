package com.elegant.software.quickpay.payments.qrpay

import com.elegant.software.quickpay.payments.support.PaymentUpdateBus
import com.elegant.software.quickpay.payments.truelayer.api.PaymentRequested
import com.elegant.software.quickpay.payments.truelayer.api.PaymentResult
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/payments")
class PaymentRequestController(private val eventPublisher: ApplicationEventPublisher,
    private val paymentUpdateBus: PaymentUpdateBus) {
    @PostMapping("/request")
    fun createPaymentRequest(@RequestBody request: PaymentRequested): ResponseEntity<Map<String, String>> {
        val paymentRequestId=UUID.randomUUID()
        request.paymentRequestId=paymentRequestId
        eventPublisher.publishEvent(request)
        paymentUpdateBus.emit(paymentRequestId, PaymentResult(paymentRequestId = paymentRequestId,orderId =request.orderId))
        return ResponseEntity.accepted().body(mapOf("paymentRequestId" to paymentRequestId.toString()))
    }


}