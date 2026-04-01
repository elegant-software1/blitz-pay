package com.elegant.software.blitzpay.pricecomparison.application

import com.elegant.software.blitzpay.betterprice.agent.api.toDomain
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonOutcomeFactory
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonService
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonValidation
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.ProductMatchingService
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.ExplanationCode
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.MonitoringFailureCode
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.MonitoringStage
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.MonitoringWarningCode
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.ProviderLookupBottleneck
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.ProviderLookupFailure
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.ProviderLookupResult
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.ProviderLookupWarning
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.PriceLookupProvider
import com.elegant.software.blitzpay.support.TestFixtureLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class PriceComparisonMonitoringTest {

    private val provider = mock<PriceLookupProvider>()
    private val validation = PriceComparisonValidation()
    private val service = PriceComparisonService(
        validation = validation,
        provider = provider,
        matchingService = ProductMatchingService(),
        outcomeFactory = PriceComparisonOutcomeFactory(validation)
    )

    @Test
    fun `completed snapshot clamps progress to valid range`() {
        val snapshot = validation.snapshot(stage = MonitoringStage.COMPARISON, progress = 135)

        assertEquals(100, snapshot.progress)
        assertEquals(MonitoringStage.COMPARISON, snapshot.stage)
    }

    @Test
    fun `compare maps provider warnings and bottleneck into monitoring snapshot`() {
        val scenario = TestFixtureLoader.betterPriceScenario()
        whenever(provider.findOffers(eq(scenario.inputData.toDomain()))).thenReturn(
            ProviderLookupResult(
                offers = scenario.providerOffers,
                warnings = listOf(
                    ProviderLookupWarning(
                        code = "partial_results",
                        detail = "Lookup completed with a reduced retailer set after a slow provider response",
                        stage = "offer_extraction"
                    )
                ),
                bottleneck = ProviderLookupBottleneck(
                    stage = "offer_extraction",
                    reason = "slow_provider_response",
                    detail = "Offer lookup took longer than expected while waiting for a retailer response"
                )
            )
        )

        val result = service.compare(scenario.inputData.toDomain())

        assertEquals(MonitoringStage.COMPLETED, result.monitoring.stage)
        assertEquals(MonitoringWarningCode.PARTIAL_RESULTS, result.monitoring.warnings.single().code)
        assertEquals(MonitoringStage.OFFER_EXTRACTION, result.monitoring.bottleneck?.stage)
        assertEquals("slow_provider_response", result.monitoring.bottleneck?.reason)
    }

    @Test
    fun `compare returns failure monitoring details when provider lookup fails`() {
        val scenario = TestFixtureLoader.betterPriceScenario()
        whenever(provider.findOffers(eq(scenario.inputData.toDomain()))).thenReturn(
            ProviderLookupResult(
                offers = emptyList(),
                warnings = listOf(
                    ProviderLookupWarning(
                        code = "partial_results",
                        detail = "Lookup aborted before a complete retailer set could be inspected",
                        stage = "search_discovery"
                    )
                ),
                bottleneck = ProviderLookupBottleneck(
                    stage = "search_discovery",
                    reason = "provider_timeout",
                    detail = "External retailer lookup exceeded the synchronous request budget"
                ),
                failure = ProviderLookupFailure(
                    stage = "search_discovery",
                    code = "provider_lookup_failed",
                    message = "Retailer lookup failed before a comparable offer set was assembled",
                    retriable = true
                )
            )
        )

        val result = service.compare(scenario.inputData.toDomain())

        assertEquals(ExplanationCode.PROVIDER_LOOKUP_FAILED, result.explanationCode)
        assertEquals(MonitoringStage.FAILED, result.monitoring.stage)
        assertEquals(MonitoringFailureCode.PROVIDER_LOOKUP_FAILED, result.monitoring.failure?.code)
        assertTrue(result.monitoring.failure?.retriable == true)
        assertTrue(result.monitoring.failure?.message?.contains("Retailer lookup failed") == true)
    }

    @Test
    fun `compare returns validation failure snapshot for invalid request`() {
        val invalidRequest = TestFixtureLoader.betterPriceScenario().inputData.copy(
            inputPrice = BigDecimal.ZERO,
            productTitle = null,
            brandName = null,
            modelName = null,
            sku = null,
            additionalAttributes = emptyMap()
        )

        val result = service.compare(invalidRequest.toDomain())

        assertEquals(ExplanationCode.INVALID_REQUEST, result.explanationCode)
        assertEquals(MonitoringStage.FAILED, result.monitoring.stage)
        assertEquals(MonitoringFailureCode.INVALID_REQUEST, result.monitoring.failure?.code)
    }
}
