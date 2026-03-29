package com.elegant.software.blitzpay.payments.stripe.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StripeProperties::class)
class StripeConfig
