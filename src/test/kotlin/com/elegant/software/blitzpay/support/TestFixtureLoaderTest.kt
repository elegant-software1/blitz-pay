package com.elegant.software.blitzpay.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestFixtureLoaderTest {

    @Test
    fun `loads canonical invoice scenario metadata and expectations`() {
        val scenario = TestFixtureLoader.invoiceScenario()

        assertEquals("invoice-canonical", scenario.scenarioId)
        assertEquals("invoice", scenario.domain)
        assertTrue(scenario.tags.contains("canonical"))
        assertEquals("INV-2026-001", scenario.inputData.invoiceNumber)
        assertEquals("INV-2026-001", scenario.expectations.invoiceNumber)
    }

    @Test
    fun `serializes canonical invoice request as json`() {
        val json = TestFixtureLoader.invoiceRequestJson()

        assertTrue(json.contains("\"invoiceNumber\":\"INV-2026-001\""))
        assertTrue(json.contains("\"currency\":\"EUR\""))
    }

    @Test
    fun `creates explicit fixture variants without mutating canonical scenario`() {
        val canonical = TestFixtureLoader.invoiceData()
        val withBankAccount = TestFixtureLoader.withBankAccount()
        val withFooter = TestFixtureLoader.withFooter()
        val withLogo = TestFixtureLoader.withLogo()
        val singleLineItem = TestFixtureLoader.singleLineItem()

        assertNotNull(withBankAccount.bankAccount)
        assertEquals(TestFixtureLoader.invoiceScenario().expectations.bankName, withBankAccount.bankAccount?.bankName)
        assertEquals(TestFixtureLoader.invoiceScenario().expectations.footerText, withFooter.footerText)
        assertEquals(TestFixtureLoader.invoiceScenario().expectations.logoBase64, withLogo.logoBase64)
        assertEquals(1, singleLineItem.lineItems.size)
        assertEquals(TestFixtureLoader.invoiceScenario().expectations.singleLineItemDescription, singleLineItem.lineItems.single().description)
        assertTrue(canonical.bankAccount == null)
        assertTrue(canonical.footerText == null)
        assertTrue(canonical.logoBase64 == null)
        assertFalse(canonical.lineItems.size == 1)
    }

    @Test
    fun `loads canonical product comparison scenarios`() {
        val betterPrice = TestFixtureLoader.betterPriceScenario()
        val noBetterPrice = TestFixtureLoader.noBetterPriceScenario()
        val unavailable = TestFixtureLoader.comparisonUnavailableScenario()
        val slowdown = TestFixtureLoader.monitoringSlowdownScenario()
        val failure = TestFixtureLoader.monitoringFailureScenario()

        assertEquals("pricecomparison", betterPrice.domain)
        assertEquals("better_price_found", betterPrice.expectations.status)
        assertEquals("no_better_price_found", noBetterPrice.expectations.status)
        assertEquals("comparison_unavailable", unavailable.expectations.status)
        assertEquals("better_price_found", slowdown.expectations.status)
        assertEquals("provider_lookup_failed", failure.expectations.explanationCode)
        assertTrue(betterPrice.providerOffers.isNotEmpty())
    }

    @Test
    fun `serializes product comparison request as json`() {
        val json = TestFixtureLoader.priceComparisonRequestJson("better-price")

        assertTrue(json.contains("\"productTitle\":\"Sony WH-1000XM5 Wireless Noise Canceling Headphones\""))
        assertTrue(json.contains("\"inputPrice\":329.99"))
    }

    @Test
    fun `loads market search fixture text`() {
        val discovery = TestFixtureLoader.marketSearchFixtureText("brave-discovery-sony.json")
        val retailerHtml = TestFixtureLoader.marketSearchFixtureText("retailer-sony-techhub.html")

        assertTrue(discovery.contains("\"results\""))
        assertTrue(retailerHtml.contains("application/ld+json"))
    }
}
