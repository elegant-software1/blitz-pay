# Test Data Fixture Catalog

This directory contains reusable business scenarios for automated tests.

## Structure

- Store canonical scenarios under a domain directory such as `invoice/`
- Use file names that describe the business scenario, for example `canonical-invoice.json`
- Keep reusable business input and reusable expectations in the same fixture document

## Naming Rules

- Prefer `domain/scenario-name.json`
- Use one canonical fixture for the shared happy path before creating extra files
- Express small differences as loader-backed variants instead of copying the whole fixture

## Variant Rules

- Add a new fixture file only when the business scenario is materially different
- Keep one-off literals inline only when reuse would make the test harder to understand
- Name loader helper methods after the business difference they introduce

## Current Catalog

- `invoice/canonical-invoice.json`: canonical invoice scenario shared by controller and service tests
## Price Comparison Fixtures

- `pricecomparison/better-price.json`: Canonical scenario where a qualifying cheaper offer exists
- `pricecomparison/no-better-price.json`: Canonical scenario where no cheaper qualifying offer exists
- `pricecomparison/comparison-unavailable.json`: Canonical scenario where matching or comparable pricing is unavailable
- `pricecomparison/monitoring-slowdown.json`: Scenario where the comparison succeeds but records an offer-lookup bottleneck and warning
- `pricecomparison/monitoring-failure.json`: Scenario where offer lookup fails and the response includes failure monitoring details

## Market Search Fixtures

- `marketsearch/brave-discovery-sony.json`: Canonical Brave discovery payload for a Sony product query
- `marketsearch/deepsearch-discovery-sony.json`: Canonical DeepSearch discovery payload normalized into the same Sony retailer set
- `marketsearch/deepsearch-discovery-empty.json`: DeepSearch discovery payload with no candidate retailer hits
- `marketsearch/deepsearch-provider-failure.json`: Canonical DeepSearch upstream failure payload for adapter and monitoring tests
- `marketsearch/retailer-sony-techhub.html`: Retailer page with JSON-LD `Product` and `Offer` data
- `marketsearch/retailer-empty.html`: Retailer page with no extractable structured offer data
- `marketsearch/provider-failure.json`: Canonical upstream provider failure payload for adapter tests

Monitoring expectations:

- Every structured result must include a `monitoring` object
- Success cases should finish with `monitoring.stage = completed` and `monitoring.progress = 100`
- Slow-path scenarios may include `monitoring.warnings` and `monitoring.bottleneck`
- Failure scenarios should include `monitoring.failure` without requiring a separate lookup endpoint

These fixtures mirror the repository policy in `CONSTITUATION.md`: shared business scenarios live in `src/test/resources/testdata/` and tests should derive small, explicit variants rather than duplicating full request/response samples inline.
