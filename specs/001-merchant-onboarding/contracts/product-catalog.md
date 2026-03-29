# API Contract: Merchant Product Catalog

**Feature Branch**: `001-merchant-onboarding`  
**Date**: 2026-04-21  
**Module**: `merchant`  
**Base path**: `/v1/merchants/{merchantId}/products`

---

## Security

All endpoints require an authenticated principal. `{merchantId}` in the path is validated against the principal's merchant reference:

- `MERCHANT_APPLICANT`: may only access their own `{merchantId}`.
- `OPERATIONS_REVIEWER` / `SYSTEM`: may access any `{merchantId}`.

Requests where `{merchantId}` does not match the principal's entitlement return `403 Forbidden`.

Product images are private objects in S3-compatible storage. API responses may include `imageUrl`, but that value is a short-lived signed retrieval URL generated after authorization succeeds.

---

## Common Response Shape

```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "merchantId": "7b3d9f00-1234-4abc-8765-000000000001",
  "name": "Artisan Coffee Blend 250g",
  "description": "**Single-origin** medium roast with cocoa notes.",
  "unitPrice": 12.50,
  "imageUrl": "http://localhost:9000/blitzpay/merchants/7b3d9f00-1234-4abc-8765-000000000001/products/550e8400-e29b-41d4-a716-446655440000/image.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256...",
  "active": true,
  "createdAt": "2026-04-21T10:00:00Z",
  "updatedAt": "2026-04-21T10:00:00Z"
}
```

Rules:

- `imageUrl` is `null` when no image is stored or when the stored object key cannot be resolved.
- `imageUrl` is never persisted as a database URL.
- The persisted image reference is a server-generated object key.

---

## 1. Create Product

**`POST /v1/merchants/{merchantId}/products`**

Creates a new active product for the given merchant. Accepts multipart product fields and an optional image file.

### Request

```http
POST /v1/merchants/{merchantId}/products
Content-Type: multipart/form-data
Accept: application/json
```

| Part | Type | Required | Constraints |
|------|------|----------|-------------|
| `name` | form field | YES | 1-255 characters after trim |
| `description` | form field | NO | Rich-text/Markdown, max 2,000 characters |
| `unitPrice` | form field | YES | Decimal number `>= 0`, up to 4 fractional digits |
| `image` | file | NO | JPEG, PNG, or WebP; max 5 MB |

Example:

```bash
curl -X POST "http://localhost:8080/v1/merchants/{merchantId}/products" \
  -H "Accept: application/json" \
  -F "name=Artisan Coffee Blend 250g" \
  -F "description=**Single-origin** medium roast with cocoa notes." \
  -F "unitPrice=12.50" \
  -F "image=@./coffee.webp;type=image/webp"
```

### Response `201 Created`

Returns the common product response shape.

### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | `name` blank, description longer than 2,000 characters, `unitPrice` negative/non-numeric, unsupported image type, image larger than 5 MB |
| `403 Forbidden` | Principal not entitled to `{merchantId}` |
| `404 Not Found` | `{merchantId}` does not exist |
| `502 Bad Gateway` or `503 Service Unavailable` | Object storage upload failed; no product record is persisted |

---

## 2. List Active Products

**`GET /v1/merchants/{merchantId}/products`**

Returns all active products for the merchant. Inactive products are excluded.

### Response `200 OK`

```json
{
  "merchantId": "7b3d9f00-1234-4abc-8765-000000000001",
  "products": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "merchantId": "7b3d9f00-1234-4abc-8765-000000000001",
      "name": "Artisan Coffee Blend 250g",
      "description": "**Single-origin** medium roast with cocoa notes.",
      "unitPrice": 12.50,
      "imageUrl": "http://localhost:9000/blitzpay/merchants/.../image.webp?X-Amz-Algorithm=AWS4-HMAC-SHA256...",
      "active": true,
      "createdAt": "2026-04-21T10:00:00Z",
      "updatedAt": "2026-04-21T10:00:00Z"
    }
  ]
}
```

Empty catalog returns `products: []`, not `404`.

---

## 3. Get Product

**`GET /v1/merchants/{merchantId}/products/{productId}`**

Returns a single active product. Returns `404` for both non-existent and inactive products to prevent enumeration.

### Response `200 OK`

Returns the common product response shape.

### Error Responses

| Status | Condition |
|--------|-----------|
| `403 Forbidden` | Principal not entitled to `{merchantId}` |
| `404 Not Found` | Product not found or soft-deleted |

---

## 4. Update Product

**`PUT /v1/merchants/{merchantId}/products/{productId}`**

Replaces mutable product fields. If an `image` part is present, the server validates and uploads it, then stores the resulting object key. If no `image` part is present, the existing image key remains unchanged.

### Request

```http
PUT /v1/merchants/{merchantId}/products/{productId}
Content-Type: multipart/form-data
Accept: application/json
```

| Part | Type | Required | Constraints |
|------|------|----------|-------------|
| `name` | form field | YES | 1-255 characters after trim |
| `description` | form field | NO | Rich-text/Markdown, max 2,000 characters |
| `unitPrice` | form field | YES | Decimal number `>= 0`, up to 4 fractional digits |
| `image` | file | NO | JPEG, PNG, or WebP; max 5 MB |

### Response `200 OK`

Returns the common product response shape with a fresh signed `imageUrl` when an image is present.

### Error Responses

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Validation failure, unsupported image type, oversized image |
| `403 Forbidden` | Principal not entitled |
| `404 Not Found` | Product not found or soft-deleted |
| `502 Bad Gateway` or `503 Service Unavailable` | Object storage upload failed; existing product remains unchanged |

---

## 5. Deactivate Product

**`DELETE /v1/merchants/{merchantId}/products/{productId}`**

Sets `active = false`. The product and image key are retained for record-keeping.

### Response `204 No Content`

No body.

### Error Responses

| Status | Condition |
|--------|-----------|
| `403 Forbidden` | Principal not entitled |
| `404 Not Found` | Product not found or already inactive |

---

## Validation Details

| Rule | Result |
|------|--------|
| Missing `name` or blank `name` | `400 Bad Request` |
| `description` longer than 2,000 characters | `400 Bad Request` |
| Negative `unitPrice` | `400 Bad Request` |
| Image content type not in `image/jpeg`, `image/png`, `image/webp` | `400 Bad Request` |
| Image exceeds 5 MB | `400 Bad Request` |
| Storage upload fails | Product create/update fails; no partial product record is persisted |
| Stored image key cannot be signed/resolved on read | Response uses `imageUrl: null` |

---

## Tenant Isolation Guarantee

Every product response contains only products where `merchant_application_id = {merchantId}`:

- Enforced at application layer via Hibernate `tenantFilter`
- Enforced at DB layer via PostgreSQL RLS policy `merchant_tenant_isolation`
- Object keys are scoped under `merchants/{merchantId}/products/{productId}/...`
