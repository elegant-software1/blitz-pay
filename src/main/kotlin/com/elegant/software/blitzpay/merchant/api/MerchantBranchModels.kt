package com.elegant.software.blitzpay.merchant.api

import java.time.Instant
import java.util.UUID

data class CreateBranchRequest(
    val name: String,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geofenceRadiusMeters: Int? = null,
    val googlePlaceId: String? = null,
    val stripeSecretKey: String? = null,
    val stripePublishableKey: String? = null,
    val braintreeMerchantId: String? = null,
    val braintreePublicKey: String? = null,
    val braintreePrivateKey: String? = null,
    val braintreeEnvironment: String? = null,
)

data class BranchResponse(
    val id: UUID,
    val merchantId: UUID,
    val name: String,
    val active: Boolean,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val geofenceRadiusMeters: Int?,
    val googlePlaceId: String?,
    val placeFormattedAddress: String?,
    val placeRating: Double?,
    val placeReviewCount: Int?,
    val placeEnrichmentStatus: String?,
    val placeEnrichedAt: Instant?,
    val hasStripeCredentials: Boolean,
    val hasBraintreeCredentials: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
