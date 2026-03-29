package com.elegant.software.blitzpay.merchant.api

import java.util.UUID

data class SetMerchantLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val geofenceRadiusMeters: Int,
    val googlePlaceId: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)

data class MerchantLocationResponse(
    val merchantId: UUID,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadiusMeters: Int,
    val googlePlaceId: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
    val placeFormattedAddress: String?,
    val placeRating: Double?,
    val placeReviewCount: Int?,
    val placeEnrichmentStatus: String?,
    val placeEnrichedAt: java.time.Instant?
)

data class NearbyMerchantResponse(
    val merchantId: UUID,
    val legalBusinessName: String,
    val latitude: Double,
    val longitude: Double,
    val geofenceRadiusMeters: Int,
    val googlePlaceId: String?,
    val distanceMeters: Double
)

data class NearbyMerchantsResponse(
    val merchants: List<NearbyMerchantResponse>
)
