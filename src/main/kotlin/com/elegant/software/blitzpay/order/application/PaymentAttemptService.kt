package com.elegant.software.blitzpay.order.application

import com.elegant.software.blitzpay.order.api.OrderGateway
import com.elegant.software.blitzpay.order.api.OrderPaymentSummary
import com.elegant.software.blitzpay.order.domain.OrderStatus
import com.elegant.software.blitzpay.order.domain.PaymentAttempt
import com.elegant.software.blitzpay.order.repository.OrderRepository
import com.elegant.software.blitzpay.order.repository.PaymentAttemptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class PaymentAttemptService(
    private val orderRepository: OrderRepository,
    private val paymentAttemptRepository: PaymentAttemptRepository,
) : OrderGateway {
    override fun assertPayable(orderId: String): OrderPaymentSummary {
        val order = orderRepository.findByOrderId(orderId)
            ?: throw NoSuchElementException("Order not found: $orderId")
        require(order.status != OrderStatus.PAID) { "Order is already paid: $orderId" }
        require(order.status != OrderStatus.CANCELLED) { "Order is cancelled: $orderId" }
        return OrderPaymentSummary(
            orderId = order.orderId,
            merchantId = order.merchantApplicationId,
            branchId = order.merchantBranchId,
            totalAmountMinor = order.totalAmountMinor,
            currency = order.currency,
        )
    }

    override fun linkPaymentAttempt(
        orderId: String,
        paymentRequestId: UUID,
        provider: String,
        providerReference: String?,
    ): OrderPaymentSummary {
        val order = orderRepository.findByOrderId(orderId)
            ?: throw NoSuchElementException("Order not found: $orderId")
        require(order.status != OrderStatus.PAID) { "Order is already paid: $orderId" }
        require(order.status != OrderStatus.CANCELLED) { "Order is cancelled: $orderId" }

        val attempt = paymentAttemptRepository.findByPaymentRequestId(paymentRequestId)
            ?: PaymentAttempt(
                orderIdFk = order.id,
                orderId = order.orderId,
                paymentRequestId = paymentRequestId,
                provider = provider,
                providerReference = providerReference,
            )
        attempt.provider = provider
        attempt.update(providerReference = providerReference, at = Instant.now())
        paymentAttemptRepository.save(attempt)

        order.markPaymentInProgress(paymentRequestId, provider)
        orderRepository.save(order)
        return assertPayable(orderId)
    }
}
