package com.elegant.software.blitzpay.pricecomparison.application

import com.elegant.software.blitzpay.betterprice.agent.api.toDomain
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonOutcomeFactory
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonService
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonValidation
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.ProductMatchingService
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.ExplanationCode
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.MonitoringStage
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.MonitoringWarningCode
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.PriceComparisonStatus
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.ProviderLookupResult
import com.elegant.software.blitzpay.betterprice.pricecomparison.provider.PriceLookupProvider
import com.elegant.software.blitzpay.support.TestFixtureLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class PriceComparisonServiceOutcomeTest {

    private val provider = mock<PriceLookupProvider>()
    private val service = PriceComparisonService(
        validation = PriceComparisonValidation(),
        provider = provider,
        matchingService = ProductMatchingService(),
        outcomeFactory = PriceComparisonOutcomeFactory(PriceComparisonValidation())
    )

    @Test
    fun `compare returns no better price when no lower qualifying offer exists`() {
        val scenario = TestFixtureLoader.noBetterPriceScenario()
        whenever(provider.findOffers(eq(scenario.inputData.toDomain()))).thenReturn(
            ProviderLookupResult(scenario.providerOffers)
        )

        val result = service.compare(scenario.inputData.toDomain())

        assertEquals(PriceComparisonStatus.NO_BETTER_PRICE_FOUND, result.status)
        assertEquals(BigDecimal("0.00"), result.savingsAmount)
        assertEquals(ExplanationCode.NO_CHEAPER_QUALIFYING_OFFER, result.explanationCode)
        assertEquals(MonitoringStage.COMPLETED, result.monitoring.stage)
    }

    @Test
    fun `compare returns comparison unavailable for weak product matches`() {
        val scenario = TestFixtureLoader.comparisonUnavailableScenario()
        whenever(provider.findOffers(eq(scenario.inputData.toDomain()))).thenReturn(
            ProviderLookupResult(scenario.providerOffers)
        )

        val result = service.compare(scenario.inputData.toDomain())

        assertEquals(PriceComparisonStatus.COMPARISON_UNAVAILABLE, result.status)
        assertEquals(ExplanationCode.LOW_MATCH_CONFIDENCE, result.explanationCode)
        assertEquals(BigDecimal("0.00"), result.savingsAmount)
        assertEquals(MonitoringStage.COMPLETED, result.monitoring.stage)
        assertEquals(MonitoringWarningCode.LOW_MATCH_CONFIDENCE, result.monitoring.warnings.single().code)
    }
}
