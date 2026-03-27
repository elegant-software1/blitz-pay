# Feature Specification: Kubernetes Helm Deployment with Secrets and Ingress

**Feature Branch**: `005-k8s-helm-deploy`
**Created**: 2026-03-27
**Status**: Draft
**Input**: User description: "I want to be able to deploy blitzpay docker image in K8s, It is already creating image in cd.yml file, I want to use helm chart and there are also some secrets I want to make use of k8s secrets, later on I may move this secrets to AWS, at the end I want my back-end endpoints along with swagger to be exposed via ingress"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Deploy BlitzPay to Kubernetes via Helm (Priority: P1)

A platform engineer needs to deploy the BlitzPay application to a Kubernetes cluster by running a Helm install or upgrade command (or having the CD pipeline do it), without manually authoring deployment manifests. The existing CD pipeline already builds and pushes the Docker image; the engineer wants the deployment step to pick up that image and roll it out reliably.

**Why this priority**: Without a working deployment, none of the other capabilities (secret management, external access) can be validated. This is the foundational requirement.

**Independent Test**: Can be tested by running a Helm install/upgrade command and verifying that BlitzPay pods reach a running state in the cluster and the application responds on its internal service address.

**Acceptance Scenarios**:

1. **Given** a Kubernetes cluster is available and the BlitzPay Docker image exists in the container registry, **When** the Helm chart is installed or upgraded with the correct image tag, **Then** the application pods start successfully and pass readiness checks within 5 minutes.
2. **Given** a new Docker image tag is produced by the CD pipeline, **When** the Helm chart is upgraded with the new image tag, **Then** running pods are replaced with zero downtime via a rolling update.
3. **Given** the Helm chart is deployed, **When** a pod crashes or is evicted, **Then** Kubernetes automatically restarts it without manual intervention.

---

### User Story 2 - Manage Application Secrets Securely (Priority: P2)

A platform engineer needs all sensitive configuration values (database credentials, API keys, third-party tokens) to be stored and injected via the cluster's native secret management system rather than hardcoded in configuration files or baked into the container image. The approach must also support a future migration to a managed cloud secrets service without requiring application code changes.

**Why this priority**: Security of credentials is non-negotiable before external exposure. Secrets must be in place and validated before traffic is routed to the application.

**Independent Test**: Can be tested by deploying the application with secrets stored in the cluster's secret store, confirming the application starts and connects to its dependencies, and verifying that no plaintext credentials appear in deployment manifests, pod specs, or logs.

**Acceptance Scenarios**:

1. **Given** sensitive configuration values are stored in the cluster's secret store, **When** the application pods start, **Then** the pods receive those values as environment variables or mounted files — no secrets appear in Helm chart source files or version-controlled values files.
2. **Given** a secret value is rotated in the cluster's secret store, **When** the application is redeployed, **Then** the updated value is picked up without modifying any chart source file.
3. **Given** the secret management approach is in place, **When** the team later migrates secrets to a managed cloud secrets service, **Then** only the secret injection mechanism needs to change — application code and Helm templates remain unaffected.

---

### User Story 3 - Expose API Endpoints and Swagger UI Externally via Ingress (Priority: P3)

A developer or API consumer needs to reach the BlitzPay backend REST endpoints and the interactive API documentation (Swagger UI) through a stable, externally accessible URL rather than through port-forwarding or cluster-internal addresses.

**Why this priority**: External access is the end goal of the deployment. It builds on a working deployment (P1) and secure secrets (P2).

**Independent Test**: Can be tested by sending HTTP/HTTPS requests to the configured public hostname and verifying that both API endpoints and the Swagger UI respond correctly, and that only intended paths are exposed.

**Acceptance Scenarios**:

1. **Given** the Helm chart is installed with ingress enabled, **When** an external client sends a request to the configured hostname for an API endpoint, **Then** the request is routed to the BlitzPay application and a valid response is returned.
2. **Given** the ingress is configured, **When** a developer navigates to the Swagger UI path in a browser, **Then** the interactive API documentation loads and allows exploration of all available endpoints.
3. **Given** the ingress is active, **When** a request is made to a path not mapped to the application, **Then** the ingress returns an appropriate error without exposing internal cluster details.
4. **Given** TLS is configured, **When** a client connects over HTTPS, **Then** the connection is established with a valid certificate and all traffic is encrypted in transit.

---

### Edge Cases

- What happens when the Docker image tag referenced in the Helm values does not exist in the registry?
- How does the system behave when a required secret is missing or malformed in the cluster's secret store at pod startup?
- What happens when the ingress hostname is not yet resolvable in DNS at the time of deployment?
- How is the deployment handled when the cluster has insufficient resources to schedule new pods?
- What happens if the application fails its readiness probe repeatedly during a rolling update — does the rollout halt and preserve the previous version?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The deployment configuration MUST be packaged as a Helm chart that can install and upgrade the BlitzPay application in a Kubernetes cluster with a single command.
- **FR-002**: The Helm chart MUST accept the Docker image repository and tag as configurable values so the CD pipeline can pass in the image produced by the existing build step.
- **FR-003**: The Helm chart MUST support rolling updates so that new versions replace old pods without causing downtime.
- **FR-004**: All sensitive configuration values (database credentials, API keys, tokens) MUST be sourced from the cluster's secret management system and injected into the application at runtime — they MUST NOT appear in any version-controlled file.
- **FR-005**: The secret management integration MUST be designed so that the injection mechanism can be replaced by a managed cloud secrets service without changes to application code or Helm templates.
- **FR-006**: The Helm chart MUST include an ingress resource that routes external HTTP/HTTPS traffic to the BlitzPay backend service.
- **FR-007**: The ingress MUST expose all backend REST API endpoints under a configurable hostname and path prefix.
- **FR-008**: The ingress MUST expose the Swagger UI at a dedicated, configurable path accessible to developers.
- **FR-009**: The ingress configuration MUST support TLS termination with a certificate referenced by name; both the hostname and certificate reference MUST be configurable per environment.
- **FR-010**: The application MUST define liveness, readiness, and startup probes so the cluster can determine when a pod is healthy and ready to receive traffic. The startup probe allows for slow JVM startup without triggering premature liveness failures.
- **FR-011**: The Helm chart MUST be configurable for multiple environments (at minimum: staging and production) via separate values files without modifying chart templates.
- **FR-012**: The ingress MUST block external access to `/actuator` paths. Kubernetes liveness, readiness, and startup probes access actuator endpoints directly on the pod and are not affected by ingress-level restrictions.
- **FR-013**: The pod template MUST include Prometheus scrape annotations (`prometheus.io/scrape: "true"`, `prometheus.io/port: "8080"`, `prometheus.io/path: "/actuator/prometheus"`) so that Prometheus can automatically discover and scrape the application's metrics endpoint from within the cluster.
- **FR-014**: The pod template MUST include sufficient labels (application name, instance, environment) so that Loki can correlate log streams with the correct application and environment without manual configuration.
- **FR-015**: The CD pipeline deploy job MUST run on a self-hosted GitHub Actions runner that has network access to the target Kubernetes cluster (`https://myserver:6443`). The runner MUST have `helm` and `kubectl` pre-installed.

### Key Entities

- **Helm Chart**: The packaged deployment configuration describing all cluster resources required to run BlitzPay (workload, service, ingress, secret references, probes).
- **Docker Image**: The container image produced by the CD pipeline, referenced by repository and tag in the Helm chart values.
- **Cluster Secret**: A cluster-managed store entry holding a sensitive configuration value that is injected into application pods at runtime.
- **Ingress Resource**: The routing rule that maps external hostnames and URL paths to the internal BlitzPay service.
- **Environment Values File**: A per-environment configuration override file specifying image tags, hostnames, replica counts, and other environment-specific settings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A platform engineer can deploy or upgrade BlitzPay to a target cluster in under 10 minutes from triggering the deployment step.
- **SC-002**: Zero secrets appear as plaintext in any version-controlled file, pod specification, or application log after deployment.
- **SC-003**: Rolling deployments complete with zero observable downtime — existing requests are served by the previous version until the new version passes its readiness check.
- **SC-004**: 100% of defined REST API endpoints and the Swagger UI are reachable via the configured external hostname immediately after a successful deployment. The `/actuator` path prefix returns 403 or 404 when accessed via the public ingress hostname.
- **SC-005**: A secret value can be rotated in the cluster's secret store and picked up on the next deployment without changes to any chart source or template file.
- **SC-006**: Migrating secrets to a managed cloud service requires changes only to the secret injection layer — no changes to application code or Helm templates.
- **SC-007**: Deployment failures (bad image tag, missing secret, failed readiness probe) surface a clear, actionable error within 5 minutes so the engineer can diagnose without inspecting cluster internals.
- **SC-008**: After a successful deployment, BlitzPay metrics are discoverable and scrapeable by Prometheus via pod annotations (cluster-internal only). Application logs are ingested into Loki and queryable in Grafana by application name and environment label.

## Assumptions

- The CD pipeline (`cd.yml`) already builds and pushes the BlitzPay Docker image to a container registry; this feature adds only the Kubernetes deployment step downstream of that existing step.
- The initial target cluster is a kind (Kubernetes IN Docker) cluster running locally at `https://myserver:6443`. The CD deploy job runs on a self-hosted GitHub Actions runner on the same network as this cluster.
- `helm` and `kubectl` are pre-installed on the self-hosted runner. The runner's kubeconfig is pre-configured for the kind cluster (or the kubeconfig is stored as a GitHub Actions secret and decoded at deploy time).
- An ingress controller is already installed in the cluster and capable of handling HTTP/HTTPS traffic.
- DNS for the target ingress hostname(s) will be configured externally by the team; this feature delivers the ingress configuration but not DNS record management.
- TLS certificates are managed externally (e.g., by a certificate manager in the cluster or provided manually); the chart references them by name.
- Initial secrets are created manually in the cluster's secret store by a platform engineer before deployment; automated secret provisioning at deployment time is out of scope for this feature.
- The future migration to a managed cloud secrets service (e.g., AWS Secrets Manager) is out of scope for this feature, but the design must not prevent it.
- Multi-environment support covers at minimum staging and production environments.
- The Swagger UI is already served by the BlitzPay application at a known internal path; the ingress only needs to route to it.
- Prometheus, Grafana, and Loki are already deployed and operational in the cluster. The Helm chart provides the pod-level configuration hooks (scrape annotations, labels) but does not install or configure the observability stack itself.

## Clarifications

### Session 2026-03-27

- Q: Should the `/actuator` path prefix be accessible externally via the ingress? → A: No — block `/actuator` at the ingress level; Kubernetes probes reach actuator endpoints directly on the pod and are unaffected.
- Q: What observability stack is in use and what Helm chart configuration is required? → A: Grafana, Prometheus, and Loki — add Prometheus scrape annotations to the pod template and ensure pod labels include app name and environment for Loki log correlation.
- Q: What is the CD execution environment and target cluster? → A: Self-hosted GitHub Actions runner deploying to a local kind cluster at `https://myserver:6443`; `helm` and `kubectl` pre-installed on the runner.