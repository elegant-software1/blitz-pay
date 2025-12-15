package de.elegantsoftware.blitzpay.invoice

import com.fasterxml.jackson.annotation.JsonIgnore
import de.elegantsoftware.blitzpay.product.Product
import jakarta.persistence.*

@Entity
@Table(name = "invoice_item")
data class InvoiceItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val quantity: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @JsonIgnore
    val invoice: Invoice
)
