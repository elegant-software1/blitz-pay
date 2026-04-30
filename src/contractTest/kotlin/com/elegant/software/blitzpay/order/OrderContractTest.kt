package com.elegant.software.blitzpay.order

import com.elegant.software.blitzpay.contract.ContractVerifierBase
import com.elegant.software.blitzpay.order.api.OrderItemResponse
import com.elegant.software.blitzpay.order.api.OrderResponse
import com.elegant.software.blitzpay.order.application.OrderService
import com.elegant.software.blitzpay.order.domain.OrderStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.UUID

class OrderContractTest : ContractVerifierBase() {
    @MockitoBean
    private lateinit var orderService: OrderService

    @Test
    fun `post orders returns created order`() {
        whenever(orderService.create(any())).thenReturn(
            OrderResponse(
                orderId = "ORDER-123",
                merchantId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                branchId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                status = OrderStatus.PENDING_PAYMENT,
                currency = "EUR",
                totalAmountMinor = 2900,
                items = listOf(
                    OrderItemResponse(
                        productId = UUID.fromString("22222222-2222-2222-2222-222222222221"),
                        name = "Coffee",
                        quantity = 2,
                        unitPriceMinor = 1250,
                        lineTotalMinor = 2500,
                    )
                ),
                createdAt = Instant.parse("2026-04-30T10:00:00Z"),
            )
        )

        webTestClient.post()
            .uri("/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "items": [
                    { "productId": "22222222-2222-2222-2222-222222222221", "quantity": 2 },
                    { "productId": "22222222-2222-2222-2222-222222222222", "quantity": 1 }
                  ]
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.orderId").isEqualTo("ORDER-123")
            .jsonPath("$.status").isEqualTo("PENDING_PAYMENT")
            .jsonPath("$.totalAmountMinor").isEqualTo(2900)
    }

    @Test
    fun `get order returns paid status`() {
        whenever(orderService.get("ORDER-123")).thenReturn(
            OrderResponse(
                orderId = "ORDER-123",
                merchantId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                branchId = null,
                status = OrderStatus.PAID,
                currency = "EUR",
                totalAmountMinor = 2900,
                items = emptyList(),
                createdAt = Instant.parse("2026-04-30T10:00:00Z"),
                paidAt = Instant.parse("2026-04-30T10:15:30Z"),
            )
        )

        webTestClient.get()
            .uri("/v1/orders/ORDER-123")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("PAID")
            .jsonPath("$.paidAt").isEqualTo("2026-04-30T10:15:30Z")
    }
}
