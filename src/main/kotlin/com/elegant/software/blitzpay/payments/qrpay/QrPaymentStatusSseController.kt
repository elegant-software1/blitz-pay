package com.elegant.software.blitzpay.payments.qrpay

import com.elegant.software.blitzpay.payments.truelayer.api.QrPaymentResult
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/qr-payments/status")
class QrPaymentStatusSseController(
    private val qrPaymentUpdateBus: QrPaymentUpdateBus
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping(
        value = ["/{paymentRequestId}/events"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun streamStatus(@PathVariable paymentRequestId: UUID): Flux<ServerSentEvent<String>> {
        logger.info { "QR status SSE connected for paymentRequestId: $paymentRequestId" }

        return qrPaymentUpdateBus.sink(paymentRequestId)
            .asFlux()
            .map { qrPaymentResult ->
                val statusMessage = when (qrPaymentResult.status) {
                    "qr_initiated" -> "QR code generated"
                    "qr_scanned" -> "QR code scanned"
                    "qr_expired" -> "QR code expired"
                    "executed" -> "Payment successful"
                    "failed" -> "Payment failed"
                    else -> "Status: ${qrPaymentResult.status}"
                }

                ServerSentEvent.builder<String>()
                    .event("qr_payment_status")
                    .id(qrPaymentResult.paymentRequestId.toString())
                    .data("""
                        {
                            "paymentRequestId": "${qrPaymentResult.paymentRequestId}",
                            "status": "${qrPaymentResult.status}",
                            "message": "$statusMessage",
                            "qrCodeData": "${qrPaymentResult.qrCodeData ?: ""}",
                            "timestamp": "${qrPaymentResult.timestamp}"
                        }
                    """.trimIndent())
                    .build()
            }
            .timeout(Duration.ofMinutes(5), Flux.empty())
            .doFinally { signal ->
                logger.debug { "QR status SSE stream ended: $signal" }
            }
    }
}
