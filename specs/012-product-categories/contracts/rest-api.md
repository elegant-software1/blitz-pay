# REST API Contracts: Merchant Product Categories

**Feature**: 012-product-categories  
**Date**: 2026-04-28  
**Base path**: `/{version}/merchants/{merchantId}` (version defaults to `v1`)

---

## New Endpoints

### POST `/product-categories` — Create a category

**Request** (`application/json`):
```json
{ "name": "Drinks" }
```

**Response 201** (`application/json`):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Drinks",
  "createdAt": "2026-04-28T10:00:00Z",
  "updatedAt": "2026-04-28T10:00:00Z"
}
```

**Response 400** — name blank, exceeds 100 chars, or duplicate (case-insensitive):
```json
{ "error": "A category named 'drinks' already exists for this merchant" }
```

**Response 404** — merchant not found:
```json
{ "error": "Merchant not found: <merchantId>" }
```

---

### GET `/product-categories` — List all categories

**Response 200** (`application/json`):
```json
[
  { "id": "...", "name": "Drinks", "createdAt": "...", "updatedAt": "..." },
  { "id": "...", "name": "Vegetables", "createdAt": "...", "updatedAt": "..." }
]
```

Results are ordered alphabetically by name (case-insensitive).

---

### PUT `/product-categories/{categoryId}` — Rename a category

**Request** (`application/json`):
```json
{ "name": "Soft Drinks" }
```

**Response 200** (`application/json`): Updated `ProductCategoryResponse` (same shape as POST 201).

**Response 400** — name blank, exceeds 100 chars, or duplicate:
```json
{ "error": "A category named 'soft drinks' already exists for this merchant" }
```

**Response 404** — category not found for this merchant:
```json
{ "error": "Category not found: <categoryId>" }
```

---

### DELETE `/product-categories/{categoryId}` — Delete a category

**Response 204** — successfully deleted.

**Response 409 Conflict** — category still has active products assigned:
```json
{ "error": "Cannot delete category 'Drinks': 5 product(s) are still assigned to it" }
```

**Response 404** — category not found for this merchant.

---

## Modified Endpoints

### POST `/products` — Create a product (extended)

Add optional field to multipart form:

| Part | Type | Required | Description |
|------|------|----------|-------------|
| `categoryId` | UUID string | No | Category to assign; must belong to this merchant |
| `productCode` | Integer string | No | Branch-scoped product code; if omitted the system generates the next highest numeric code in the branch |

**Response 201**: `ProductResponse` now includes:
```json
{
  "categoryId": "3fa85f64-...",
  "categoryName": "Drinks",
  "productCode": 12,
  ...existing fields...
}
```

`categoryId` and `categoryName` are `null` when no category is assigned.
If `productCode` is supplied and already exists in the same branch, the request updates the existing product with that code instead of creating a duplicate product row.

---

### GET `/products` — List products (extended)

Add optional query parameter:

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `categoryId` | UUID | No | Filter to products in this category; omit to return all |

**Response 200**: Each item in the array now includes `categoryId`, `categoryName`, and `productCode`.

---

### GET `/products/{productId}` — Get a product (extended)

**Response 200**: Includes `categoryId`, `categoryName` (null if uncategorised), and `productCode`.

---

### PUT `/products/{productId}` — Update a product (extended)

Add optional field to multipart form:

| Part | Type | Required | Description |
|------|------|----------|-------------|
| `categoryId` | UUID string | No | Assign or change category; send empty string to clear |
| `productCode` | Integer string | No | Set or retarget by branch-scoped product code; existing code in the branch targets that product |

---

## New MCP Tools

### `category_id_by_name`
Get a category ID by name for a given merchant.

**Input**:
| Field | Type | Required |
|-------|------|----------|
| `merchantId` | UUID string | Yes |
| `categoryName` | String | Yes |

**Output**: Category UUID string. Throws if not found.

---

### `category_id_by_name_or_create`
Get or create a category by name for a given merchant.

**Input**:
| Field | Type | Required |
|-------|------|----------|
| `merchantId` | UUID string | Yes |
| `categoryName` | String | Yes |

**Output**: Category UUID string (existing or newly created).

---

### `merchant_list_product_categories`
List all categories for a merchant.

**Input**:
| Field | Type | Required |
|-------|------|----------|
| `merchantId` | UUID string | Yes |

**Output**: JSON array of `{ id, name, createdAt, updatedAt }`.

---

### `merchant_product_assign_category`
Assign or clear a category on a product (wraps the existing `merchant_product_update` tool with category support).

**Input**:
| Field | Type | Required |
|-------|------|----------|
| `merchantId` | UUID string | Yes |
| `productId` | UUID string | Yes |
| `branchId` | UUID string | Yes |
| `categoryId` | UUID string / null | No — null clears the category |
| `productCode` | Integer string / null | No — null preserves current code; existing code in the branch targets that product |

**Output**: Updated `ProductResponse` including `categoryId`, `categoryName`, and `productCode`.
