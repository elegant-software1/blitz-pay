package com.elegant.software.blitzpay.payments.qrpay

import com.elegant.software.blitzpay.payments.support.PaymentUpdateBus
import com.elegant.software.blitzpay.payments.truelayer.api.PaymentResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "QR Payments", description = "Operations related to QR payments")
@RestController
@RequestMapping("/qr-payments")
class QrPaymentSseController(private val bus: PaymentUpdateBus) {
    private val logger = KotlinLogging.logger {}

    @Operation(
        summary = "Stream payment events via SSE",
        description = "Opens a Server-Sent Events stream that emits payment status updates for the given payment request ID. The stream auto-closes after 5 minutes of inactivity."
    )
    @ApiResponse(responseCode = "200", description = "SSE stream of payment events")
    @GetMapping(
        value = ["/{paymentRequestId}/events"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun stream(
        @Parameter(description = "Payment request UUID") @PathVariable paymentRequestId: UUID
    ): Flux<ServerSentEvent<PaymentResult>> {
        logger.info { "SSE client connected for paymentRequestId: $paymentRequestId" }
        return bus.sink(paymentRequestId)
            .asFlux()
            .timeout(Duration.ofMinutes(5), Flux.empty())
            .map { update ->
                ServerSentEvent.builder(update)
                    .event("payment")
                    .id(update.paymentRequestId.toString())
                    .build()
            }
            .doFinally { signal ->
                logger.debug { "SSE stream ended with signal $signal for paymentRequestId: $paymentRequestId" }
                bus.complete(paymentRequestId)
            }
    }
}
