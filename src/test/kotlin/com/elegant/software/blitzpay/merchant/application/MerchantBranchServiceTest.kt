package com.elegant.software.blitzpay.merchant.application

import com.elegant.software.blitzpay.merchant.api.CreateBranchRequest
import com.elegant.software.blitzpay.merchant.domain.MerchantBranch
import com.elegant.software.blitzpay.merchant.repository.MerchantApplicationRepository
import com.elegant.software.blitzpay.merchant.repository.MerchantBranchRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

class MerchantBranchServiceTest {
    private val branchRepository = mock<MerchantBranchRepository>()
    private val merchantRepository = mock<MerchantApplicationRepository>()
    private val service = MerchantBranchService(branchRepository, merchantRepository)

    @Test
    fun `create branch stores operational address and geolocation`() {
        val merchantId = UUID.randomUUID()
        whenever(merchantRepository.existsById(merchantId)).thenReturn(true)
        whenever(branchRepository.save(any<MerchantBranch>())).thenAnswer { it.arguments[0] }

        val response = service.create(
            merchantId,
            CreateBranchRequest(
                name = "Bremen Mitte",
                addressLine1 = "Marktplatz 1",
                city = "Bremen",
                postalCode = "28195",
                country = "DE",
                latitude = 53.0758,
                longitude = 8.8072,
                geofenceRadiusMeters = 250,
                googlePlaceId = "ChIJ-test"
            )
        )

        assertEquals("Marktplatz 1", response.addressLine1)
        assertEquals("Bremen", response.city)
        assertEquals(53.0758, response.latitude)
        assertEquals(8.8072, response.longitude)
        assertEquals(250, response.geofenceRadiusMeters)
        assertEquals("ChIJ-test", response.googlePlaceId)
        assertEquals("PENDING", response.placeEnrichmentStatus)
    }
}
