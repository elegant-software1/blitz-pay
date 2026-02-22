package com.elegant.software.blitzpay.payments.truelayer.support

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "qr")
data class QrCodeProperties(
    var expiry: Expiry = Expiry(),
    var truelayer: TrueLayer = TrueLayer(),
    var server: Server = Server()
) {
    data class Expiry(
        var minutes: Int = 15,
        var cleanupInterval: Long = 300000
    )

    data class TrueLayer(
        var deepLinkBase: String = "https://payment.truelayer.com/quickpay",
        var redirectUri: String = "http://localhost:8080/api/qr-payments/callback"
    )

    data class Server(
        var baseUrl: String = "http://localhost:8080"
    )
}
