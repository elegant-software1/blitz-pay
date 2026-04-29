package com.elegant.software.blitzpay.invoice.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import com.elegant.software.blitzpay.payments.QuickpayApplication
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.util.function.Consumer

@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop", "spring.liquibase.enabled=false"])
@ContextConfiguration(classes = [QuickpayApplication::class])
@EnableJpaRepositories(basePackages = ["com.elegant.software.blitzpay"])
@EntityScan(basePackages = ["com.elegant.software.blitzpay"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class InvoiceRepositoryTest {

    @Autowired
    private lateinit var invoiceRepository: InvoiceRepository

    @Test
    fun `persists and loads invoice with person recipient`() {
        val invoice = Invoice(
            amount = BigDecimal("125.50"),
            paymentStatus = PaymentStatus.PENDING,
            createdAt = Instant.parse("2026-04-12T10:15:30Z")
        ).apply {
            addRecipient(
                InvoiceRecipient(
                    recipientType = RecipientType.PERSON,
                    displayName = "Alex Example",
                    email = "alex@example.com",
                    customerReference = "CUST-1001"
                )
            )
        }

        val saved = invoiceRepository.saveAndFlush(invoice)
        val loaded = requireNotNull(invoiceRepository.findById(saved.id).orElse(null))

        assertThat(loaded.createdAt).isEqualTo(Instant.parse("2026-04-12T10:15:30Z"))
        assertThat(loaded.amount).isEqualByComparingTo("125.50")
        assertThat(loaded.paymentStatus).isEqualTo(PaymentStatus.PENDING)
        assertThat(loaded.recipients).singleElement().satisfies(Consumer {
            assertThat(it.recipientType).isEqualTo(RecipientType.PERSON)
            assertThat(it.displayName).isEqualTo("Alex Example")
            assertThat(it.email).isEqualTo("alex@example.com")
            assertThat(it.customerReference).isEqualTo("CUST-1001")
        })
    }

    @Test
    fun `persists and loads invoice with group recipient`() {
        val invoice = Invoice(
            amount = BigDecimal("980.00"),
            paymentStatus = PaymentStatus.PAID
        ).apply {
            addRecipient(
                InvoiceRecipient(
                    recipientType = RecipientType.GROUP,
                    displayName = "Berlin Operations",
                    groupId = "group-berlin-ops",
                    groupName = "Berlin Operations",
                    customerReference = "TEAM-42"
                )
            )
        }

        val saved = invoiceRepository.saveAndFlush(invoice)
        val loaded = requireNotNull(invoiceRepository.findById(saved.id).orElse(null))

        assertThat(loaded.paymentStatus).isEqualTo(PaymentStatus.PAID)
        assertThat(loaded.recipients).singleElement().satisfies(Consumer {
            assertThat(it.recipientType).isEqualTo(RecipientType.GROUP)
            assertThat(it.displayName).isEqualTo("Berlin Operations")
            assertThat(it.groupId).isEqualTo("group-berlin-ops")
            assertThat(it.groupName).isEqualTo("Berlin Operations")
            assertThat(it.email).isNull()
        })
    }

    @Test
    fun `persists and loads invoice with multiple recipients`() {
        val invoice = Invoice(
            amount = BigDecimal("1500.00"),
            paymentStatus = PaymentStatus.RECEIVED
        ).apply {
            addRecipients(
                listOf(
                    InvoiceRecipient(
                        recipientType = RecipientType.PERSON,
                        displayName = "Taylor Example",
                        email = "taylor@example.com"
                    ),
                    InvoiceRecipient(
                        recipientType = RecipientType.GROUP,
                        displayName = "Shared Finance",
                        groupName = "Shared Finance"
                    )
                )
            )
        }

        val saved = invoiceRepository.saveAndFlush(invoice)
        val loaded = requireNotNull(invoiceRepository.findById(saved.id).orElse(null))

        assertThat(loaded.paymentStatus).isEqualTo(PaymentStatus.RECEIVED)
        assertThat(loaded.recipients).hasSize(2)
        assertThat(loaded.recipients.map { it.recipientType })
            .containsExactlyInAnyOrder(RecipientType.PERSON, RecipientType.GROUP)
    }

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
