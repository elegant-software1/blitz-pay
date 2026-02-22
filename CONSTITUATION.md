# BlitzPay Constitution

## Core Principles

### I. Focused Changes
Changes must stay narrowly scoped to the requested outcome. Contributors should prefer revising the existing design over introducing parallel patterns or unrelated refactors.

### II. Fixture-First Reuse
Reusable business test data, including both canonical inputs and expected outputs, must live in shared fixtures under `src/test/resources/testdata/` instead of being hard-coded across multiple tests. Canonical scenarios should include representative input plus reusable expectations so tests can assert against the fixture instead of guessing values.

### III. Readable Variants
Small differences from a canonical test scenario should be expressed as explicit variants in test support code or narrowly scoped helpers. Inline literals are allowed only when the value is clearly one-off and reuse would make the test harder to understand.

### IV. Test and Review Discipline
Behavior changes require corresponding automated test updates. Reviewers should reject duplicated reusable business data, opaque fixture naming, and fixture files that differ only by trivial changes.

### V. Documentation as Policy
Repository standards that affect contribution or review are part of repository governance and must be recorded in this constitution. Code comments and feature specs may support the rule but do not replace it.

## Quality Standards

- Fixture files must use non-sensitive representative data only.
- Fixture names must communicate domain and scenario intent.
- The same business scenario should be updated in one canonical fixture rather than copied across multiple tests.
- Changes that affect modular boundaries or exposed APIs must remain consistent with `.specify/references/architecture_guidelines.md`.

## Build Configuration Standards

### Dependency Grouping in Gradle Files

All dependencies in `build.gradle.kts` and `gradle/libs.versions.toml` must be organized with clear comment headers that indicate:

1. **Module/Domain boundary** - Which modulith module or shared component the dependency serves
2. **Purpose** - Brief description of what the dependency provides
3. **Platform vs. Third-party** - Distinguish managed dependencies from explicit versioned ones

Example structure in `build.gradle.kts`:
```kotlin
// ----------------------------
// Modulith: `invoice`
// Purpose: ZUGFeRD/Factur-X invoice XML + Thymeleaf->PDF rendering
// ----------------------------
implementation(libs.mustang.library)
implementation(libs.spring.boot.starter.thymeleaf)
```

### Version Catalog Organization

The `gradle/libs.versions.toml` file must follow this structure:

1. **[Plugins]** section - Build and development tool plugins
2. **[Versions]** section - Explicit version definitions (only for non-managed dependencies)
3. **[Libraries]** section - Organized by module/domain with comment headers matching `build.gradle.kts`

### BOM (Bill of Materials) Usage

When a dependency is managed by a platform BOM (e.g., Spring Boot, Spring Modulith):

1. **Do NOT declare versions** in `libs.versions.toml` for BOM-managed dependencies
2. **Add a comment** indicating the BOM manages the version
3. **Use version catalog references** in `build.gradle.kts` instead of hardcoded strings
4. **Group BOM-managed dependencies** separately from third-party libraries requiring explicit versions

Example for Spring Boot BOM:
```toml
# [Libraries] section in libs.versions.toml
# -----------------------------------------------------------------------------
# Shared platform (all Modulith modules)
# Purpose: web/http, JSON, Modulith runtime, OpenAPI documentation
# NOTE: Spring Boot BOM manages versions for spring-boot-starter-* dependencies
# -----------------------------------------------------------------------------
# Spring Boot starters (version managed by Spring Boot BOM)
spring-boot-starter-webflux = { module = "org.springframework.boot:spring-boot-starter-webflux" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }

# Third-party libraries (explicit versions required)
springdoc-openapi-starter-webflux-ui = { module = "org.springdoc:springdoc-openapi-starter-webflux-ui", version.ref = "springdoc" }
```

This standard ensures:
- Clear ownership and purpose for each dependency
- Easier dependency version upgrades via BOM leverage
- Consistent organization across the codebase
- Reduced maintenance burden when platform versions change

## Review Process

- Compliant example: two invoice tests reuse the same canonical fixture and derive a small bank-account variant through shared support code.
- Non-compliant example: multiple tests repeat the same customer names, invoice number, and line-item text inline with no justification.
- Acceptable exception: a single assertion keeps a tiny literal inline because converting it into a shared fixture would reduce readability.

## Governance

This constitution supersedes local conventions for fixture reuse and review standards. Amendments must update this file and keep `AGENTS.md` aligned with contributor-facing policy. Reviewers and implementers are expected to verify compliance during normal code review.

**Version**: 1.1.0 | **Ratified**: 2026-03-25 | **Last Amended**: 2026-03-25
