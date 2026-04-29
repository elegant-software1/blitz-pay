# Data Model: Merchant Product Categories

**Feature**: 012-product-categories  
**Date**: 2026-04-28

## New Entity: MerchantProductCategory

### JPA Entity

```kotlin
// com.elegant.software.blitzpay.merchant.domain.MerchantProductCategory
@Entity
@Table(name = "merchant_product_categories", schema = "blitzpay")
class MerchantProductCategory(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "merchant_application_id", nullable = false, updatable = false) val merchantApplicationId: UUID,
    @Column(nullable = false, length = 100) var name: String,
    @Column(name = "created_at", nullable = false, updatable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = createdAt,
) {
    fun rename(newName: String, at: Instant = Instant.now()) {
        require(newName.isNotBlank()) { "name must not be blank" }
        require(newName.length <= 100) { "name must be <= 100 characters" }
        name = newName.trim()
        updatedAt = at
    }
}
```

### Liquibase Migration (new file: `20260428-003-merchant-product-categories.sql`)

```sql
-- changeset mehdi:20260428-003-merchant-product-categories
CREATE TABLE blitzpay.merchant_product_categories (
    id                      UUID         NOT NULL,
    merchant_application_id UUID         NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_merchant_product_categories PRIMARY KEY (id),
    CONSTRAINT fk_merchant_product_categories_application
        FOREIGN KEY (merchant_application_id)
        REFERENCES blitzpay.merchant_applications (id),
    CONSTRAINT chk_merchant_product_category_name_length CHECK (char_length(name) <= 100)
);
CREATE UNIQUE INDEX uq_merchant_product_category_name
    ON blitzpay.merchant_product_categories (merchant_application_id, lower(name));
CREATE INDEX ix_merchant_product_categories_merchant
    ON blitzpay.merchant_product_categories (merchant_application_id);
-- rollback DROP TABLE blitzpay.merchant_product_categories;

-- changeset mehdi:20260428-004-merchant-product-categories-rls
ALTER TABLE blitzpay.merchant_product_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE blitzpay.merchant_product_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY merchant_tenant_isolation
    ON blitzpay.merchant_product_categories
    USING (
        merchant_application_id = NULLIF(current_setting('app.current_merchant_id', true), '')::uuid
    );
-- rollback DROP POLICY merchant_tenant_isolation ON blitzpay.merchant_product_categories;
--         ALTER TABLE blitzpay.merchant_product_categories DISABLE ROW LEVEL SECURITY;
```

### Liquibase Migration (new file: `20260428-004-merchant-products-add-category-fk.sql`)

```sql
-- changeset mehdi:20260428-003-merchant-products-add-category-fk
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN product_category_id UUID,
    ADD CONSTRAINT fk_merchant_products_category
        FOREIGN KEY (product_category_id)
        REFERENCES blitzpay.merchant_product_categories (id);
CREATE INDEX ix_merchant_products_category
    ON blitzpay.merchant_products (product_category_id)
    WHERE product_category_id IS NOT NULL;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN product_category_id;
```

---

## Modified Entity: MerchantProduct

Add two nullable fields:

```kotlin
@Column(name = "product_category_id")
var productCategoryId: UUID? = null,

@Column(name = "product_code")
var productCode: Long? = null,
```

Extend the existing `update(...)` method signature to accept `productCategoryId: UUID?` and `productCode: Long?`.

### Liquibase Migration (new file: `20260429-001-product-code-support.sql`)

```sql
-- changeset mehdi:20260429-001-product-code-support
ALTER TABLE blitzpay.merchant_products
    ADD COLUMN product_code BIGINT;
CREATE UNIQUE INDEX uq_merchant_products_branch_product_code
    ON blitzpay.merchant_products (merchant_branch_id, product_code)
    WHERE product_code IS NOT NULL;
CREATE INDEX ix_merchant_products_branch_product_code_desc
    ON blitzpay.merchant_products (merchant_branch_id, product_code DESC)
    WHERE product_code IS NOT NULL;
-- rollback DROP INDEX IF EXISTS ix_merchant_products_branch_product_code_desc;
-- rollback DROP INDEX IF EXISTS uq_merchant_products_branch_product_code;
-- rollback ALTER TABLE blitzpay.merchant_products DROP COLUMN product_code;
```

---

## Repositories

### New: MerchantProductCategoryRepository

```kotlin
interface MerchantProductCategoryRepository : JpaRepository<MerchantProductCategory, UUID> {
    fun findAllByMerchantApplicationId(merchantApplicationId: UUID): List<MerchantProductCategory>
    fun findByMerchantApplicationIdAndId(merchantApplicationId: UUID, id: UUID): MerchantProductCategory?
    fun findByMerchantApplicationIdAndNameIgnoreCase(merchantApplicationId: UUID, name: String): MerchantProductCategory?
    fun existsByIdAndMerchantApplicationId(id: UUID, merchantApplicationId: UUID): Boolean
    fun existsByMerchantApplicationIdAndNameIgnoreCase(merchantApplicationId: UUID, name: String): Boolean
}
```

### Extended: MerchantProductRepository

Add one method:

```kotlin
fun findAllByActiveTrueAndMerchantBranchIdAndProductCategoryId(
    merchantBranchId: UUID,
    productCategoryId: UUID
): List<MerchantProduct>

fun findByMerchantBranchIdAndProductCode(merchantBranchId: UUID, productCode: Long): MerchantProduct?

@Query("select max(p.productCode) from MerchantProduct p where p.merchantBranchId = :merchantBranchId")
fun findMaxProductCodeByMerchantBranchId(merchantBranchId: UUID): Long?
```

---

## API Models

### New request/response types (in `merchant/api/MerchantProductCategoryModels.kt`)

```kotlin
data class CreateProductCategoryRequest(val name: String)

data class RenameProductCategoryRequest(val name: String)

data class ProductCategoryResponse(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Extended ProductResponse

Add three fields to `ProductResponse`:

```kotlin
val categoryId: UUID? = null,
val categoryName: String? = null,
val productCode: Long? = null,
```

### Extended CreateProductRequest / UpdateProductRequest

Add two optional fields to each:

```kotlin
val categoryId: UUID? = null,
val productCode: Long? = null,
```

---

## Entity Relationships

```
merchant_applications (1) ──< merchant_product_categories (many)
merchant_product_categories (1) ──< merchant_products (many, nullable FK)
merchant_applications (1) ──< merchant_products (many)
```

---

## Validation Rules

| Field | Rule |
|-------|------|
| `name` (category) | Non-blank, ≤ 100 chars after trim |
| `name` uniqueness | Case-insensitive per merchant (DB unique index) |
| `categoryId` on product | Must belong to same merchant, or null |
| `productCode` on product | Optional positive integer provided by caller or generated by system |
| `productCode` uniqueness | Unique within a branch |
| Duplicate provided `productCode` | Routes request to the existing branch product |
| Auto-generated `productCode` | Uses next highest numeric value in the branch and never reuses gaps |
| Category deletion | Blocked if any active product references it |
