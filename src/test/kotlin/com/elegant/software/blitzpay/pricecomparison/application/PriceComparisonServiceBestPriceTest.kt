package com.elegant.software.blitzpay.pricecomparison.application

import com.elegant.software.blitzpay.betterprice.agent.api.toDomain
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonOutcomeFactory
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonService
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonValidation
import com.elegant.software.blitzpay.betterprice.pricecomparison.application.ProductMatchingService
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.ExplanationCode
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.MonitoringStage
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

class PriceComparisonServiceBestPriceTest {

    private val provider = mock<PriceLookupProvider>()
    private val service = PriceComparisonService(
        validation = PriceComparisonValidation(),
        provider = provider,
        matchingService = ProductMatchingService(),
        outcomeFactory = PriceComparisonOutcomeFactory(PriceComparisonValidation())
    )

    @Test
    fun `compare returns best cheaper offer and exact savings`() {
        val scenario = TestFixtureLoader.betterPriceScenario()
        whenever(provider.findOffers(eq(scenario.inputData.toDomain()))).thenReturn(
            ProviderLookupResult(scenario.providerOffers)
        )

        val result = service.compare(scenario.inputData.toDomain())

        assertEquals(PriceComparisonStatus.BETTER_PRICE_FOUND, result.status)
        assertEquals("TechHub", result.bestOffer?.sellerName)
        assertEquals(BigDecimal("279.99"), result.bestOffer?.offerPrice)
        assertEquals(BigDecimal("50.00"), result.savingsAmount)
        assertEquals(ExplanationCode.BETTER_PRICE_AVAILABLE, result.explanationCode)
        assertEquals(MonitoringStage.COMPLETED, result.monitoring.stage)
        assertEquals(100, result.monitoring.progress)
    }
}
