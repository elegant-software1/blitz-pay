package com.elegant.software.quickpay.payments.truelayer

import com.elegant.software.quickpay.payments.truelayer.api.PaymentGateway
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TrueLayerPaymentStarter(
    private val gateway: PaymentGateway
) {


    @EventListener
    fun on(e: PaymentGateway.PaymentRequested) {
        gateway.startPayment(
            PaymentGateway.PaymentRequested(e.orderId, e.amountMinorUnits, e.currency, e.userDisplayName, e.redirectReturnUri)
        )
    }
}
