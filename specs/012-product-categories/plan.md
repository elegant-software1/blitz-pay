# Implementation Plan: Merchant Product Categories

**Branch**: `012-product-categories` | **Date**: 2026-04-28 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/012-product-categories/spec.md`

## Summary

Merchants need to organise their product catalogue into named categories (e.g., Drinks, Wine, Vegetables) that are scoped to their store. This feature adds a `merchant_product_categories` table, extends the existing product domain with an optional category FK and a branch-scoped numeric `productCode`, exposes four new REST endpoints for category management, extends the existing product endpoints to carry category data and `productCode`, and adds MCP support for category workflows and product-code-aware updates. When callers omit `productCode`, the application assigns the next highest numeric code within the branch; when callers provide an existing branch code, the request targets that existing product instead of creating a duplicate.

## Technical Context

**Language/Version**: Kotlin 2.3.20 on Java 25  
**Primary Dependencies**: Spring Boot 4.0.4, Spring WebFlux (reactive), Spring Modulith, Hibernate/JPA, Spring AI (MCP `@McpTool`)  
**Storage**: PostgreSQL 16 (`blitzpay` schema), `ddl-auto: none`, Liquibase for all schema changes  
**Testing**: JUnit 5 + Mockito Kotlin (unit), WebTestClient (contract tests, `contract-test` Spring profile)  
**Target Platform**: Linux server (JVM)  
**Project Type**: Web service  
**Performance Goals**: Category list ≤ 500 items per merchant; response times equivalent to existing product list endpoint  
**Constraints**: Schema changes only via Liquibase; no `@OnDelete` annotations; SLF4J logging only (`LoggerFactory.getLogger`); tenant isolation via Hibernate filter + PostgreSQL RLS on every merchant-scoped table  
**Scale/Scope**: Per-merchant catalogue; soft limit of 500 categories per merchant

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The constitution file is a placeholder template (not yet project-specific). Applying the architectural conventions from `CLAUDE.md` and `CONSTITUTION.md` instead:

| Gate | Status | Notes |
|------|--------|-------|
| Schema via Liquibase only | PASS | Two new migration files planned; no Hibernate DDL |
| No `@OnDelete` annotations | PASS | Deletion guard at service layer, not DB cascade |
| SLF4J logging | PASS | Using `LoggerFactory.getLogger` throughout |
| Spring Modulith — no cross-module bean coupling | PASS | All new code stays in `merchant` module |
| Tenant isolation (Hibernate filter + RLS) | PASS | RLS policy added to new table; Hibernate filter reused |
| Contract tests for new endpoints | PASS | `MerchantProductCategoryContractTest` planned |
| Module verification maintained | PASS | Existing `MerchantModularityTest` covers the module |

## Project Structure

### Documentation (this feature)

```text
specs/012-product-categories/
├── plan.md          ← this file
├── research.md      ← Phase 0 decisions
├── data-model.md    ← entity definitions, migrations, repositories
├── quickstart.md    ← local dev guide
├── contracts/
│   └── rest-api.md  ← REST + MCP tool contracts
├── checklists/
│   └── requirements.md
└── tasks.md         ← Phase 2 output (/speckit.tasks — not yet created)
```

### Source Code

```text
src/main/resources/db/changelog/
├── 20260428-003-merchant-product-categories.sql   ← new table + RLS
├── 20260428-004-merchant-products-add-category-fk.sql ← nullable FK column
└── 20260429-001-product-code-support.sql          ← product code column + branch uniqueness

src/main/kotlin/com/elegant/software/blitzpay/merchant/
├── domain/
│   ├── MerchantProduct.kt                  ← add productCategoryId + productCode fields
│   └── MerchantProductCategory.kt          ← NEW entity
├── repository/
│   ├── MerchantProductRepository.kt        ← add category-filter + productCode methods
│   └── MerchantProductCategoryRepository.kt ← NEW repo
├── api/
│   ├── MerchantProductModels.kt            ← extend responses + requests with productCode
│   └── MerchantProductCategoryModels.kt    ← NEW models
├── application/
│   ├── MerchantProductService.kt           ← pass categoryId through and resolve productCode upserts
│   └── MerchantProductCategoryService.kt   ← NEW service
├── web/
│   ├── MerchantProductController.kt        ← accept optional categoryId + productCode
│   └── MerchantProductCategoryController.kt ← NEW controller
└── mcp/
    └── MerchantProductTool.kt              ← category tools + productCode-aware update/upsert

src/test/kotlin/com/elegant/software/blitzpay/merchant/
└── application/
    ├── MerchantProductServiceTest.kt       ← extend existing tests
    └── MerchantProductCategoryServiceTest.kt ← NEW unit tests

src/contractTest/kotlin/com/elegant/software/blitzpay/merchant/
└── MerchantProductCategoryContractTest.kt  ← NEW contract tests
```

**Structure Decision**: Single-module extension. All new code lives under `com.elegant.software.blitzpay.merchant` following the existing `domain / repository / api / application / web / mcp` layering. No new modules or packages are introduced.

## Implementation Sequence

### Step 1 — Database migrations

1. Create `20260428-003-merchant-product-categories.sql`:
   - Table `blitzpay.merchant_product_categories` with PK, FK to `merchant_applications`, unique index on `(merchant_application_id, lower(name))`, name length check
   - RLS: enable, force, tenant isolation policy (same pattern as `merchant_products`)

2. Create `20260428-004-merchant-products-add-category-fk.sql`:
   - `ALTER TABLE blitzpay.merchant_products ADD COLUMN product_category_id UUID`
   - FK constraint → `merchant_product_categories(id)` (no cascade)
   - Partial index on `product_category_id WHERE product_category_id IS NOT NULL`

### Step 2 — Domain layer

3. Create `MerchantProductCategory` JPA entity (see `data-model.md` for full definition).

4. Extend `MerchantProduct`:
   - Add `@Column(name = "product_category_id") var productCategoryId: UUID? = null`
   - Add `@Column(name = "product_code") var productCode: Long? = null`
   - Add `productCategoryId: UUID?` and `productCode: Long?` parameters to `update()` method

### Step 3 — Repository layer

5. Create `MerchantProductCategoryRepository` (see `data-model.md`).

6. Extend `MerchantProductRepository`:
   - Add `findAllByActiveTrueAndMerchantBranchIdAndProductCategoryId(merchantBranchId: UUID, productCategoryId: UUID): List<MerchantProduct>`
   - Add branch-scoped `productCode` lookup and max-code query methods

### Step 4 — API models

7. Create `MerchantProductCategoryModels.kt`:
   - `CreateProductCategoryRequest(name: String)`
   - `RenameProductCategoryRequest(name: String)`
   - `ProductCategoryResponse(id, name, createdAt, updatedAt)`

8. Extend `MerchantProductModels.kt`:
   - `ProductResponse`: add `val categoryId: UUID? = null`, `val categoryName: String? = null`, and `val productCode: Long? = null`
   - `CreateProductRequest`: add `val categoryId: UUID? = null` and `val productCode: Long? = null`
   - `UpdateProductRequest`: add `val categoryId: UUID? = null` and `val productCode: Long? = null`

### Step 5 — Service layer

9. Create `MerchantProductCategoryService` with:
   - `create(merchantId, name)` — validate, check duplicate (case-insensitive), save
   - `list(merchantId)` — return sorted alphabetically
   - `rename(merchantId, categoryId, newName)` — validate, check duplicate, update
   - `delete(merchantId, categoryId)` — check no active products assigned, then delete

10. Extend `MerchantProductService`:
    - `create(...)`: accept optional `categoryId`; validate it belongs to merchant; persist
    - `create(...)`: if `productCode` is omitted, assign the next highest numeric code within the branch
    - `create/update(...)`: if the supplied `productCode` already exists in the same branch, route the request to that existing product
    - `update(...)`: accept optional `categoryId`; validate ownership; update field
    - `list(merchantId, merchantBranchId, categoryId: UUID? = null)`: use category-filtered repo method when `categoryId` non-null
    - `get(...)`: populate `categoryId`/`categoryName` and `productCode` on response

### Step 6 — Web layer

11. Create `MerchantProductCategoryController`:
    - `POST /product-categories` → 201
    - `GET /product-categories` → 200
    - `PUT /product-categories/{categoryId}` → 200
    - `DELETE /product-categories/{categoryId}` → 204 / 409
    - `@ExceptionHandler` for `IllegalArgumentException` (400), `NoSuchElementException` (404), `IllegalStateException` (409)

12. Extend `MerchantProductController`:
    - `create`: add optional `@RequestPart("categoryId", required = false) categoryId: String?`
    - `create`: add optional `@RequestPart("productCode", required = false) productCode: String?`
    - `list`: add optional `@RequestParam("categoryId", required = false) categoryId: UUID?`
    - `get`: no controller change needed (populated by service)
    - `update`: add optional `@RequestPart("categoryId", required = false) categoryId: String?`
    - `update`: add optional `@RequestPart("productCode", required = false) productCode: String?`

### Step 7 — MCP layer

13. Extend `MerchantProductTools` with three new `@McpTool` methods:
    - `getCategoryIdByName(merchantId, categoryName)` — look up by name, throw if not found
    - `getOrCreateCategoryId(merchantId, categoryName)` — find or create
    - `listProductCategories(merchantId)` — return list of `ProductCategoryResponse`

14. Extend `updateProduct` MCP tool:
    - Add optional `categoryId: String? = null` parameter
    - Add optional `productCode: String? = null` parameter
    - Pass through to `UpdateProductRequest`

### Step 8 — Tests

15. Create `MerchantProductCategoryServiceTest`:
    - Create: success, duplicate name (case-insensitive), blank name
    - List: returns alphabetical order
    - Rename: success, duplicate target name
    - Delete: success (no products), blocked (has products)

16. Extend `MerchantProductServiceTest`:
    - Create/update with valid categoryId
    - Create/update with categoryId belonging to different merchant → error
    - List with categoryId filter
    - Explicit `productCode`, generated `productCode`, and duplicate-code targeting behavior

17. Create `MerchantProductCategoryContractTest` (WebTestClient, `contract-test` profile):
    - All four category endpoints: happy paths + error cases
    - Product create/list with category

## Complexity Tracking

No constitution violations to justify.
