package com.elegant.software.blitzpay.merchant.application

import com.elegant.software.blitzpay.merchant.domain.MerchantOnboardingStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

data class MerchantAuditEvent(
    val applicationId: UUID,
    val applicationReference: String,
    val actorId: String,
    val action: String,
    val status: MerchantOnboardingStatus,
    val occurredAt: Instant
)

@Component
class MerchantAuditTrail {
    fun record(event: MerchantAuditEvent) {
        // Placeholder for audit persistence/publishing until the onboarding flow is specified.
    }
}
