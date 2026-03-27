# Quickstart: BlitzPay Kubernetes Deployment via Helm

**Branch**: `005-k8s-helm-deploy` | **Date**: 2026-03-27

This guide covers how to deploy BlitzPay to a Kubernetes cluster using the Helm chart after implementation is complete.

---

## Prerequisites

- `kubectl` configured with access to the target cluster
- `helm` 3.x installed
- Nginx Ingress Controller installed in the cluster
- cert-manager installed (for TLS certificate provisioning)
- The BlitzPay Docker image pushed to `ghcr.io/medimohammadise/blitzpay:<tag>`
- DNS for the ingress hostname pointing to the cluster's ingress load balancer IP

---

## Step 1: Create the Kubernetes Secret (once per environment)

Create the secret in the target namespace **before** running Helm. This is done once; the secret persists across deployments.

```bash
kubectl create namespace blitzpay-prod  # or blitzpay-staging

kubectl create secret generic blitzpay-secrets \
  --namespace blitzpay-prod \
  --from-literal=SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/quickpay_db" \
  --from-literal=SPRING_DATASOURCE_USERNAME="<db-user>" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="<db-password>" \
  --from-literal=TRUELAYER_CLIENT_ID="<client-id>" \
  --from-literal=TRUELAYER_CLIENT_SECRET="<client-secret>" \
  --from-literal=TRUELAYER_KEY_ID="<key-id>" \
  --from-literal=TRUELAYER_PRIVATE_KEY_PATH="/secrets/private.pem" \
  --from-literal=TRUELAYER_MERCHANT_ACCOUNT_ID="<merchant-id>" \
  --from-literal=TRUELAYER_ACCESS_TOKEN="<access-token>"
```

> **Never commit these values to Git.** The Helm chart references the secret by name only.

---

## Step 2: Deploy via Helm

### Staging

```bash
helm upgrade --install blitzpay ./helm/blitzpay \
  --namespace blitzpay-staging \
  --create-namespace \
  --values ./helm/blitzpay/values.yaml \
  --values ./helm/blitzpay/values-staging.yaml \
  --set image.tag=<image-tag> \
  --wait \
  --timeout 5m
```

### Production

```bash
helm upgrade --install blitzpay ./helm/blitzpay \
  --namespace blitzpay-prod \
  --create-namespace \
  --values ./helm/blitzpay/values.yaml \
  --values ./helm/blitzpay/values-prod.yaml \
  --set image.tag=<image-tag> \
  --wait \
  --timeout 5m
```

Replace `<image-tag>` with the release tag (e.g., `v0.2.2`) or commit SHA.

---

## Step 3: Verify the Deployment

```bash
# Check pods are running
kubectl get pods -n blitzpay-prod -l app.kubernetes.io/name=blitzpay

# Check rollout status
kubectl rollout status deployment/blitzpay -n blitzpay-prod

# Check ingress is configured
kubectl get ingress -n blitzpay-prod

# Verify the app responds via port-forward (actuator is blocked at ingress level)
kubectl port-forward deployment/blitzpay 8080:8080 -n blitzpay-prod &
curl http://localhost:8080/actuator/health/readiness
```

---

## Step 4: Access Swagger UI

Once deployed and DNS is configured:

```
https://<ingress-hostname>/swagger-ui
```

The OpenAPI schema is at:

```
https://<ingress-hostname>/api-docs
```

---

## Rolling Back

If a deployment fails:

```bash
helm rollback blitzpay -n blitzpay-prod
```

This restores the previous Helm release. Kubernetes automatically rolls back pods to the previous version.

---

## CD Pipeline (Automated)

The `cd.yml` workflow automatically runs `helm upgrade --install` after a GitHub release is published. The image tag from `github.ref_name` is passed via `--set image.tag`.

**Required GitHub Actions secret**:
- `KUBECONFIG`: base64-encoded kubeconfig for the target cluster

```bash
# Encode your kubeconfig
cat ~/.kube/config | base64 | pbcopy  # macOS
# Add as KUBECONFIG in GitHub repository secrets
```

---

## Future: Migrating Secrets to AWS Secrets Manager

When ready to migrate, install External Secrets Operator and create `SecretStore` + `ExternalSecret` resources pointing to AWS Secrets Manager. The Helm chart templates are unchanged — they continue to reference `blitzpay-secrets` by name, which ESO will create and keep in sync automatically. See `research.md` section 3 for migration steps.
