package com.elegant.software.blitzpay.invoice.internal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "invoice_recipients")
class InvoiceRecipient(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    val recipientType: RecipientType,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "email")
    val email: String? = null,

    @Column(name = "group_id")
    val groupId: String? = null,

    @Column(name = "group_name")
    val groupName: String? = null,

    @Column(name = "customer_reference")
    val customerReference: String? = null
) {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    lateinit var invoice: Invoice

    init {
        when (recipientType) {
            RecipientType.PERSON -> require(!email.isNullOrBlank()) {
                "person recipients must define email"
            }

            RecipientType.GROUP -> require(!groupId.isNullOrBlank() || !groupName.isNullOrBlank()) {
                "group recipients must define groupId or groupName"
            }
        }
    }

    internal fun attachTo(invoice: Invoice) {
        this.invoice = invoice
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InvoiceRecipient) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
