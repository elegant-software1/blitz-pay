package com.elegant.software.blitzpay.payments.qrpay

import com.elegant.software.blitzpay.payments.truelayer.api.QrPaymentResult
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class QrPaymentUpdateAdapter(
    private val qrPaymentUpdateBus: QrPaymentUpdateBus
) {

    fun publishQrInitiated(
        paymentRequestId: UUID,
        qrCodeData: String,
        deepLink: String,
        amount: String,
        currency: String
    ) {
        val result = QrPaymentResult(
            paymentRequestId = paymentRequestId,
            status = "qr_initiated",
            qrCodeData = qrCodeData,
            deepLink = deepLink,
            timestamp = Instant.now()
        )
        qrPaymentUpdateBus.emit(paymentRequestId, result)
    }

    fun publishQrScanned(paymentRequestId: UUID) {
        val result = QrPaymentResult(
            paymentRequestId = paymentRequestId,
            status = "qr_scanned",
            timestamp = Instant.now()
        )
        qrPaymentUpdateBus.emit(paymentRequestId, result)
    }

    fun publishQrExpired(paymentRequestId: UUID) {
        val result = QrPaymentResult(
            paymentRequestId = paymentRequestId,
            status = "qr_expired",
            timestamp = Instant.now()
        )
        qrPaymentUpdateBus.emit(paymentRequestId, result)
    }

    fun publishPaymentStatus(
        paymentRequestId: UUID,
        status: String,
        transactionId: String? = null,
        amount: Double? = null,
        currency: String? = null
    ) {
        val result = QrPaymentResult(
            paymentRequestId = paymentRequestId,
            status = status,
            transactionId = transactionId,
            amount = amount,
            currency = currency,
            timestamp = Instant.now()
        )
        qrPaymentUpdateBus.emit(paymentRequestId, result)
    }
}
