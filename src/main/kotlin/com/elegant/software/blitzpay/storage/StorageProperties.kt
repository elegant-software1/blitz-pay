package com.elegant.software.blitzpay.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blitzpay.storage")
data class StorageProperties(
    val endpoint: String,
    val region: String = "us-east-1",
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val pathStyleAccess: Boolean = true
)
