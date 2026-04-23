# Data Model: merchant-location-product-api

Entities

1. MerchantLocation
   - id: UUID (PK)
   - branchId: UUID (FK to merchant_branches) (immutable)
   - name: String
   - addressLine1..addressLine2, city, postalCode, country: String
   - latitude: Double
   - longitude: Double
   - googlePlaceId: String?
   - active: Boolean (default true for REST/API managed records; MCP-created branch placeholders default inactive at the branch level)
   - createdAt: Instant
   - updatedAt: Instant

   Validation
   - branchId required and must exist
   - latitude between -90..90, longitude -180..180
   - name non-blank

2. Product
   - id: UUID (PK)
   - branchId: UUID (FK)
   - locationId: UUID (FK to MerchantLocation)
   - name: String
   - description: String?
   - priceMinor: Integer (>=0)
   - currency: ISO-4217 code
   - imageStorageKey: String? (stable private object-storage key)
   - active: Boolean (default true for REST/API creation; default false for MCP-created ingestion records)
   - createdAt, updatedAt

   Validation
   - branchId and locationId must exist and be active (unless business rule allows)
   - priceMinor required and non-negative
   - MCP image ingestion accepts JPEG, PNG, or WebP up to the product image size limit
   - MCP crop coordinates must be complete (`cropX`, `cropY`, `cropWidth`, `cropHeight`), positive for width/height, and inside source image bounds

Indexes
- idx_locations_branch_id (branchId)
- idx_products_location_id (locationId)

Notes
- Keep branch ownership immutable on create; updates cannot change branchId.
- Use optimistic concurrency via updatedAt timestamp for simple conflict resolution.
- MCP helper tools may create missing branches/products by name, but new MCP-created records remain inactive until reviewed.
- MCP lookup/update paths include inactive branches/products to prevent duplicate draft records during repeated ingestion.
- Product image references are object keys only; signed URLs are generated at read time after access checks.
