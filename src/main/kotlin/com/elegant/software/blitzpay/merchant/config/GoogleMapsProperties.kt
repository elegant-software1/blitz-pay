package com.elegant.software.blitzpay.merchant.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blitzpay.google-maps")
data class GoogleMapsProperties(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "https://maps.googleapis.com",
    val enrichmentIntervalMs: Long = 300_000,
    val batchSize: Int = 50
)
