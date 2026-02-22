package com.elegant.software.blitzpay.payments.truelayer.inbound

import com.elegant.software.blitzpay.payments.truelayer.api.QrPaymentResponse
import com.elegant.software.blitzpay.payments.truelayer.api.QrPaymentStatus
import com.elegant.software.blitzpay.payments.truelayer.outbound.QrPaymentService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID

@WebFluxTest(QrCodeController::class)
class QrCodeControllerTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var qrPaymentService: QrPaymentService

    private val testPaymentId = UUID.randomUUID()

    private fun buildPaymentResponse(paymentId: UUID = testPaymentId) = QrPaymentResponse(
        success = true,
        paymentRequestId = paymentId,
        transactionId = "tx-123",
        status = QrPaymentStatus.INITIATED,
        qrCodeData = "https://payment.truelayer.com/checkout/abc",
        qrCodeImage = "base64image==",
        qrCodeUrl = "http://localhost:8080/api/qr-payments/$paymentId/image",
        deepLink = "truelayer://payment-link/abc",
        paymentUrl = "https://payment.truelayer.com/checkout/abc",
        merchant = "TestMerchant",
        amount = 5.00,
        currency = "EUR",
        expiresAt = Instant.now().plusSeconds(86400),
        message = "Scan QR code to pay 5.0 EUR at TestMerchant"
    )

    @Test
    fun `POST createQrPayment returns 201 with response body`() {
        whenever(qrPaymentService.initiateQrPayment(any())).thenReturn(buildPaymentResponse())

        webTestClient.post()
            .uri("/api/qr-payments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"merchant":"TestMerchant","amount":5.00,"currency":"EUR","orderDetails":"Latte"}""")
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.status").isEqualTo("INITIATED")
            .jsonPath("$.merchant").isEqualTo("TestMerchant")
    }

    @Test
    fun `GET quick returns 200 with response body`() {
        whenever(qrPaymentService.initiateQrPayment(any())).thenReturn(buildPaymentResponse())

        webTestClient.get()
            .uri("/api/qr-payments/quick?merchant=TestMerchant&amount=5.00&order=Latte")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.merchant").isEqualTo("TestMerchant")
    }

    @Test
    fun `GET health returns 200 UP`() {
        webTestClient.get()
            .uri("/api/qr-payments/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("qr-payments")
    }

    @Test
    fun `GET payment by ID returns 200 when found`() {
        whenever(qrPaymentService.getQrPayment(testPaymentId)).thenReturn(buildPaymentResponse())

        webTestClient.get()
            .uri("/api/qr-payments/$testPaymentId")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.paymentRequestId").isEqualTo(testPaymentId.toString())
    }

    @Test
    fun `GET payment by ID returns 404 when not found`() {
        whenever(qrPaymentService.getQrPayment(any())).thenReturn(null)

        webTestClient.get()
            .uri("/api/qr-payments/$testPaymentId")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isEqualTo("Payment not found")
    }

    @Test
    fun `GET payment by invalid ID returns 400`() {
        webTestClient.get()
            .uri("/api/qr-payments/not-a-uuid")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("Invalid payment request ID")
    }

    @Test
    fun `GET callback with success returns 200`() {
        webTestClient.get()
            .uri("/api/qr-payments/callback?payment_id=pay-123")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("success")
            .jsonPath("$.payment_id").isEqualTo("pay-123")
    }

    @Test
    fun `GET callback with error returns 200 with error status`() {
        webTestClient.get()
            .uri("/api/qr-payments/callback?error=payment_canceled&error_description=User+canceled+the+payment")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("error")
            .jsonPath("$.error").isEqualTo("payment_canceled")
            .jsonPath("$.message").isEqualTo("User canceled the payment")
    }

    @Test
    fun `GET qr image returns 200 with image bytes when found`() {
        val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        whenever(qrPaymentService.getQrImage(testPaymentId)).thenReturn(imageBytes)

        webTestClient.get()
            .uri("/api/qr-payments/$testPaymentId/image")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_PNG)
    }

    @Test
    fun `GET qr image returns 404 when not found`() {
        whenever(qrPaymentService.getQrImage(any())).thenReturn(null)

        webTestClient.get()
            .uri("/api/qr-payments/$testPaymentId/image")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isEqualTo("QR image not found")
    }
}
