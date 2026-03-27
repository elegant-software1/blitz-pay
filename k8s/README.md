# Kubernetes Deployment

> **The canonical deployment method is the Helm chart at `helm/blitz-pay/`.**
> The raw manifest `deployment.yaml` in this directory is deprecated and kept for reference only.
> Do not apply it directly.

---

## Prerequisites

- `kubectl` configured against the target cluster
- `helm` >= 3.x
- The two secrets below created in the target namespace

---

## Step 1 — Create the application credentials secret

Injected as environment variables into the container.

```bash
kubectl create secret generic blitz-pay-secrets \
  --namespace <namespace> \
  --from-literal=SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/quickpay_db" \
  --from-literal=SPRING_DATASOURCE_USERNAME="<user>" \
  --from-literal=SPRING_DATASOURCE_PASSWORD="<password>" \
  --from-literal=TRUELAYER_CLIENT_ID="<client-id>" \
  --from-literal=TRUELAYER_CLIENT_SECRET="<client-secret>" \
  --from-literal=TRUELAYER_KEY_ID="<key-id>" \
  --from-literal=TRUELAYER_MERCHANT_ACCOUNT_ID="<merchant-id>" \
  --from-literal=TRUELAYER_ACCESS_TOKEN="<access-token>"
```

## Step 2 — Create the TrueLayer PEM key secret

The TrueLayer SDK reads the EC private key from a **file path** — it cannot accept key
material as a plain environment variable string. The PEM content is stored in a dedicated
secret and mounted read-only into the container at
`/var/run/secrets/truelayer/private.pem`.

`TRUELAYER_PRIVATE_KEY_PATH` is set automatically by the Helm chart's ConfigMap — you do
not need to set it manually.

```bash
kubectl create secret generic blitz-pay-truelayer-pem \
  --namespace <namespace> \
  --from-file=private.pem=./ec512-private-key.pem
```

> **Why a separate secret?**
> `envFrom.secretRef` injects string environment variables only — it cannot mount files.
> File secrets require a volume mount, which must reference a dedicated secret.
> Keeping the PEM in its own secret also allows independent key rotation and maps cleanly
> to a single AWS Secrets Manager entry during future migration.

---

## Step 3 — Deploy with Helm

### Staging

```bash
helm upgrade --install blitz-pay ./helm/blitz-pay \
  --namespace blitz-pay-staging \
  --create-namespace \
  --values ./helm/blitz-pay/values.yaml \
  --values ./helm/blitz-pay/values-staging.yaml \
  --wait --timeout 5m
```

### Production

```bash
helm upgrade --install blitz-pay ./helm/blitz-pay \
  --namespace blitz-pay-prod \
  --create-namespace \
  --values ./helm/blitz-pay/values.yaml \
  --values ./helm/blitz-pay/values-prod.yaml \
  --set image.tag=<version> \
  --wait --timeout 5m
```

---

## Rotating secrets

### Rotate a credential

```bash
kubectl patch secret blitz-pay-secrets \
  --namespace blitz-pay-prod \
  --type='json' \
  -p='[{"op":"replace","path":"/data/TRUELAYER_ACCESS_TOKEN","value":"'$(echo -n "<new-token>" | base64)'"}]'
```

### Rotate the PEM key

```bash
kubectl create secret generic blitz-pay-truelayer-pem \
  --namespace blitz-pay-prod \
  --from-file=private.pem=./new-ec512-private-key.pem \
  --dry-run=client -o yaml | kubectl apply -f -
```

After rotating either secret, trigger a rolling restart:

```bash
kubectl rollout restart deployment/blitz-pay -n blitz-pay-prod
```

---

## Migrating secrets to AWS Secrets Manager

Both secrets are designed for zero-change migration via
[External Secrets Operator (ESO)](https://external-secrets.io/):

1. Install ESO in your cluster
2. Create an AWS IAM role for ESO (IRSA on EKS)
3. Create a `SecretStore` pointing to your AWS region
4. Create two `ExternalSecret` resources — one mapping `blitz-pay/prod/credentials` →
   `blitz-pay-secrets`, and one mapping `blitz-pay/prod/truelayer-private-key` →
   `blitz-pay-truelayer-pem` (key: `private.pem`)

ESO syncs both K8s Secrets automatically. **No changes to Helm templates or values files
are required.**

See `helm/blitz-pay/SECRET-SETUP.md` for the full ESO manifest examples.

---

## Troubleshooting

```bash
# Check pod status
kubectl get pods -n blitz-pay-prod -l app.kubernetes.io/name=blitz-pay

# Tail logs
kubectl logs -f deployment/blitz-pay -n blitz-pay-prod

# Describe pod (shows volume mount errors, image pull errors, etc.)
kubectl describe pod -l app.kubernetes.io/name=blitz-pay -n blitz-pay-prod

# Verify PEM file is present inside the container
kubectl exec -it deployment/blitz-pay -n blitz-pay-prod -- \
  ls -la /var/run/secrets/truelayer/

# Check rollout status
kubectl rollout status deployment/blitz-pay -n blitz-pay-prod

# Port-forward for local testing
kubectl port-forward svc/blitz-pay 8080:80 -n blitz-pay-prod
curl http://localhost:8080/actuator/health
```