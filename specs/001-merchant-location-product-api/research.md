# Research: merchant-location-product-api

Decision: Expose RESTful endpoints on MCP server under /v1/branches/{branchId}/locations and /v1/branches/{branchId}/locations/{locationId}/products.

Rationale: REST is consistent with existing MCP APIs and easy for LLM or AI agents to call. Using branchId path parameter enforces association with existing branch and prevents implicit branch creation.

Idempotency: Support client-supplied id or Idempotency-Key header for create endpoints to avoid duplicates.

Auth/Authorization: Leverage existing MCP auth; endpoints must validate tenant/branch ownership. Assume JWT or existing session mechanism.

Error semantics: 201 for create, 200 for update, 400 for validation, 404 for missing branch/location, 409 for conflict (inactive location/business rule).

Alternatives considered:
- GraphQL: rejected due to mismatch with project REST patterns.
- Embedded resources vs separate tables: separate tables chosen for clarity and per-location product scoping.

Security notes:
- Validate branch ownership server-side; do not trust branchId in JWT alone.
- Rate-limit agent calls and log audit metadata.
