# Implementation Plan: Kubernetes Helm Deployment with Secrets and Ingress

**Branch**: `005-k8s-helm-deploy` | **Date**: 2026-03-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-k8s-helm-deploy/spec.md`

## Summary

Package the BlitzPay Spring Boot application into a Helm chart that deploys to Kubernetes with secure secret injection (native K8s Secrets, pre-created outside Helm), an Nginx Ingress exposing the REST API and Swagger UI over HTTPS, and a new CD pipeline deploy step. The chart is designed to swap in External Secrets Operator for AWS Secrets Manager in a future phase without changing application code or Helm templates.

## Technical Context

**Language/Version**: Kotlin / JVM 21 (Spring Boot, Gradle)
**Primary Dependencies**: Helm 3, Kubernetes 1.28+, Nginx Ingress Controller, cert-manager (TLS)
**Storage**: PostgreSQL (external to cluster; connection via secret-injected env vars)
**Testing**: Helm template linting (`helm lint`, `helm template`), `kubectl rollout status`
**Target Platform**: Kubernetes cluster (cloud-agnostic; EKS assumed for future AWS secrets migration)
**Project Type**: Web service deployment pipeline
**Performance Goals**: Rolling deploy completes within 5 minutes; zero downtime during upgrade
**Constraints**: Secrets MUST NOT appear in any version-controlled file; chart must support ≥2 environments (staging, prod)
**Scale/Scope**: Single microservice; staging = 1 replica, production = 2+ replicas

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The project constitution is a template with placeholder content — no enforced principles are defined. No gates apply. Constitution check passes by default.

**Post-design re-check**: No violations introduced. The Helm chart is a single-purpose deployment artifact with no unnecessary complexity.

## Project Structure

### Documentation (this feature)

```text
specs/005-k8s-helm-deploy/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── ingress.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
helm/
└── blitzpay/                      # Helm chart root
    ├── Chart.yaml                 # Chart metadata (name, version, appVersion)
    ├── values.yaml                # Default values (dev-friendly defaults)
    ├── values-staging.yaml        # Staging environment overrides
    ├── values-prod.yaml           # Production environment overrides
    └── templates/
        ├── _helpers.tpl           # Label helpers and name helpers
        ├── deployment.yaml        # Pod spec: image, probes, secret refs, resources
        ├── service.yaml           # ClusterIP service on port 8080
        ├── ingress.yaml           # Nginx Ingress: /api, /api-docs, /swagger-ui paths
        ├── configmap.yaml         # Non-sensitive config (SPRING_PROFILES_ACTIVE, TRUELAYER_ENV, etc.)
        └── NOTES.txt              # Post-install instructions

k8s/                               # DEPRECATED — superseded by Helm chart
└── deployment.yaml                # Raw manifests; kept for reference, not applied

.github/workflows/
└── cd.yml                         # UPDATED: adds a deploy job after image build
```

**Structure Decision**: Single Helm chart under `helm/blitzpay/` following standard Helm chart layout. The existing `k8s/deployment.yaml` raw manifests are deprecated but kept for reference. No monorepo or multi-chart structure needed for a single microservice.

## Complexity Tracking

> No constitution violations. Table not required.
