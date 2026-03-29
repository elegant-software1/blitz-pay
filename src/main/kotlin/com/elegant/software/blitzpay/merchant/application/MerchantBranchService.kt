package com.elegant.software.blitzpay.merchant.application

import com.elegant.software.blitzpay.merchant.api.BranchResponse
import com.elegant.software.blitzpay.merchant.api.CreateBranchRequest
import com.elegant.software.blitzpay.merchant.domain.MerchantBranch
import com.elegant.software.blitzpay.merchant.domain.MerchantLocation
import com.elegant.software.blitzpay.merchant.repository.MerchantApplicationRepository
import com.elegant.software.blitzpay.merchant.repository.MerchantBranchRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MerchantBranchService(
    private val merchantBranchRepository: MerchantBranchRepository,
    private val merchantApplicationRepository: MerchantApplicationRepository,
) {

    fun create(merchantId: UUID, request: CreateBranchRequest): BranchResponse {
        require(request.name.isNotBlank()) { "Branch name must not be blank" }
        require((request.latitude == null) == (request.longitude == null)) {
            "latitude and longitude must both be provided or both be omitted"
        }
        request.geofenceRadiusMeters?.let { require(it > 0) { "geofenceRadiusMeters must be positive" } }
        check(merchantApplicationRepository.existsById(merchantId)) { "Merchant not found: $merchantId" }
        val branch = MerchantBranch(
            merchantApplicationId = merchantId,
            name = request.name,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            city = request.city,
            postalCode = request.postalCode,
            country = request.country,
            location = request.toLocation(),
            stripeSecretKey = request.stripeSecretKey,
            stripePublishableKey = request.stripePublishableKey,
            braintreeMerchantId = request.braintreeMerchantId,
            braintreePublicKey = request.braintreePublicKey,
            braintreePrivateKey = request.braintreePrivateKey,
            braintreeEnvironment = request.braintreeEnvironment,
        )
        return merchantBranchRepository.save(branch).toResponse()
    }

    fun list(merchantId: UUID): List<BranchResponse> {
        check(merchantApplicationRepository.existsById(merchantId)) { "Merchant not found: $merchantId" }
        return merchantBranchRepository
            .findAllByMerchantApplicationIdAndActiveTrue(merchantId)
            .map { it.toResponse() }
    }

    fun get(merchantId: UUID, branchId: UUID): BranchResponse {
        check(merchantApplicationRepository.existsById(merchantId)) { "Merchant not found: $merchantId" }
        val branch = merchantBranchRepository.findByIdAndActiveTrue(branchId)
            ?: error("Branch not found: $branchId")
        check(branch.merchantApplicationId == merchantId) { "Branch not found: $branchId" }
        return branch.toResponse()
    }

    private fun MerchantBranch.toResponse() = BranchResponse(
        id = id,
        merchantId = merchantApplicationId,
        name = name,
        active = active,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        city = city,
        postalCode = postalCode,
        country = country,
        latitude = location?.latitude,
        longitude = location?.longitude,
        geofenceRadiusMeters = location?.geofenceRadiusMeters,
        googlePlaceId = location?.googlePlaceId,
        placeFormattedAddress = location?.placeFormattedAddress,
        placeRating = location?.placeRating,
        placeReviewCount = location?.placeReviewCount,
        placeEnrichmentStatus = location?.placeEnrichmentStatus,
        placeEnrichedAt = location?.placeEnrichedAt,
        hasStripeCredentials = stripeSecretKey != null && stripePublishableKey != null,
        hasBraintreeCredentials = braintreeMerchantId != null && braintreePublicKey != null && braintreePrivateKey != null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun CreateBranchRequest.toLocation(): MerchantLocation? {
        val hasCoordinates = latitude != null && longitude != null
        if (!hasCoordinates) return null
        return MerchantLocation(
            latitude = requireNotNull(latitude),
            longitude = requireNotNull(longitude),
            geofenceRadiusMeters = geofenceRadiusMeters ?: 500,
            googlePlaceId = googlePlaceId,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            city = city,
            postalCode = postalCode,
            country = country,
            placeEnrichmentStatus = googlePlaceId?.let { "PENDING" }
        )
    }
}
