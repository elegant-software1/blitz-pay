package com.elegant.software.blitzpay.payments.push.internal

import com.elegant.software.blitzpay.payments.push.api.PaymentStatusChanged
import com.elegant.software.blitzpay.payments.push.api.PaymentStatusCode
import com.elegant.software.blitzpay.payments.push.api.PaymentStatusUpdateGateway
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class PaymentStatusUpdateGatewayImpl(
    private val paymentStatusService: PaymentStatusService,
    private val eventPublisher: ApplicationEventPublisher,
) : PaymentStatusUpdateGateway {

    @Transactional
    override fun settle(paymentRequestId: UUID, sourceEventId: String, occurredAt: Instant) =
        applyAndPublish(paymentRequestId, PaymentStatusCode.SETTLED, sourceEventId, occurredAt)

    @Transactional
    override fun fail(paymentRequestId: UUID, sourceEventId: String, occurredAt: Instant) =
        applyAndPublish(paymentRequestId, PaymentStatusCode.FAILED, sourceEventId, occurredAt)

    private fun applyAndPublish(paymentRequestId: UUID, status: PaymentStatusCode, sourceEventId: String, occurredAt: Instant) {
        val transition = paymentStatusService.apply(paymentRequestId, status, occurredAt, sourceEventId)
        if (transition.changed) {
            eventPublisher.publishEvent(
                PaymentStatusChanged(
                    paymentRequestId = transition.paymentRequestId,
                    newStatus = transition.newStatus,
                    previousStatus = transition.previousStatus,
                    occurredAt = transition.occurredAt,
                    sourceEventId = transition.sourceEventId,
                )
            )
        }
    }
}
