# Tasks: Kubernetes Helm Deployment with Secrets and Ingress

**Input**: Design documents from `/specs/005-k8s-helm-deploy/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ingress.md ✓, quickstart.md ✓

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)

## Path Conventions

Infrastructure feature. Source files live in `helm/blitzpay/` at the repository root. CD changes are in `.github/workflows/cd.yml`.

---

## Phase 1: Setup (Helm Chart Scaffold)

**Purpose**: Create the Helm chart skeleton and shared helpers. Nothing in this phase deploys anything — it establishes the directory structure and reusable template fragments.

- [ ] T001 Create `helm/blitzpay/Chart.yaml` with name `blitzpay`, chart version `0.1.0`, appVersion `0.2.2`, and a short description
- [ ] T002 [P] Create `helm/blitzpay/templates/_helpers.tpl` defining `blitzpay.fullname`, `blitzpay.labels`, and `blitzpay.selectorLabels` template helpers
- [ ] T003 [P] Create `helm/blitzpay/.helmignore` excluding `*.md`, `*.txt`, `.git`, `specs/` from chart packaging

---

## Phase 2: Foundational (Values Files — Blocking All User Stories)

**Purpose**: Define the complete configuration schema in `values.yaml` and per-environment overrides. All Helm templates reference these values, so they must be complete before any template is authored.

**⚠️ CRITICAL**: Templates in Phase 3–5 reference values defined here. Complete this phase before starting any user story.

- [ ] T004 Create `helm/blitzpay/values.yaml` with all configurable defaults: `replicaCount`, `image.repository`, `image.tag`, `image.pullPolicy`, `resources`, `secrets.name`, `config.*` (non-sensitive env vars), `ingress.enabled`, `ingress.hostname`, `ingress.tlsSecretName`, `ingress.ingressClassName`; set `image.repository: ghcr.io/medimohammadise/blitzpay`, `image.tag: latest`, `secrets.name: blitzpay-secrets`
- [ ] T005 [P] Create `helm/blitzpay/values-staging.yaml` overriding: `replicaCount: 1`, `ingress.hostname: api.staging.blitzpay.com`, `ingress.tlsSecretName: blitzpay-tls-staging`, `config.springProfilesActive: prod`, `config.truelayerEnv: sandbox`
- [ ] T006 [P] Create `helm/blitzpay/values-prod.yaml` overriding: `replicaCount: 2`, `ingress.hostname: api.blitzpay.com`, `ingress.tlsSecretName: blitzpay-tls-prod`, `config.springProfilesActive: prod`, `config.truelayerEnv: production`, `resources.requests.cpu: 500m`, `resources.requests.memory: 768Mi`

**Checkpoint**: Values schema is complete — user story template work can begin.

---

## Phase 3: User Story 1 — Deploy BlitzPay to Kubernetes via Helm (Priority: P1) 🎯 MVP

**Goal**: A platform engineer can run `helm upgrade --install` and have BlitzPay pods start successfully with liveness/readiness probes passing and the application reachable on its internal ClusterIP service.

**Independent Test**: `helm lint helm/blitzpay` passes; `helm template helm/blitzpay --values helm/blitzpay/values.yaml` renders valid YAML; after `helm upgrade --install`, `kubectl rollout status deployment/blitzpay -n <namespace>` reports success.

### Implementation for User Story 1

- [ ] T007 [US1] Create `helm/blitzpay/templates/deployment.yaml`: Deployment resource with `spec.replicas: {{ .Values.replicaCount }}`, container image `{{ .Values.image.repository }}:{{ .Values.image.tag }}`, `imagePullPolicy: {{ .Values.image.pullPolicy }}`, port 8080, resource requests/limits from values, liveness probe at `/actuator/health/liveness` port 8080 (initialDelay 60s, period 10s, failureThreshold 3), readiness probe at `/actuator/health/readiness` port 8080 (initialDelay 30s, period 5s, failureThreshold 3), rolling update strategy (`maxSurge: 1`, `maxUnavailable: 0`), standard labels from `blitzpay.labels` helper
- [ ] T008 [P] [US1] Create `helm/blitzpay/templates/service.yaml`: ClusterIP Service on port 80 targeting containerPort 8080, selector using `blitzpay.selectorLabels` helper
- [ ] T009 [P] [US1] Create `helm/blitzpay/templates/configmap.yaml`: ConfigMap named `{{ include "blitzpay.fullname" . }}-config` with keys `SPRING_PROFILES_ACTIVE: {{ .Values.config.springProfilesActive }}`, `TRUELAYER_API_BASE`, `TRUELAYER_ENV`, `TRUELAYER_HTTP_LOGS`, `TRUELAYER_WEBHOOK_JKU` all from `.Values.config.*`
- [ ] T010 [US1] Add `envFrom` to `deployment.yaml` referencing the configmap: `- configMapRef: { name: {{ include "blitzpay.fullname" . }}-config }`
- [ ] T011 [US1] Validate chart: run `helm lint helm/blitzpay` (must pass with no errors); run `helm template blitzpay helm/blitzpay --values helm/blitzpay/values.yaml` and verify rendered output contains Deployment, Service, and ConfigMap resources with correct field values
- [ ] T012 [US1] Create `helm/blitzpay/templates/NOTES.txt` with post-install output showing: namespace, how to check pod status (`kubectl get pods -n {{ .Release.Namespace }}`), how to port-forward (`kubectl port-forward svc/blitzpay 8080:80 -n {{ .Release.Namespace }}`), and reminder to pre-create `blitzpay-secrets` before deploying

**Checkpoint**: `helm lint` passes. Rendered Deployment, Service, and ConfigMap are valid. Pods start in a real cluster (without secrets yet — app may not fully boot but Helm release succeeds).

---

## Phase 4: User Story 2 — Manage Application Secrets Securely (Priority: P2)

**Goal**: All sensitive env vars (DB credentials, TrueLayer keys) are injected from a pre-created K8s Secret. No credential values appear in any Helm template, values file, or rendered manifest.

**Independent Test**: `helm template ... | grep -i password` returns nothing. Application starts and connects to PostgreSQL when the pre-created secret is present. Rotating the secret and redeploying picks up new values without changing any chart file.

### Implementation for User Story 2

- [ ] T013 [US2] Add `envFrom` secret reference to `helm/blitzpay/templates/deployment.yaml`: append `- secretRef: { name: "{{ .Values.secrets.name }}" }` to the `envFrom` list (after the configMapRef entry)
- [ ] T014 [P] [US2] Add `secrets.name` to `helm/blitzpay/values.yaml` (already defined in T004 as `blitzpay-secrets`) and add a comment block documenting all required secret keys (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `TRUELAYER_CLIENT_ID`, `TRUELAYER_CLIENT_SECRET`, `TRUELAYER_KEY_ID`, `TRUELAYER_PRIVATE_KEY_PATH`, `TRUELAYER_MERCHANT_ACCOUNT_ID`, `TRUELAYER_ACCESS_TOKEN`) and the `kubectl create secret` command template
- [ ] T015 [P] [US2] Create `helm/blitzpay/SECRET-SETUP.md` (not committed as a chart template) with step-by-step `kubectl create secret generic blitzpay-secrets --namespace <ns> --from-literal=SPRING_DATASOURCE_URL="..." ...` command for each environment; include a note on ESO migration path
- [ ] T016 [US2] Validate secret injection: run `helm template blitzpay helm/blitzpay --values helm/blitzpay/values.yaml` and verify rendered Deployment contains `envFrom` with both `configMapRef` and `secretRef` entries; run `helm template ... | grep -i 'password\|secret\|token\|key'` and confirm no literal values appear (only key names)
- [ ] T017 [US2] Run `helm lint helm/blitzpay` again to confirm chart still passes after secret changes

**Checkpoint**: Rendered manifest has `envFrom.secretRef` referencing `blitzpay-secrets`. No credential values in any file. Application starts when the secret exists in the cluster.

---

## Phase 5: User Story 3 — Expose API Endpoints and Swagger UI via Ingress (Priority: P3)

**Goal**: External HTTPS traffic reaches the BlitzPay REST API and Swagger UI through a configured Ingress. The CD pipeline automatically runs `helm upgrade --install` after each GitHub release.

**Independent Test**: After `helm upgrade --install`, `kubectl get ingress -n <namespace>` shows the ingress with the correct hostname. `curl -k https://<hostname>/actuator/health/readiness` returns 200. `curl -k https://<hostname>/swagger-ui` returns HTML. `curl -k https://<hostname>/api-docs` returns JSON.

### Implementation for User Story 3

- [ ] T018 [US3] Create `helm/blitzpay/templates/ingress.yaml`: Ingress resource gated by `{{ if .Values.ingress.enabled }}`, `ingressClassName: {{ .Values.ingress.ingressClassName }}`, TLS block with host `{{ .Values.ingress.hostname }}` and secretName `{{ .Values.ingress.tlsSecretName }}`, three `pathType: Prefix` rules in this order: path `/api-docs` → service port 80, path `/swagger-ui` → service port 80, path `/` → service port 80; annotations include `nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"` and `cert-manager.io/cluster-issuer: letsencrypt-prod` (configurable via `ingress.annotations` in values)
- [ ] T019 [P] [US3] Add `ingress.annotations` map to `helm/blitzpay/values.yaml` (default to cert-manager annotation) and add `ingress.ingressClassName: nginx` default; render annotations block in `ingress.yaml` using `{{- with .Values.ingress.annotations }}{{ toYaml . | nindent 4 }}{{- end }}`
- [ ] T020 [US3] Update `.github/workflows/cd.yml`: add a `deploy` job that runs after the `release` job; steps: `actions/checkout@v4`, `azure/setup-helm@v3`, configure kubectl from `secrets.KUBECONFIG` (base64 decode to `$HOME/.kube/config`), run `helm upgrade --install blitzpay ./helm/blitzpay --namespace blitzpay-prod --create-namespace --values ./helm/blitzpay/values.yaml --values ./helm/blitzpay/values-prod.yaml --set image.tag=${{ github.ref_name }} --wait --timeout 5m`, then `kubectl rollout status deployment/blitzpay -n blitzpay-prod --timeout=5m`
- [ ] T021 [US3] Validate ingress template: run `helm template blitzpay helm/blitzpay --values helm/blitzpay/values.yaml --set ingress.enabled=true` and verify rendered Ingress has all three path rules (`/api-docs`, `/swagger-ui`, `/`), TLS block, and correct backend service name
- [ ] T022 [US3] Run final `helm lint helm/blitzpay` across all values files: `helm lint helm/blitzpay -f helm/blitzpay/values.yaml -f helm/blitzpay/values-staging.yaml` and `helm lint helm/blitzpay -f helm/blitzpay/values.yaml -f helm/blitzpay/values-prod.yaml`

**Checkpoint**: Full chart lints clean for staging and production value sets. Ingress template renders with correct paths. CD pipeline has a working `deploy` job definition.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, documentation, and deprecation of the legacy raw manifests.

- [ ] T023 [P] Add deprecation notice to top of `k8s/deployment.yaml`: comment block stating "DEPRECATED: This file is superseded by the Helm chart at helm/blitzpay/. Do not apply this file directly. See specs/005-k8s-helm-deploy/quickstart.md for deployment instructions."
- [ ] T024 [P] Add `helm/blitzpay/README.md` (chart-level, not a project README) documenting: prerequisites, `helm upgrade --install` commands for staging and production, required GitHub Actions secret (`KUBECONFIG`), secret creation command, and link to `specs/005-k8s-helm-deploy/quickstart.md`
- [ ] T025 Run `helm template blitzpay helm/blitzpay -f helm/blitzpay/values.yaml -f helm/blitzpay/values-prod.yaml --set image.tag=v0.2.2` and review full rendered output end-to-end; confirm: Deployment has both `configMapRef` and `secretRef` in `envFrom`, rolling update strategy is set, Ingress has all three path rules, Service targets port 8080, labels are consistent across all resources
- [ ] T026 [P] Verify `quickstart.md` steps are accurate against the implemented chart: check all `kubectl` and `helm` commands reference actual resource names and namespaces as rendered by the chart

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 (Chart.yaml and helpers must exist before values are authored)
- **Phase 3 (US1)**: Depends on Phase 2 completion — values schema must be final before templates reference them
- **Phase 4 (US2)**: Depends on T007 (deployment.yaml must exist before adding `envFrom.secretRef`)
- **Phase 5 (US3)**: Depends on Phase 3 (Service must exist for ingress backend reference); T020 (cd.yml) can run in parallel with T018/T019
- **Phase 6 (Polish)**: Depends on all user story phases complete

### User Story Dependencies

- **US1 (P1)**: Depends only on Phase 1–2 — no cross-story dependencies
- **US2 (P2)**: Depends on T007 (deployment.yaml created in US1) — must be done after T007 completes
- **US3 (P3)**: Depends on T008 (service.yaml) for ingress backend — can start once T008 is done; T020 (cd.yml) is independent

### Within Each User Story

- Values files (Phase 2) before templates (Phase 3+)
- Service (T008) and ConfigMap (T009, T010) can be authored in parallel with Deployment (T007)
- Secret injection (T013) must follow Deployment creation (T007)
- Ingress (T018, T019) requires Service (T008) to exist for backend reference
- Lint validation tasks always run last within each phase

### Parallel Opportunities

- T002 and T003 (Phase 1) — parallel, different files
- T005 and T006 (Phase 2) — parallel, different files
- T008, T009 (Phase 3) — parallel with each other (different files)
- T014, T015 (Phase 4) — parallel with each other (different files)
- T019, T020 (Phase 5) — partially parallel (T019 modifies values.yaml, T020 modifies cd.yml — different files)
- T023, T024, T026 (Phase 6) — parallel, different files

---

## Parallel Example: User Story 1

```bash
# After T007 is complete, these can run in parallel:
Task T008: "Create helm/blitzpay/templates/service.yaml"
Task T009: "Create helm/blitzpay/templates/configmap.yaml"

# After T009 is complete:
Task T010: "Add envFrom configMapRef to deployment.yaml"

# After T008, T009, T010 are complete:
Task T011: "helm lint and helm template validation"
Task T012: "Create NOTES.txt"  ← parallel with T011 (different file)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Helm chart scaffold (T001–T003)
2. Complete Phase 2: Values files (T004–T006)
3. Complete Phase 3: Core Helm chart templates (T007–T012)
4. **STOP and VALIDATE**: Run `helm lint` and `helm template`; deploy to a test cluster and verify pods start
5. MVP is a working Helm chart — deployable even without secrets or ingress

### Incremental Delivery

1. Phase 1–3 → Working Helm chart with ConfigMap (MVP)
2. Add Phase 4 (US2) → Secure secret injection; no credential leakage
3. Add Phase 5 (US3) → Ingress + CD automation
4. Phase 6 → Polish and deprecate old manifests

### Single-Developer Strategy

1. T001 → T002, T003 (parallel) → T004 → T005, T006 (parallel)
2. T007 → T008, T009 (parallel) → T010 → T011, T012 (parallel)
3. T013 → T014, T015 (parallel) → T016 → T017
4. T018 → T019, T020 (parallel) → T021 → T022
5. T023, T024, T025, T026 (T023, T024, T026 parallel; T025 last)

---

## Notes

- [P] tasks = different files, no shared dependencies — safe to run concurrently
- `helm lint` and `helm template` are the primary validation tools — no cluster needed
- `secrets.name` is always a name reference, never a value — confirm with `grep` after each render
- The `k8s/` directory is NOT modified (only annotated as deprecated in T023)
- `TRUELAYER_PRIVATE_KEY_PATH` refers to a file path inside the container — key file mounting via a separate Secret volume may be a follow-up task if the PEM file is not baked into the image
- Commit after each checkpoint (end of each Phase) for clean rollback history
