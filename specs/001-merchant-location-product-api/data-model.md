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
   - active: Boolean (default true)
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
   - active: Boolean (default true)
   - createdAt, updatedAt

   Validation
   - branchId and locationId must exist and be active (unless business rule allows)
   - priceMinor required and non-negative

Indexes
- idx_locations_branch_id (branchId)
- idx_products_location_id (locationId)

Notes
- Keep branch ownership immutable on create; updates cannot change branchId.
- Use optimistic concurrency via updatedAt timestamp for simple conflict resolution.
