package com.elegant.software.blitzpay.merchant.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "merchant_branches",
    schema = "blitzpay",
    indexes = [Index(name = "idx_merchant_branches_merchant_application_id", columnList = "merchant_application_id")]
)
class MerchantBranch(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "merchant_application_id", nullable = false, updatable = false)
    val merchantApplicationId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "address_line1")
    var addressLine1: String? = null,

    @Column(name = "address_line2")
    var addressLine2: String? = null,

    @Column(name = "city")
    var city: String? = null,

    @Column(name = "postal_code")
    var postalCode: String? = null,

    @Column(name = "country")
    var country: String? = null,

    // Stripe credentials — override merchant-level defaults when non-null
    @Column(name = "stripe_secret_key", length = 512)
    var stripeSecretKey: String? = null,

    @Column(name = "stripe_publishable_key", length = 512)
    var stripePublishableKey: String? = null,

    // Braintree credentials — override merchant-level defaults when non-null
    @Column(name = "braintree_merchant_id")
    var braintreeMerchantId: String? = null,

    @Column(name = "braintree_public_key")
    var braintreePublicKey: String? = null,

    @Column(name = "braintree_private_key", length = 512)
    var braintreePrivateKey: String? = null,

    @Column(name = "braintree_environment", length = 64)
    var braintreeEnvironment: String? = null,

    @Embedded
    var location: MerchantLocation? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt,
) {
    fun deactivate(at: Instant = Instant.now()) {
        active = false
        updatedAt = at
    }

    fun updateCredentials(
        stripeSecretKey: String? = this.stripeSecretKey,
        stripePublishableKey: String? = this.stripePublishableKey,
        braintreeMerchantId: String? = this.braintreeMerchantId,
        braintreePublicKey: String? = this.braintreePublicKey,
        braintreePrivateKey: String? = this.braintreePrivateKey,
        braintreeEnvironment: String? = this.braintreeEnvironment,
        at: Instant = Instant.now(),
    ) {
        this.stripeSecretKey = stripeSecretKey
        this.stripePublishableKey = stripePublishableKey
        this.braintreeMerchantId = braintreeMerchantId
        this.braintreePublicKey = braintreePublicKey
        this.braintreePrivateKey = braintreePrivateKey
        this.braintreeEnvironment = braintreeEnvironment
        this.updatedAt = at
    }
}
