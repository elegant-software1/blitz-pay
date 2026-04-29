# Feature Specification: Merchant Product Categories

**Feature Branch**: `012-product-categories`  
**Created**: 2026-04-28  
**Status**: Draft  
**Input**: User description: "I want merchant to be able to create product category, each product can be assigned to a product category and product category is specific for merchant like drinks, softdrink, vegis, wein etc, product category should be supported in product related APIs and mcp server"

## Clarifications

### Session 2026-04-29

- Q: What uniqueness scope should `productCode` use? → A: Unique per branch
- Q: What should happen if a provided `productCode` already exists in the same branch? → A: Update the existing product instead
- Q: How should auto-generated `productCode` values be assigned? → A: Next highest numeric value per branch, with no gap reuse

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Merchant Creates and Manages Product Categories (Priority: P1)

A merchant wants to organise their product catalogue by grouping items into meaningful categories (e.g., "Drinks", "Soft Drinks", "Vegetables", "Wine"). The merchant can create, rename, and delete categories specific to their store. No other merchant sees or can access these categories.

**Why this priority**: Categories are the foundation of this feature. Without the ability to create them, assigning products to them is impossible. This story alone delivers immediate organisational value.

**Independent Test**: Create a merchant account, add three categories ("Drinks", "Wine", "Vegetables"), and verify that the category list returns exactly those three entries scoped to that merchant.

**Acceptance Scenarios**:

1. **Given** a logged-in merchant, **When** they create a category with a unique name, **Then** the category is saved and appears in their category list.
2. **Given** a logged-in merchant, **When** they attempt to create a category with a name that already exists for their store, **Then** the system rejects the request with a clear duplicate-name error.
3. **Given** two different merchants each with a category named "Drinks", **When** either merchant lists their categories, **Then** they see only their own categories and not the other merchant's.
4. **Given** a logged-in merchant, **When** they rename an existing category, **Then** the updated name is reflected immediately and all products assigned to that category still belong to it.
5. **Given** a logged-in merchant, **When** they delete a category that has no products assigned, **Then** the category is removed successfully.
6. **Given** a logged-in merchant, **When** they delete a category that still has products assigned, **Then** the system either blocks deletion (returning a clear error) or reassigns products to "uncategorised" — defaulting to blocking with an error message.

---

### User Story 2 - Merchant Assigns a Category to a Product (Priority: P2)

When creating or updating a product, the merchant can optionally select one of their own categories to group the product under. A product belongs to at most one category at a time. A product may also be uncategorised.

**Why this priority**: Assigning categories to products is the core value of this feature, enabling filtered browsing and catalogue organisation. Depends on P1 categories existing first.

**Independent Test**: Create one category and one product, assign the product to the category, then retrieve the product and verify the category is present in the response.

**Acceptance Scenarios**:

1. **Given** a merchant with at least one category and one product, **When** they assign the product to a category, **Then** the product record reflects the assigned category.
2. **Given** a merchant viewing their product list, **When** they filter by category, **Then** only products belonging to that category are returned.
3. **Given** a product already assigned to category A, **When** the merchant reassigns it to category B, **Then** the product now belongs to category B and no longer to category A.
4. **Given** a product assigned to a category, **When** the merchant removes the category assignment, **Then** the product becomes uncategorised and the category is unaffected.
5. **Given** a merchant, **When** they attempt to assign a product to a category that belongs to a different merchant, **Then** the system rejects the request with an authorisation error.

---

### User Story 3 - Category Data Exposed via MCP Server Tool (Priority: P3)

The MCP server exposes tools that allow AI agents (or automated workflows) to look up product categories, create categories, and update product assignments by name — mirroring what the REST API provides.

**Why this priority**: MCP server support extends the feature to agentic/AI workflows. The REST API must be stable first (P1 + P2), making this a natural third step.

**Independent Test**: Using the MCP tools, call "create product category", then "assign product to category", then "list products by category" and verify all three succeed end-to-end.

**Acceptance Scenarios**:

1. **Given** an MCP client connected as a merchant, **When** the client calls the "get or create category by name" tool, **Then** the category is returned (existing or newly created).
2. **Given** an MCP client, **When** the client calls "assign product to category" with valid product and category names, **Then** the assignment is persisted and confirmed.
3. **Given** an MCP client, **When** the client calls "list categories for merchant", **Then** all categories for that merchant are returned.
4. **Given** an MCP client, **When** the client provides an invalid merchant or category reference, **Then** the tool returns a structured error rather than crashing.

---

### Edge Cases

- What happens when a merchant tries to create more than a reasonable number of categories (e.g., thousands)? — System applies a soft limit (e.g., 500 categories per merchant) and returns a clear limit-exceeded error.
- How does the system handle concurrent category deletion and product assignment to the same category? — The system treats deletion as a guarded operation; if products are still assigned, deletion is rejected.
- What happens when a category name contains special characters or is excessively long? — The system enforces a maximum name length (e.g., 100 characters) and trims leading/trailing whitespace; special characters are allowed.
- What happens when a product is retrieved but its assigned category has since been deleted? — The product is returned with an "uncategorised" state; no error is thrown.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a merchant to create a product category with a unique (per-merchant) name.
- **FR-002**: System MUST scope product categories to the owning merchant; no cross-merchant visibility or assignment is permitted.
- **FR-003**: System MUST allow a merchant to list all their product categories.
- **FR-004**: System MUST allow a merchant to rename an existing product category.
- **FR-005**: System MUST allow a merchant to delete a product category that has no products currently assigned to it.
- **FR-006**: System MUST reject deletion of a product category that still has products assigned, returning a descriptive error.
- **FR-007**: System MUST allow a merchant to assign any of their products to one of their own categories.
- **FR-008**: System MUST allow a merchant to reassign a product from one category to another in a single operation.
- **FR-009**: System MUST allow a merchant to remove the category assignment from a product, leaving it uncategorised.
- **FR-010**: System MUST allow filtering the merchant's product list by category.
- **FR-011**: System MUST expose category management (create, list) and product-category assignment via the MCP server tools.
- **FR-012**: System MUST enforce a maximum category name length of 100 characters.
- **FR-013**: Product list responses MUST include the assigned category name (and identifier) when a category is set.
- **FR-014**: System MUST support an optional `productCode` on product create and update requests in both REST API and MCP tool flows.
- **FR-015**: When an application provides a `productCode`, the system MUST persist that exact value if it is unique within the product's branch.
- **FR-016**: When no `productCode` is provided, the system MUST auto-generate a numeric product code for the product.
- **FR-016**: When no `productCode` is provided, the system MUST auto-generate the next highest numeric product code within the product's branch.
- **FR-017**: Product codes MUST be unique within a branch.
- **FR-018**: When a create or update request provides a `productCode` that already exists in the same branch, the system MUST treat the request as targeting the existing product with that code instead of creating a second product with the same code.
- **FR-019**: Product responses MUST include the effective `productCode`.
- **FR-020**: Auto-generated product codes MUST be monotonic within a branch and MUST NOT reuse gaps left by deleted or reassigned products.

### Key Entities

- **Product Category**: A named grouping owned by a single merchant. Key attributes: unique identifier, merchant owner, name (unique per merchant), created/updated timestamps.
- **Product**: An existing entity extended with an optional reference to one Product Category and a branch-scoped unique `productCode`. A product may be uncategorised. If no code is supplied by the caller, the system assigns the next auto-generated numeric code for that branch.
- **Product**: An existing entity extended with an optional reference to one Product Category and a branch-scoped unique `productCode`. A product may be uncategorised. If no code is supplied by the caller, the system assigns the next highest auto-generated numeric code for that branch without reusing gaps. If a supplied code already exists in that branch, the request targets that existing product record.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A merchant can create, rename, and delete a category in under 30 seconds total using the standard product management interface.
- **SC-002**: Product list filtering by category returns results within the same response-time envelope as unfiltered product lists (no measurable degradation).
- **SC-003**: 100% of product responses include accurate category information (name and identifier, or explicit null when uncategorised).
- **SC-004**: MCP tool calls for category operations complete successfully in all defined acceptance scenarios with no unhandled errors.
- **SC-005**: Cross-merchant category isolation is enforced — 0 incidents of one merchant's categories or assignments being visible to or modifiable by another merchant.

## Assumptions

- Each product belongs to at most one category at a time (single-category assignment, not multi-tag).
- Category names are case-insensitive for uniqueness checks within a merchant (e.g., "Drinks" and "drinks" are treated as the same name).
- A merchant's existing product-related REST endpoints and MCP tools will be extended rather than replaced.
- Product code uniqueness is enforced per branch, not globally or per merchant.
- Auto-generated product codes are numeric, branch-scoped, monotonic, and gap-preserving.
- The MCP server already has a concept of the acting merchant identity; category operations will use the same identity resolution mechanism.
- Category ordering in list responses defaults to alphabetical by name; no custom ordering is required for the initial version.
- Soft-delete or archiving of categories is out of scope for this version; deletion is permanent.
- Bulk category assignment (assigning many products at once) is out of scope for this version.
