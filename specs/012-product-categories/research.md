# Research: Merchant Product Categories

**Feature**: 012-product-categories  
**Date**: 2026-04-28

## Findings

### Decision: Case-Insensitive Uniqueness via Functional Index

**Decision**: Enforce per-merchant category name uniqueness case-insensitively using a PostgreSQL unique index on `(merchant_application_id, lower(name))`.

**Rationale**: "Drinks" and "drinks" are the same category from a merchant's perspective. A functional index is enforced at the DB layer — the safest boundary for uniqueness. No application-layer collation logic is needed.

**Alternatives considered**:
- Unique constraint on `(merchant_application_id, name)` — rejected: allows "Drinks" and "drinks" as separate categories, confusing UX.
- Normalise to lowercase before storing — rejected: loses original casing the merchant chose.

---

### Decision: FK from `merchant_products` to `merchant_product_categories` (nullable)

**Decision**: Add a nullable `product_category_id UUID` column to `merchant_products` referencing `merchant_product_categories(id)`. No `ON DELETE CASCADE` — deletion of a category with assigned products is blocked at the service layer.

**Rationale**: Matches the project convention (Liquibase owns FK constraints; `@OnDelete` is not used). Blocking deletion instead of cascading avoids silent data loss.

**Alternatives considered**:
- `ON DELETE SET NULL` cascade — rejected: would silently un-categorise all products when a category is deleted, surprising for merchants with large catalogues.
- `ON DELETE CASCADE` — rejected: would delete products, not just their category link.

---

### Decision: Row-Level Security on `merchant_product_categories`

**Decision**: Apply the same RLS pattern used on `merchant_products`: enable RLS, force it, and add a policy `USING (merchant_application_id = NULLIF(current_setting('app.current_merchant_id', true), '')::uuid)`.

**Rationale**: Tenant isolation is enforced at the DB layer for all merchant-scoped tables. Adding RLS to the new table follows the existing convention and prevents cross-merchant category exposure even in the event of service-layer bugs.

**Alternatives considered**:
- Application-layer-only isolation — rejected: the existing architecture uses dual isolation (Hibernate filter + RLS). Diverging here would be inconsistent.

---

### Decision: Extend existing `MerchantProductTools` rather than new class

**Decision**: Add category MCP tools to `MerchantProductTools` as additional `@McpTool` methods.

**Rationale**: The existing class already groups all merchant/product MCP operations. A separate class would require additional Spring wiring for the same dependencies.

**Alternatives considered**:
- New `MerchantProductCategoryTools` component — acceptable but unnecessary given the cohesion of existing tools.

---

### Decision: `categoryId` in product create/update as optional `UUID?` form field

**Decision**: Extend `CreateProductRequest` and `UpdateProductRequest` with an optional `categoryId: UUID?`. The controller reads it as an optional `@RequestPart`.

**Rationale**: Keeps the multipart form interface consistent with the existing product endpoint design. Null means "leave category unchanged" on update; null on create means "no category".

**Alternatives considered**:
- Separate `PATCH /{productId}/category` endpoint — rejected: increases surface area unnecessarily when the existing PUT already handles full updates.

---

### Decision: List categories endpoint returns all merchant categories, unsorted → alphabetical default

**Decision**: `MerchantProductCategoryRepository.findAllByMerchantApplicationId()` → service returns sorted by `name` ascending (case-insensitive). Controller exposes via `GET /product-categories`.

**Rationale**: Alphabetical ordering is the assumption documented in the spec. Spring Data derived query + in-memory sort is adequate for the expected volume (≤ 500 categories per merchant).

**Alternatives considered**:
- `ORDER BY lower(name)` in a `@Query` JPQL — equally valid but adds annotation noise for a simple case. In-memory sort is cleaner for small collections.

---

### Decision: No separate filter query for category in `findAllByActiveTrueAndMerchantBranchId`

**Decision**: Add an optional `categoryId: UUID?` parameter to `MerchantProductService.list()` and a new `findAllByActiveTrueAndMerchantBranchIdAndProductCategoryId` repository method when `categoryId` is non-null.

**Rationale**: Cleanest extension — avoids N+1, keeps the existing method for the unfiltered case.
