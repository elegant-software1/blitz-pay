package com.elegant.software.blitzpay.invoice.internal

/**
 * `PAID` means the invoice is settled from the payer side.
 * `RECEIVED` is reserved for a confirmed receipt state in the business workflow.
 */
enum class PaymentStatus {
    PENDING,
    PAID,
    RECEIVED
}
