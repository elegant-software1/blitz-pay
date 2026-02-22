package com.elegant.software.blitzpay.payments.truelayer.support

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*

@Service
class QrCodeGenerator {
    private val logger = KotlinLogging.logger {}

    data class QrCodeResult(
        val paymentUrl: String,
        val deepLink: String,
        val qrCodeImage: String
    )

    fun generatePaymentQRCode(
        paymentRequestId: UUID,
        amount: String,
        merchant: String,
        currency: String = "EUR",
        paymentUrl: String? = null
    ): QrCodeResult {
        logger.info { "Generating QR code for payment: $paymentRequestId" }

        val finalPaymentUrl = paymentUrl ?: createTrueLayerPaymentUrl(paymentRequestId)

        val payloadJson = """{
        "m": "$merchant",
        "a": $amount,
        "c": "$currency",
        "u": "$finalPaymentUrl"
    }""".trimIndent()

        val deepLink = createDeepLink(finalPaymentUrl)
        val qrCodeImage = generateQRCodeBase64(payloadJson)

        return QrCodeResult(
            paymentUrl = finalPaymentUrl,
            deepLink = deepLink,
            qrCodeImage = qrCodeImage
        )
    }

    private fun createTrueLayerPaymentUrl(paymentRequestId: UUID): String =
        "https://payment.truelayer.com/checkout/$paymentRequestId"

    private fun createDeepLink(paymentUrl: String): String {
        val paymentId = paymentUrl.substringAfterLast("/")
        return "truelayer://payment/$paymentId"
    }

    fun generateQRCodeImage(text: String, width: Int = 300, height: Int = 300): ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix: BitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
        ByteArrayOutputStream().use { outputStream ->
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
            return outputStream.toByteArray()
        }
    }

    fun generateQRCodeBase64(text: String, size: Int = 300): String {
        val imageBytes = generateQRCodeImage(text, size, size)
        return Base64.getEncoder().encodeToString(imageBytes)
    }
}
