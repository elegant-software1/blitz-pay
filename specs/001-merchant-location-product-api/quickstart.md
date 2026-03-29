# Quickstart: Merchant Location & Product API

Endpoints

- Create Location: POST /v1/branches/{branchId}/locations
- Create Product: POST /v1/branches/{branchId}/locations/{locationId}/products

Headers

- Authorization: Bearer <token>
- Idempotency-Key: <client-provided-key> (optional for idempotent create)

Example: create location

curl -X POST "https://mcp.example/v1/branches/{branchId}/locations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen || echo key123)" \
  -d '{"name":"Main Store","addressLine1":"1 High St","latitude":51.5,"longitude":-0.12}'

Example: create product

curl -X POST "https://mcp.example/v1/branches/{branchId}/locations/{locationId}/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Coffee","priceMinor":250,"currency":"EUR"}'

