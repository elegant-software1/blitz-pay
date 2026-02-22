package com.elegant.software.blitzpay.payments.qrpay

import com.elegant.software.blitzpay.payments.truelayer.api.QrPaymentResult
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class QrPaymentUpdateBus {
    private val sinks = ConcurrentHashMap<UUID, Sinks.Many<QrPaymentResult>>()

    fun sink(paymentRequestId: UUID): Sinks.Many<QrPaymentResult> =
        sinks.computeIfAbsent(paymentRequestId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }

    fun emit(paymentRequestId: UUID, update: QrPaymentResult) {
        sink(paymentRequestId).tryEmitNext(update)
    }

    fun complete(paymentRequestId: UUID) {
        sinks.remove(paymentRequestId)?.tryEmitComplete()
    }
}
