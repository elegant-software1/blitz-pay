package com.elegant.software.blitzpay.merchant.support

import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import org.springframework.stereotype.Component

@Component
class MerchantObservabilitySupport {
    fun recordSuccess(operation: String, status: MerchantOnboardingStatus) {
        // Placeholder for metrics/tracing until observability behavior is specified.
    }
}
