# Data Model: Kubernetes Helm Deployment for BlitzPay

**Branch**: `005-k8s-helm-deploy` | **Date**: 2026-03-27

This feature is a deployment infrastructure feature, not a data feature. The "entities" here are the deployment configuration artifacts and their relationships.

---

## Entities

### Helm Chart (`helm/blitzpay/`)

The top-level deployment package. Contains all Kubernetes resource templates and configuration defaults.

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | yes | Chart name: `blitzpay` |
| `version` | semver | yes | Chart version (independent of app version) |
| `appVersion` | string | yes | Application version (e.g., `0.2.2`) |
| `description` | string | yes | Human-readable chart description |

**Relationships**: Contains → EnvironmentValues (1..n), HelmTemplate (1..n)

---

### EnvironmentValues

A per-environment configuration file that overrides chart defaults. Three instances: base, staging, production.

| Field | Type | Required | Description |
|---|---|---|---|
| `replicaCount` | integer | yes | Number of pod replicas |
| `image.repository` | string | yes | Container image repository URL |
| `image.tag` | string | yes | Container image tag (overridden by CD at deploy time) |
| `image.pullPolicy` | string | yes | `IfNotPresent` (dev) or `Always` (prod) |
| `ingress.enabled` | boolean | yes | Enable/disable ingress resource |
| `ingress.hostname` | string | yes | External hostname for the ingress rule |
| `ingress.tlsSecretName` | string | yes | Name of the TLS secret in the cluster |
| `ingress.ingressClassName` | string | yes | Ingress class (e.g., `nginx`) |
| `secrets.name` | string | yes | Name of the pre-created K8s Secret resource |
| `config.springProfilesActive` | string | yes | Spring profile (`prod`, `staging`) |
| `config.truelayerEnv` | string | yes | TrueLayer environment (`production`, `sandbox`) |
| `config.truelayerApiBase` | string | yes | TrueLayer API base URL |
| `config.truelayerWebhookJku` | string | yes | TrueLayer webhook JKU URL |
| `config.truelayerHttpLogs` | boolean | no | Enable TrueLayer HTTP request logging |
| `resources.requests.cpu` | string | yes | CPU request (e.g., `250m`) |
| `resources.requests.memory` | string | yes | Memory request (e.g., `512Mi`) |
| `resources.limits.cpu` | string | yes | CPU limit (e.g., `1000m`) |
| `resources.limits.memory` | string | yes | Memory limit (e.g., `1Gi`) |

**State transitions**: Values files are static; the `image.tag` field is overridden at deploy time via `--set image.tag=<tag>`.

---

### KubernetesSecret (`blitzpay-secrets`)

A pre-created cluster resource holding all sensitive configuration. Managed outside Helm by a platform engineer. NOT a Helm-managed resource.

| Secret Key | Description | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | Include DB hostname, port, DB name |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | |
| `TRUELAYER_CLIENT_ID` | TrueLayer OAuth2 client ID | |
| `TRUELAYER_CLIENT_SECRET` | TrueLayer OAuth2 client secret | |
| `TRUELAYER_KEY_ID` | TrueLayer EC key ID for signing | |
| `TRUELAYER_PRIVATE_KEY_PATH` | Path to EC private key (PEM) | Key file must also be mounted or baked |
| `TRUELAYER_MERCHANT_ACCOUNT_ID` | TrueLayer merchant account UUID | |
| `TRUELAYER_ACCESS_TOKEN` | TrueLayer bearer access token | |

**Validation rules**:
- All keys MUST be present; application fails to start if any required key is missing.
- Values MUST NOT appear in version-controlled files.
- One Secret resource per environment namespace.

**Relationships**: Referenced by → Deployment (via `envFrom.secretRef`) | Future: Synchronized from → ExternalSecret (ESO)

---

### IngressResource

The Kubernetes Ingress object created by the Helm chart to route external traffic.

| Field | Type | Description |
|---|---|---|
| `hostname` | string | External DNS hostname (e.g., `api.blitzpay.com`) |
| `tlsSecretName` | string | Name of TLS secret containing cert+key |
| `paths[0].path` | `/api-docs` | Routes to OpenAPI schema endpoint |
| `paths[1].path` | `/swagger-ui` | Routes to Swagger UI |
| `paths[2].path` | `/` | Catch-all for all REST API endpoints |
| `paths[*].pathType` | `Prefix` | All paths use Prefix matching |
| `paths[*].backend.port` | `8080` | Application container port |

**Relationships**: Routes to → Service | Terminates TLS using → KubernetesSecret (TLS cert)

---

### ConfigMap (`blitzpay-config`)

A Helm-managed resource holding non-sensitive application configuration injected as environment variables.

| Key | Description | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `TRUELAYER_API_BASE` | TrueLayer API base URL | `https://api.truelayer.com` |
| `TRUELAYER_ENV` | TrueLayer environment | `production` |
| `TRUELAYER_HTTP_LOGS` | Enable HTTP request logging | `false` |
| `TRUELAYER_WEBHOOK_JKU` | Webhook JKU URL | `https://api.blitzpay.com/.well-known/jwks.json` |

**Relationships**: Injected into → Deployment (via `envFrom.configMapRef`)

---

## Entity Relationships

```
EnvironmentValues
    └─ references ──► KubernetesSecret (by name, pre-created outside Helm)
    └─ configures ──► Deployment
                          └─ envFrom ──► ConfigMap (Helm-managed)
                          └─ envFrom ──► KubernetesSecret (pre-created)
                          └─ serves  ──► Service (ClusterIP :8080)
                                            └─ routed-by ──► IngressResource
                                                                 └─ TLS ──► KubernetesSecret (TLS cert)
```

---

## Future State: External Secrets Operator

When migrating to AWS Secrets Manager, a new entity type is introduced:

### ExternalSecret (ESO Resource)
Maps AWS Secrets Manager entries to a K8s Secret. The Helm chart does NOT manage this resource; it is applied separately.

| Field | Description |
|---|---|
| `spec.secretStoreRef.name` | Name of the `SecretStore` pointing to AWS |
| `spec.target.name` | Name of the K8s Secret to create (e.g., `blitzpay-secrets`) |
| `spec.data[*].remoteRef.key` | AWS secret name |
| `spec.data[*].remoteRef.property` | JSON property in the AWS secret |

**Impact on Helm chart**: Zero. The K8s Secret name (`blitzpay-secrets`) remains the same; the chart's `secretRef` reference is unchanged.
