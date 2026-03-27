# Contract: BlitzPay Ingress Routing

**Branch**: `005-k8s-helm-deploy` | **Date**: 2026-03-27

This contract defines the external interface exposed by the BlitzPay Kubernetes Ingress. It specifies the URL paths, routing rules, and TLS behaviour that consumers (developers, API clients, CI/CD verification steps) can rely on.

---

## Host

| Environment | Hostname (placeholder) |
|---|---|
| Staging | `api.staging.blitzpay.com` |
| Production | `api.blitzpay.com` |

> Actual hostnames are configured in `values-staging.yaml` and `values-prod.yaml`. DNS must be configured externally before the ingress is reachable.

---

## TLS

All external traffic MUST use HTTPS. HTTP connections are redirected to HTTPS by the ingress controller.

| Environment | TLS Secret Name |
|---|---|
| Staging | `blitzpay-tls-staging` |
| Production | `blitzpay-tls-prod` |

Certificates are managed by cert-manager (or provided manually). The Helm chart references the secret by name.

---

## Routes

All paths use `pathType: Prefix` and route to the BlitzPay service on port 8080.

| Path Prefix | Destination | Description |
|---|---|---|
| `/api-docs` | BlitzPay :8080/api-docs | OpenAPI JSON schema (SpringDoc) |
| `/swagger-ui` | BlitzPay :8080/swagger-ui | Interactive Swagger UI |
| `/` | BlitzPay :8080 | All REST API endpoints (catch-all) |

### Notes

- `/api-docs` is the custom SpringDoc path configured in `application.yml` (`springdoc.api-docs.path: /api-docs`). This is NOT the SpringDoc default `/v3/api-docs`.
- `/swagger-ui` loads the interactive browser UI. The Swagger UI fetches its schema from `/api-docs`.
- The catch-all `/` rule routes all other paths (e.g., `/payments`, `/invoices`, `/actuator/health`) to the application.
- Ingress path ordering: more specific paths (`/api-docs`, `/swagger-ui`) are listed before the catch-all. The ingress controller matches longest prefix first.

---

## Ingress Class

| Setting | Value |
|---|---|
| `ingressClassName` | `nginx` |

An Nginx Ingress Controller must be installed in the cluster. The class name is configurable via `ingress.ingressClassName` in the values file.

---

## Health Check Endpoints (Internal, not Ingress-routed)

These actuator endpoints are used by Kubernetes probes and are NOT intended for external access through the ingress. They are accessible on the cluster-internal service only.

| Path | Probe Type | Port |
|---|---|---|
| `/actuator/health/liveness` | Liveness | 8080 |
| `/actuator/health/readiness` | Readiness | 8080 |

> If the ingress catch-all `/` is active, these paths will also be reachable externally. Restrict access via `nginx.ingress.kubernetes.io/server-snippet` if the actuator must not be public.

---

## Error Behaviour

| Condition | Ingress Response |
|---|---|
| Path not matched | 404 from ingress controller |
| Backend pod not ready | 503 (ingress holds traffic until pod is ready) |
| TLS certificate invalid | Browser/client TLS handshake error |
| Hostname not resolvable | DNS resolution failure (client-side) |

---

## Example Requests (Staging)

```bash
# REST API call
curl https://api.staging.blitzpay.com/payments

# OpenAPI schema
curl https://api.staging.blitzpay.com/api-docs

# Swagger UI (browser)
open https://api.staging.blitzpay.com/swagger-ui
```
