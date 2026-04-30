package com.elegant.software.blitzpay.order.application

import com.elegant.software.blitzpay.order.domain.OrderStatus
import com.elegant.software.blitzpay.order.repository.OrderRepository
import com.elegant.software.blitzpay.order.repository.PaymentAttemptRepository
import com.elegant.software.blitzpay.payments.push.api.PaymentStatusChanged
import com.elegant.software.blitzpay.payments.push.api.PaymentStatusCode
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderStatusProjection(
    private val orderRepository: OrderRepository,
    private val paymentAttemptRepository: PaymentAttemptRepository,
) {
    @ApplicationModuleListener
    fun on(event: PaymentStatusChanged) {
        val attempt = paymentAttemptRepository.findByPaymentRequestId(event.paymentRequestId) ?: return
        val order = orderRepository.findById(attempt.orderIdFk).orElse(null) ?: return
        attempt.update(status = event.newStatus.name, at = event.occurredAt)
        paymentAttemptRepository.save(attempt)
        val nextStatus = when (event.newStatus) {
            PaymentStatusCode.PENDING -> OrderStatus.PENDING_PAYMENT
            PaymentStatusCode.EXECUTED -> OrderStatus.PAYMENT_IN_PROGRESS
            PaymentStatusCode.SETTLED -> OrderStatus.PAID
            PaymentStatusCode.FAILED, PaymentStatusCode.EXPIRED -> OrderStatus.PAYMENT_FAILED
        }
        if (order.applyPaymentUpdate(nextStatus, event.paymentRequestId, attempt.provider, event.occurredAt)) {
            orderRepository.save(order)
        }
    }
}
