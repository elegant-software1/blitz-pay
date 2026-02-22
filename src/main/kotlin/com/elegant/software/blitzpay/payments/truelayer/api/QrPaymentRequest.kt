package com.elegant.software.blitzpay.payments.truelayer.api

import java.time.Instant
import java.util.*

data class QrPaymentRequest(
    val merchant: String,
    val amount: Double,
    val currency: String = "EUR",
    val orderDetails: String
)

data class QrPaymentResponse(
    val success: Boolean = true,
    val paymentRequestId: UUID,
    val transactionId: String,
    val status: QrPaymentStatus,
    val qrCodeData: String? = null,
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

data class QrPaymentStatusUpdate(
    val paymentRequestId: UUID,
    val status: QrPaymentStatus,
    val qrCodeData: String? = null,
    val paymentResult: QrPaymentResult? = null,
    val timestamp: Instant = Instant.now()
)

/** Separate result type for QR payment events (distinct from the TrueLayer SDK's PaymentResult). */
data class QrPaymentResult(
    val paymentRequestId: UUID,
    val status: String,
    val qrCodeData: String? = null,
    val qrStatus: String? = null,
    val deepLink: String? = null,
    val transactionId: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val timestamp: Instant = Instant.now()
)

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
