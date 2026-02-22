package com.elegant.software.blitzpay.payments.qrpay

import java.time.Instant
import java.util.UUID

/**
 * Request to generate a QR code for a payment.
 */
data class QrPaymentRequest(
    val merchant: String,
    val amount: Double,
    val currency: String = "GBP",
    val orderDetails: String,
    val paymentRequestId: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now()
)

/**
 * Response containing generated QR code data.
 */
data class QrPaymentResponse(
    val success: Boolean = true,
    val paymentRequestId: UUID,
    val status: QrPaymentStatus,
    val qrCodeImage: String? = null,
    val qrCodeUrl: String? = null,
    val deepLink: String? = null,
    val paymentUrl: String? = null,
    val merchant: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val expiresAt: Instant,
    val message: String? = null,
    val error: String? = null
)

/**
 * Lifecycle status of a QR payment.
 */
enum class QrPaymentStatus {
    INITIATED,
    PENDING,
    SCANNED,
    PROCESSING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED
}
