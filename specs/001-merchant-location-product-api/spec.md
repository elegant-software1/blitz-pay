# Feature Specification: merchant-location-product-api

**Feature Branch**: `001-merchant-onboarding`
**Created**: 2026-04-21
**Status**: Draft
**Input**: "Expose merchant location and product create/update API on the MCP server so other LLM/AI agents can create and update merchant locations and products for existing branches (do not create new branches)."

## User Scenarios & Testing (mandatory)

### User Story 1 - Create Merchant Location (Priority: P1)
An automated agent or admin needs to register a physical or operational location for an existing MerchantBranch so customers and downstream systems can reference it.

Independent Test: Call the MCP API to create a location with branchId; verify the location is persisted and returned with id, createdAt, and updatedAt.

Acceptance Scenarios:
1. Given an existing branch, when the agent calls POST /v1/branches/{branchId}/locations with valid payload, then a 201 is returned and location is associated to the branch.
2. Given an unknown branch, when the same call is made, then a 404 is returned.

---

### User Story 2 - Create Product for a Location (Priority: P1)
An automated agent needs to create a product tied to a specific MerchantLocation so payments and listings can reference location-scoped products.

Independent Test: Call POST /v1/branches/{branchId}/locations/{locationId}/products; verify 201 and product fields persisted.

Acceptance Scenarios:
1. Given an existing location, when creating a product with required fields (name, priceMinor, currency), then a 201 and product id are returned.
2. Given an unknown location, a 404 is returned.

---

### User Story 3 - Update Location and Product (Priority: P2)
Agents must be able to update mutable fields (address, geolocation, product price/availability) without changing branch ownership.

Independent Test: PATCH endpoints update fields and return 200 with updated resource and updatedAt changed.

Acceptance Scenarios:
1. Valid updates return 200 and reflect new values.
2. Concurrent updates are handled idempotently (last-writer-wins by updatedAt) and return appropriate status.

---

## Edge Cases

- Creation requests for locations/products with duplicate external IDs should be idempotent.
- Partial updates with invalid data must return 400 without mutating persisted resource.
- Product creation when the branch exists but location is inactive should return 409 or configurable policy.

## Requirements (mandatory)

### Functional Requirements

- FR-001: MCP MUST expose create and update APIs for MerchantLocation tied to an existing MerchantBranch.
- FR-002: MCP MUST expose create and update APIs for Product scoped to a MerchantLocation.
- FR-003: API MUST validate that branchId exists and belongs to the requesting tenant; calls that would create a new Branch MUST be rejected.
- FR-004: API MUST return standard HTTP status codes: 201 on create, 200 on update, 400 on validation errors, 404 for missing parent resources, 409 for business conflicts.
- FR-005: Persistent fields: id, branchId, locationId (for product), createdAt, updatedAt, active flag.
- FR-006: API MUST support idempotent create (client-provided idempotency key or id) to avoid duplicates.
- FR-007: Auditing: user/agent identity and request metadata recorded for create/update actions.
- FR-008: Updates MUST support partial updates via PATCH for MerchantLocation and Product; PUT (full replace) optional.
### Key Entities

- MerchantBranch (existing)
- MerchantLocation: id, branchId, name, address fields, geolocation (lat/long), googlePlaceId, active, createdAt, updatedAt
- Product: id, branchId, locationId, name, description, priceMinor (integer), currency, active, createdAt, updatedAt

## Success Criteria (mandatory)

- SC-001: Agents can create a MerchantLocation and Product and receive a persistent id within one request cycle (end-to-end success measured by 201 + read-back verification).
- SC-002: 95% of valid create/update requests complete within acceptable latency for agent workflows (user-facing "near-instant").
- SC-003: All created/updated resources include audit metadata (creator id, timestamp).
- SC-004: Attempts to create resources that would implicitly create a new Branch are rejected with a clear error.

## Assumptions

- The requestor will supply a valid existing branchId; no branch creation via these APIs.
- Authentication/authorization is provided by existing MCP infrastructure; endpoints will enforce tenant/branch ownership rules.
- Prices are represented as minor units (integer) and currency is ISO-4217 code.
- Backwards compatibility: existing merchant/branch data model remains unchanged.


## Clarifications

### Session 2026-04-21
- Q: Update semantics for MerchantLocation/Product APIs → A: PATCH (partial updates) (recommended: easier for agents and avoids sending full resource)

