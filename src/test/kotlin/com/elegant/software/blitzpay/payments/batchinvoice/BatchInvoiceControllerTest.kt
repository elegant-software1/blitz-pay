package com.elegant.software.blitzpay.payments.batchinvoice

import com.elegant.software.blitzpay.batchinvoice.BatchInvoiceController
import com.elegant.software.blitzpay.batchinvoice.BatchInvoiceService
import com.elegant.software.blitzpay.batchinvoice.BatchRunState
import com.elegant.software.blitzpay.batchinvoice.BatchRunStatus

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID
import org.mockito.kotlin.whenever

@WebFluxTest(BatchInvoiceController::class)
class BatchInvoiceControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var batchInvoiceService: BatchInvoiceService

    @Test
    fun `POST starts batch run`() {
        val runId = UUID.randomUUID()
        whenever(batchInvoiceService.start("/tmp/invoices.xlsx")).thenReturn(
            BatchRunStatus(
                runId = runId,
                filePath = "/tmp/invoices.xlsx",
                state = BatchRunState.PENDING,
                startedAt = Instant.now()
            )
        )

        webTestClient.post()
            .uri("/v1/batch/invoices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"filePath":"/tmp/invoices.xlsx"}""")
            .exchange()
            .expectStatus().isAccepted
            .expectBody()
            .jsonPath("$.runId").isEqualTo(runId.toString())
            .jsonPath("$.state").isEqualTo("PENDING")
    }
}
