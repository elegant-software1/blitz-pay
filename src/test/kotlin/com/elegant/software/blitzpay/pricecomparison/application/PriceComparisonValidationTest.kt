package com.elegant.software.blitzpay.pricecomparison.application

import com.elegant.software.blitzpay.betterprice.pricecomparison.application.PriceComparisonValidation
import com.elegant.software.blitzpay.betterprice.pricecomparison.domain.ProductLookupRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

class PriceComparisonValidationTest {

    private val validation = PriceComparisonValidation()

    @Test
    fun `validate rejects non-positive input price`() {
        val request = ProductLookupRequest(
            inputPrice = BigDecimal.ZERO,
            currency = "USD",
            productTitle = "Sony WH-1000XM5"
        )

        val ex = assertFailsWith<IllegalArgumentException> {
            validation.validate(request)
        }

        assertEquals("inputPrice must be a positive monetary amount", ex.message)
    }

    @Test
    fun `validate rejects missing product identifiers`() {
        val request = ProductLookupRequest(
            inputPrice = BigDecimal("99.99"),
            currency = "USD"
        )

        val ex = assertFailsWith<IllegalArgumentException> {
            validation.validate(request)
        }

        assertEquals("at least one product identifier must be provided", ex.message)
    }

    @Test
    fun `calculateSavingsPercentage returns two-decimal percentage`() {
        val percentage = validation.calculateSavingsPercentage(
            inputPrice = BigDecimal("329.99"),
            lowerPrice = BigDecimal("279.99")
        )

        assertEquals(BigDecimal("15.15"), percentage)
    }

    @Test
    fun `completedSnapshot sets completed monitoring defaults`() {
        val snapshot = validation.completedSnapshot()

        assertEquals(100, snapshot.progress)
        assertEquals("completed", snapshot.stage.wireValue())
        assertTrue(snapshot.warnings.isEmpty())
    }
}
