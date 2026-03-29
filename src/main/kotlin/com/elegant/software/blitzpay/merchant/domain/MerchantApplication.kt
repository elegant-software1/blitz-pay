package com.elegant.software.blitzpay.merchant.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.CascadeType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "merchant_applications")
class MerchantApplication(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val applicationReference: String,

    @Embedded
    var businessProfile: BusinessProfile,

    @Embedded
    var primaryContact: PrimaryContact,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MerchantOnboardingStatus = MerchantOnboardingStatus.DRAFT,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column
    var submittedAt: Instant? = null,

    @Column
    var lastUpdatedAt: Instant = createdAt,

    // Stripe merchant-level default credentials (branch may override)
    @Column(name = "stripe_secret_key", length = 512)
    var stripeSecretKey: String? = null,

    @Column(name = "stripe_publishable_key", length = 512)
    var stripePublishableKey: String? = null,

    // Braintree merchant-level default credentials (branch may override)
    @Column(name = "braintree_merchant_id")
    var braintreeMerchantId: String? = null,

    @Column(name = "braintree_public_key")
    var braintreePublicKey: String? = null,

    @Column(name = "braintree_private_key", length = 512)
    var braintreePrivateKey: String? = null,

    @Column(name = "braintree_environment", length = 64)
    var braintreeEnvironment: String? = null,
) {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "merchant_people",
        joinColumns = [JoinColumn(name = "merchant_application_id")]
    )
    var people: MutableList<Person> = mutableListOf()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "merchant_supporting_materials",
        joinColumns = [JoinColumn(name = "merchant_application_id")]
    )
    var supportingMaterials: MutableList<SupportingMaterial> = mutableListOf()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "merchant_review_decisions",
        joinColumns = [JoinColumn(name = "merchant_application_id")]
    )
    var reviewDecisions: MutableList<ReviewDecision> = mutableListOf()

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "level",      column = Column(name = "risk_level")),
        AttributeOverride(name = "score",      column = Column(name = "risk_score")),
        AttributeOverride(name = "rationale",  column = Column(name = "risk_rationale")),
        AttributeOverride(name = "assessedAt", column = Column(name = "risk_assessed_at"))
    )
    var riskAssessment: RiskAssessment? = null

    @OneToOne(fetch = FetchType.EAGER, optional = true, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "monitoring_record_id")
    var monitoringRecord: MonitoringRecord? = null

    @Embedded
    var location: MerchantLocation? = null

    fun registerDirect(activatedAt: Instant = Instant.now()) {
        MerchantOnboardingLifecycle.requireTransition(status, MerchantOnboardingStatus.ACTIVE)
        status = MerchantOnboardingStatus.ACTIVE
        submittedAt = activatedAt
        touch(activatedAt)
    }

    fun submit(submittedAt: Instant = Instant.now()) {
        MerchantOnboardingLifecycle.requireTransition(status, MerchantOnboardingStatus.SUBMITTED)
        status = MerchantOnboardingStatus.SUBMITTED
        this.submittedAt = submittedAt
        touch(submittedAt)
    }

    fun transitionTo(nextStatus: MerchantOnboardingStatus, changedAt: Instant = Instant.now()) {
        MerchantOnboardingLifecycle.requireTransition(status, nextStatus)
        status = nextStatus
        touch(changedAt)
    }

    fun requestChanges(reason: String, changedAt: Instant = Instant.now()) {
        reviewDecisions += ReviewDecision.actionRequired(reason = reason, decidedAt = changedAt)
        transitionTo(MerchantOnboardingStatus.ACTION_REQUIRED, changedAt)
    }

    fun recordDecision(
        outcome: ReviewOutcome,
        reason: String,
        reviewerId: String,
        decidedAt: Instant = Instant.now()
    ) {
        reviewDecisions += ReviewDecision(
            outcome = outcome,
            reason = reason,
            reviewerId = reviewerId,
            decidedAt = decidedAt
        )
        when (outcome) {
            ReviewOutcome.APPROVED -> transitionTo(MerchantOnboardingStatus.SETUP, decidedAt)
            ReviewOutcome.REJECTED -> transitionTo(MerchantOnboardingStatus.REJECTED, decidedAt)
            ReviewOutcome.ACTION_REQUIRED -> transitionTo(MerchantOnboardingStatus.ACTION_REQUIRED, decidedAt)
        }
    }

    fun addPerson(person: Person) {
        people += person
        touch()
    }

    fun addSupportingMaterial(material: SupportingMaterial) {
        supportingMaterials += material
        touch()
    }

    fun attachRiskAssessment(assessment: RiskAssessment) {
        riskAssessment = assessment
        touch()
    }

    fun updateLogo(storageKey: String) {
        businessProfile = businessProfile.copy(logoStorageKey = storageKey)
        touch()
    }

    fun updateLocation(newLocation: MerchantLocation) {
        location = newLocation
        touch()
    }

    fun clearLocation() {
        location = null
        touch()
    }

    fun startMonitoring(record: MonitoringRecord, changedAt: Instant = Instant.now()) {
        monitoringRecord = record
        transitionTo(MerchantOnboardingStatus.MONITORING, changedAt)
    }

    private fun touch(changedAt: Instant = Instant.now()) {
        lastUpdatedAt = changedAt
    }
}
