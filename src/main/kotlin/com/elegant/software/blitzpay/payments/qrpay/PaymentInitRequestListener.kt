@file:ApplicationModule(displayName = "outbound")

package com.elegant.software.blitzpay.payments.qrpay

import TlWebhookEnvelope
import com.elegant.software.blitzpay.order.api.OrderPaymentInitiationRequested
import com.elegant.software.blitzpay.order.api.PaymentMethod
import com.elegant.software.blitzpay.payments.support.PaymentUpdateBus
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component
import java.util.*

@Component
class PaymentInitRequestListener(
    private val paymentUpdateBus: PaymentUpdateBus,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(PaymentInitRequestListener::class.java)
    }

    @EventListener
    fun on(e: TlWebhookEnvelope) {
        LOG.info("Webhook Informed TrueLayer module {}", e)
        val paymentRequestId = e.metadata?.get("paymentRequestId")
        if (paymentRequestId is String) {
            val uuid = UUID.fromString(paymentRequestId)
            paymentUpdateBus.complete(uuid)
        }
    }

    @ApplicationModuleListener
    fun on(event: OrderPaymentInitiationRequested) {
        // TRUELAYER requires an interactive redirect: the mobile app calls POST /v1/payments/request
        // which returns paymentId + resourceToken synchronously. Initiating here would create a
        // second concurrent TrueLayer payment session, causing the /v1/payments/request call to fail.
        if (event.paymentMethod == PaymentMethod.TRUELAYER) return
        LOG.info("Received payment initiation for order {} method {} — no auto-handler registered",
            event.orderId, event.paymentMethod)
    }
}
