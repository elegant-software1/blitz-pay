package com.elegant.software.blitzpay.order.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_orders", schema = "blitzpay")
class Order(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    val orderId: String,

    @Column(name = "merchant_application_id", nullable = false, updatable = false)
    val merchantApplicationId: UUID,

    @Column(name = "merchant_branch_id")
    val merchantBranchId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,

    @Column(nullable = false, length = 3)
    val currency: String,

    @Column(name = "total_amount_minor", nullable = false)
    val totalAmountMinor: Long,

    @Column(name = "item_count", nullable = false)
    val itemCount: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    @Column(name = "last_payment_request_id")
    var lastPaymentRequestId: UUID? = null,

    @Column(name = "last_payment_provider", length = 32)
    var lastPaymentProvider: String? = null,
) {
    fun markPaymentInProgress(paymentRequestId: UUID, provider: String, at: Instant = Instant.now()) {
        require(status != OrderStatus.PAID) { "Order is already paid: $orderId" }
        require(status != OrderStatus.CANCELLED) { "Order is cancelled: $orderId" }
        if (status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PAYMENT_FAILED) {
            status = OrderStatus.PAYMENT_IN_PROGRESS
        }
        lastPaymentRequestId = paymentRequestId
        lastPaymentProvider = provider
        updatedAt = at
    }

    fun applyPaymentUpdate(
        nextStatus: OrderStatus,
        paymentRequestId: UUID,
        provider: String?,
        at: Instant,
    ): Boolean {
        if (status == OrderStatus.PAID || status == OrderStatus.CANCELLED) {
            return false
        }
        val allowed = when (status) {
            OrderStatus.PENDING_PAYMENT -> setOf(OrderStatus.PAYMENT_IN_PROGRESS, OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED)
            OrderStatus.PAYMENT_IN_PROGRESS -> setOf(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED)
            OrderStatus.PAYMENT_FAILED -> setOf(OrderStatus.PAYMENT_IN_PROGRESS, OrderStatus.CANCELLED)
            OrderStatus.PAID, OrderStatus.CANCELLED -> emptySet()
        }
        if (nextStatus !in allowed) {
            return false
        }
        status = nextStatus
        lastPaymentRequestId = paymentRequestId
        lastPaymentProvider = provider ?: lastPaymentProvider
        if (nextStatus == OrderStatus.PAID) {
            paidAt = at
        }
        updatedAt = at
        return true
    }

    companion object {
        fun nextOrderId(): String =
            "ORD-" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
    }
}
