# Data Model: Merchant Onboarding - Product Image Upload API

**Feature Branch**: `001-merchant-onboarding`  
**Date**: 2026-04-21  
**Scope**: `MerchantProduct` image metadata and S3-compatible object key semantics

---

## Existing Entities (unchanged)

| Entity | Table | Notes |
|--------|-------|-------|
| `MerchantApplication` | `blitzpay.merchant_applications` | Root merchant/onboarding aggregate; `id` is tenant key |
| `BusinessProfile` | embedded in `merchant_applications` | Existing `logo_storage_key` pattern stores object keys |
| `MerchantBranch` | `blitzpay.merchant_branches` | Optional branch relationship from payment feature; product may carry `merchant_branch_id` |
| `MerchantLocation` | `blitzpay.merchant_locations` | Existing optional location entity |

---

## Entity: `MerchantProduct`

### Table: `blitzpay.merchant_products`

The table already exists. This increment normalizes product image semantics:

- Product rows store image object keys, not direct URLs.
- Existing `image_url` column currently present in code/database is treated as legacy naming and should be renamed or superseded by `image_storage_key`.
- If the `merchant_product_images` table remains in the active schema, only one image is in scope for this feature; use `display_order = 0` as the primary product image.

### Recommended Liquibase Migration

```sql
-- liquibase formatted sql

-- changeset mehdi:20260421-005-product-image-storage-key-column
ALTER TABLE blitzpay.merchant_products
    RENAME COLUMN image_url TO image_storage_key;
-- rollback ALTER TABLE blitzpay.merchant_products RENAME COLUMN image_storage_key TO image_url;
```

If the active schema already dropped `merchant_products.image_url` and uses `merchant_product_images.storage_key`, do not recreate URL semantics. Keep the image key column/table as `storage_key` and map the domain as `imageStorageKey`.

### Field Descriptions

| Field | Column | Type | Nullable | Rules |
|-------|--------|------|----------|-------|
| `id` | `id` | `UUID` | NO | Product primary key |
| `merchantApplicationId` | `merchant_application_id` | `UUID` | NO | Tenant discriminator; FK to `merchant_applications.id` |
| `merchantBranchId` | `merchant_branch_id` | `UUID` | YES | Optional branch association used by payment routing |
| `name` | `name` | `VARCHAR(255)` | NO | Trimmed, non-blank |
| `description` | `description` | `VARCHAR(2000)` | YES | Optional rich-text/Markdown description |
| `unitPrice` | `unit_price` | `DECIMAL(12,4)` | NO | `>= 0`; no per-product currency |
| `imageStorageKey` | `image_storage_key` or `storage_key` | `VARCHAR(2048)` | YES | Stable S3/MinIO object key; never a direct URL |
| `active` | `active` | `BOOLEAN` | NO | Soft delete flag |
| `createdAt` | `created_at` | `TIMESTAMPTZ` | NO | Insert timestamp |
| `updatedAt` | `updated_at` | `TIMESTAMPTZ` | NO | Updated on mutation |

### Object Key Format

Use deterministic, tenant-scoped keys:

```text
merchants/{merchantId}/products/{productId}/image.{extension}
```

Rules:

- `{merchantId}` and `{productId}` are UUIDs.
- Extension is derived from validated content type: `.jpg`, `.png`, or `.webp`.
- Never accept a client-provided object key.
- Replacing an image writes a new object at the deterministic product key and stores that key on the product.

### Kotlin Domain Shape

```kotlin
@Entity
@Table(name = "merchant_products", schema = "blitzpay")
class MerchantProduct(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "merchant_application_id", nullable = false, updatable = false)
    val merchantApplicationId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(length = 2000)
    var description: String? = null,

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    var unitPrice: BigDecimal,

    @Column(name = "image_storage_key")
    var imageStorageKey: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt,

    @Column(name = "merchant_branch_id")
    var merchantBranchId: UUID? = null
) {
    fun update(name: String, description: String?, unitPrice: BigDecimal, imageStorageKey: String?, at: Instant = Instant.now()) {
        require(name.isNotBlank()) { "name must not be blank" }
        require(description == null || description.length <= 2000) { "description must be <= 2000 characters" }
        require(unitPrice >= BigDecimal.ZERO) { "unitPrice must be >= 0" }
        this.name = name.trim()
        this.description = description?.trim()?.ifBlank { null }
        this.unitPrice = unitPrice
        this.imageStorageKey = imageStorageKey
        this.updatedAt = at
    }

    fun deactivate(at: Instant = Instant.now()) {
        active = false
        updatedAt = at
    }
}
```

---

## API Models

### Multipart Write Parts

Product create/update does not use JSON bodies for the primary write path in this increment.

| Part | Type | Required | Validation |
|------|------|----------|------------|
| `name` | form field | YES | non-blank, max 255 chars |
| `description` | form field | NO | rich-text/Markdown, max 2,000 chars |
| `unitPrice` | form field | YES | decimal `>= 0`, max 4 fractional digits |
| `image` | file part | NO | JPEG, PNG, or WebP; max 5 MB |

### Response Model

```kotlin
data class ProductResponse(
    val productId: UUID,
    val merchantId: UUID,
    val name: String,
    val description: String?,
    val unitPrice: BigDecimal,
    val imageUrl: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

`imageUrl` is a generated signed retrieval URL, not a persisted value.

---

## Storage Boundary

### Storage Service Extension

```kotlin
interface StorageService {
    fun upload(storageKey: String, contentType: String, bytes: ByteArray)
    fun presignDownload(storageKey: String, ttlMinutes: Long = 60): String
    fun delete(storageKey: String)
}
```

Implementation uses existing AWS SDK `S3Client` and `S3Presigner` configured by `blitzpay.storage.*`.

### Validation Rules

- Reject missing required form fields with HTTP 400.
- Reject descriptions longer than 2,000 characters before DB write.
- Reject unsupported `Content-Type` before object upload.
- Reject files larger than 5 MB before DB write.
- Reject storage upload failures and do not persist product changes.
- On read, if `imageStorageKey` is null or signing/object resolution fails, return `imageUrl: null`.

---

## State Transitions

| Operation | Image State Result |
|-----------|--------------------|
| Create without image | `imageStorageKey = null` |
| Create with image | Upload object, then store generated key |
| Update without image part | Keep existing image key |
| Update with valid image | Upload/replace object, then store generated key |
| Update with invalid image | Reject request; keep existing product unchanged |
| Deactivate product | Leave image key/object unchanged for retained record |

---

## Tenant Isolation

- Product rows remain scoped by `merchant_application_id`.
- Object keys include `merchantId` and `productId`.
- API must validate that the authenticated principal is entitled to `{merchantId}` before accepting an upload.
- Signed URLs are generated only after product access checks pass.
