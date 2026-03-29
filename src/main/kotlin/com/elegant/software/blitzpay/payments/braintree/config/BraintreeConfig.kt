package com.elegant.software.blitzpay.payments.braintree.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(BraintreeProperties::class)
class BraintreeConfig
