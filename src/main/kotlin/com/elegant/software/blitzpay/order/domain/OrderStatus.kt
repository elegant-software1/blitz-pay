package com.elegant.software.blitzpay.order.domain

enum class OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_IN_PROGRESS,
    PAID,
    PAYMENT_FAILED,
    CANCELLED;

    fun isTerminal(): Boolean = this == PAID || this == CANCELLED
}
