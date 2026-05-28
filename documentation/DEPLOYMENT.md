# Checkmate++ — Deployment Guide

This guide covers deploying Checkmate++ to a Kubernetes cluster. There
are two supported paths:

- **Manual Helm install** — for ad-hoc clusters, demos, and CI
  smoke-tests.
- **Production GitOps** — what `checkmateplus.org` actually runs:
  Flux reconciles the cluster from this Git repo, Bitnami sealed-secrets
  stores secrets in-repo, and a Cloudflared tunnel exposes the cluster
  to the internet without a public LoadBalancer.

For local dev see [INSTALL.md](INSTALL.md).

## Repo layout

```
k8s/checkmateplus/         Umbrella Helm chart (this is what gets deployed)
  Chart.yaml               Lists sub-charts + bitnami postgresql, redis
  values.yaml              All defaults — versions, ports, env, resources
  charts/                  One sub-chart per service (frontend, gameserver, ...)
  templates/               ServiceAccount and shared helpers

clusters/prod-clust-1/     Flux config for the prod cluster
  kustomization.yaml       Lists every resource Flux applies to the cluster
  namespace.yaml           Creates the `checkmateplus` namespace
  gitrepository.yaml       Tells Flux which Git repo + branch to follow
  flux-app-sync.yaml       Kustomization that points Flux at `app/`
  infrastructure.yaml      Kustomization for the cluster itself
  sealed-secrets-*.yaml    Installs the sealed-secrets controller via Helm
  *-sealed-secret.yaml     Encrypted secrets safe to commit
  pub-sealed-secrets.pem   Public key for sealing new secrets
  app/checkmateplus-helmrelease.yaml   The HelmRelease that deploys the chart
```

## Versions and images

All images are published to the Kent GitLab registry:
`git.cs.kent.ac.uk:5050/jhl28/checkmate/<service>:<tag>`. Image tags are
pinned in `k8s/checkmateplus/values.yaml`; bump them there to roll a
release. The umbrella chart version (`Chart.yaml`) lives at `0.2.x`,
`appVersion` tracks the release line (`2.2.0` at time of writing).

The chart also pulls Bitnami `postgresql` and `redis` from Docker Hub.

## Prerequisites

- A Kubernetes cluster (any flavour — `kind`, `minikube`, k3s, EKS, …).
- `kubectl` configured for that cluster.
- `helm` 3.x.
- Cluster pull-secret access to the GitLab registry (see below).
- For the GitOps path: `flux` CLI, and `kubeseal` if you need to seal
  new secrets.

### Pull secret for the GitLab registry

Most images are private. Create an `imagePullSecret` in the namespace
the chart will live in:

```bash
kubectl create namespace checkmateplus

kubectl -n checkmateplus create secret docker-registry gitlab-registry \
  --docker-server=git.cs.kent.ac.uk:5050 \
  --docker-username=<gitlab-username> \
  --docker-password=<personal-access-token>
```

The HelmRelease in the prod cluster references this exact secret name
(`gitlab-registry`) under `imagePullSecrets` for every sub-chart.

## Path A — manual Helm install

Use this if you don't need GitOps, sealed-secrets, or Cloudflared — for
example a `kind` cluster on your laptop.

### 1. Provide the required secrets

The chart expects two existing Secrets in the target namespace:

- `checkmateplus-playerservice-secret` — used by PlayerService,
  MatchmakingService, and GameServer.
- `checkmateplus-voicecommandservice-secret` — used by
  VoiceCommandService.

For a local install you can create plain Secrets manually:

```bash
kubectl -n checkmateplus create secret generic \
  checkmateplus-playerservice-secret \
  --from-literal=jwtSecret='+/J48zc2Hq1vtHZNU+9HGXZ6vP3ZDaX4UtgNWqR7eSY=' \
  --from-literal=postgresUser='checkmateuser' \
  --from-literal=postgresPassword='change-me'

kubectl -n checkmateplus create secret generic \
  checkmateplus-voicecommandservice-secret \
  --from-literal=jwtSecret='change-me' \
  --from-literal=OPENAI_API_KEY='sk-...'
```

The Postgres credentials come from the same playerservice secret —
`values.yaml` wires `postgresql.auth.existingSecret` to it.

### 2. Install the chart

From the repo root:

```bash
helm dependency update k8s/checkmateplus

helm upgrade --install checkmateplus k8s/checkmateplus \
  --namespace checkmateplus \
  --set cloudflared.enabled=false
```

`cloudflared.enabled=false` skips the tunnel — appropriate when you're
not exposing the cluster on `checkmateplus.org`. Use `kubectl
port-forward` to reach services from your laptop:

```bash
kubectl -n checkmateplus port-forward svc/checkmateplus-frontend 8000:80
kubectl -n checkmateplus port-forward svc/checkmateplus-matchmakingservice 8080:8080
```

### 3. Verify

```bash
kubectl -n checkmateplus get pods
helm -n checkmateplus status checkmateplus
```

Every pod should reach `Running`/`Ready`. Postgres and Redis come from
the Bitnami sub-charts and provision automatically.

### 4. Upgrade

Edit `values.yaml` (typically just an image tag) and rerun the same
`helm upgrade --install` command.

### 5. Uninstall

```bash
helm -n checkmateplus uninstall checkmateplus
kubectl delete namespace checkmateplus
```

## Path B — production GitOps (Flux + sealed-secrets + Cloudflared)

This is the setup used by the live `checkmateplus.org` cluster
(`prod-clust-1`). Once it's bootstrapped, deploying a new release is
just a Git push.

### Topology

```
GitLab repo (branch: microservices)
      │
      ▼  pulled every 1m
 Flux GitRepository "checkmateplus"
      │
      ├─ Kustomization "checkmateplus-infra"  → clusters/prod-clust-1/
      │     installs sealed-secrets, sealed secrets, namespace,
      │     gitlab-registry imagePullSecret
      │
      └─ Kustomization "checkmateplus-app"   → clusters/prod-clust-1/app/
            HelmRelease "checkmateplus" → Helm chart at k8s/checkmateplus

The HelmRelease pulls cloudflared.tunnelToken from a sealed secret and
deploys the umbrella chart into the `checkmateplus` namespace.
```

### 1. Bootstrap the cluster (one-off)

Install Flux into the cluster — typically with `flux bootstrap`:

```bash
flux bootstrap gitlab \
  --hostname=git.cs.kent.ac.uk \
  --owner=jhl28 \
  --repository=checkmate \
  --branch=microservices \
  --path=clusters/prod-clust-1 \
  --token-auth
```

This creates a Deploy Key / token in GitLab, commits Flux's own
manifests, and starts the reconciliation loop. From this point Flux
applies anything under `clusters/prod-clust-1/`, which in turn applies
everything in `k8s/checkmateplus/`.

If you're bootstrapping by hand instead, apply the kustomization
directly:

```bash
kubectl apply -k clusters/prod-clust-1
```

### 2. Sealed secrets

Secrets that need to live in Git are encrypted with
[Bitnami Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets).
The controller is installed by the Flux HelmRelease in
`sealed-secrets-helmrelease.yaml` (chart from
`https://bitnami-labs.github.io/sealed-secrets`).

The committed `SealedSecret` resources are:

| File                                       | Decrypts to (`Secret` name)                 | Used by                          |
| ------------------------------------------ | ------------------------------------------- | -------------------------------- |
| `gitlab-auth-sealed-secret.yaml`           | `gitlab-auth`                               | Flux — credentials for the Git repo |
| `gitlab-registry-sealed-secret.yaml`       | `gitlab-registry`                           | All workloads — image pull secret |
| `cloudflared-sealed-secret.yaml`           | `checkmateplus-cloudflared-secret`          | HelmRelease (provides tunnel token) |
| `playerservice-sealed-secret.yaml`         | `checkmateplus-playerservice-secret`        | PlayerService, GameServer, MatchmakingService, Postgres |
| `voicecommandservice-sealed-secret.yaml`   | `checkmateplus-voicecommandservice-secret`  | VoiceCommandService              |

#### Sealing a new secret

The cluster's public key is committed at
`clusters/prod-clust-1/pub-sealed-secrets.pem`. You don't need cluster
access to seal a secret.

```bash
# Build the Secret you want
kubectl create secret generic my-new-secret \
  --namespace=checkmateplus \
  --from-literal=key=supersecret \
  --dry-run=client -o yaml > /tmp/my-new-secret.yaml

# Seal it with the public cert from the repo
kubeseal --format=yaml \
  --cert=clusters/prod-clust-1/pub-sealed-secrets.pem \
  < /tmp/my-new-secret.yaml \
  > clusters/prod-clust-1/my-new-sealed-secret.yaml

# Add the file to clusters/prod-clust-1/kustomization.yaml and commit.
```

Flux applies the new `SealedSecret`, the controller decrypts it into a
plain `Secret` in the same namespace, and workloads pick it up.

#### Rotating the sealing key

If `pub-sealed-secrets.pem` is regenerated (or the cluster is rebuilt),
every committed `SealedSecret` must be resealed. Export the new public
key with:

```bash
kubeseal --fetch-cert > clusters/prod-clust-1/pub-sealed-secrets.pem
```

### 3. Cloudflared tunnel

There is no public LoadBalancer or Ingress. Instead, the `cloudflared`
sub-chart runs a tunnel pod that connects outbound to Cloudflare's edge.
Cloudflare routes the public hostnames to in-cluster services.

- The tunnel **token** is stored in `checkmateplus-cloudflared-secret`
  (sealed) and injected into the HelmRelease via `valuesFrom`:

  ```yaml
  valuesFrom:
    - kind: Secret
      name: checkmateplus-cloudflared-secret
      valuesKey: tunnelToken
      targetPath: cloudflared.tunnelToken
  ```

- The **route map** (hostname → in-cluster service) lives in
  `k8s/checkmateplus/values.yaml` under `cloudflared.ingress`:

  ```yaml
  ingress:
    - hostname: checkmateplus.org
      service: http://checkmateplus-frontend...:80
    - hostname: ws.checkmateplus.org
      service: http://checkmateplus-matchmakingservice...:8080
    - hostname: gs0.checkmateplus.org
      service: http://checkmateplus-gameserver-0...:8090
    - hostname: voice.checkmateplus.org
      service: http://checkmateplus-voicecommandservice...:9100
    - service: http_status:404
  ```

  **Heads-up** (called out in the values file): the Cloudflare-side DNS
  records for these hostnames are configured manually in the Cloudflare
  dashboard. The list in `values.yaml` is the cluster-side mirror —
  changing it alone will not create new public routes. A future cleanup
  is to move to a community Cloudflare ingress controller.

### 4. Releasing a new version

1. Push the service image to the registry (CI handles this for tags on
   the `microservices` branch).
2. Bump the image tag in `k8s/checkmateplus/values.yaml` (e.g.
   `playerservice.image.tag`) and bump
   `k8s/checkmateplus/Chart.yaml#version`.
3. Commit and push to the `microservices` branch.
4. Within ~1 minute Flux reconciles the GitRepository; within ~5
   minutes the HelmRelease rolls out the new pods. Watch with:

   ```bash
   flux -n flux-system get kustomizations
   flux -n checkmateplus get helmreleases
   kubectl -n checkmateplus get pods -w
   ```

   Force an immediate reconcile if you're impatient:

   ```bash
   flux reconcile kustomization checkmateplus-app --with-source
   flux -n checkmateplus reconcile helmrelease checkmateplus
   ```

### 5. Rollback

If a release goes bad, revert the commit. Flux will re-apply the prior
state on the next reconcile. For an emergency manual rollback:

```bash
helm -n checkmateplus rollback checkmateplus
```

Note that Flux will undo this on the next sync — only useful as a stop
gap while you prepare the revert commit.

## Common operational tasks

### Inspect what Flux is doing

```bash
flux get sources git
flux -n flux-system get kustomizations
flux -n checkmateplus get helmreleases
flux logs --follow --tail 50
```

### Tail logs from a service

```bash
kubectl -n checkmateplus logs -f deploy/checkmateplus-playerservice
kubectl -n checkmateplus logs -f deploy/checkmateplus-matchmakingservice
```

### Open a shell against the in-cluster Postgres

```bash
kubectl -n checkmateplus exec -it sts/checkmateplus-postgresql -- \
  psql -U checkmateuser -d checkmate
```

### Force a Helm release re-render

```bash
flux -n checkmateplus suspend helmrelease checkmateplus
flux -n checkmateplus resume helmrelease checkmateplus
```

## Troubleshooting

- **`ImagePullBackOff` on every pod** — the `gitlab-registry` Secret is
  missing or has stale credentials. Recreate it (see Prerequisites).
- **`HelmRelease` stuck "install retries exhausted"** — `flux get hr -n
  checkmateplus` will show the error. Check the controller logs:
  `kubectl -n flux-system logs deploy/helm-controller`.
- **`SealedSecret` not unsealing** — controller running?
  `kubectl -n flux-system get pods | grep sealed-secrets`. If the
  controller's private key has changed since the secret was sealed,
  the SealedSecret needs to be regenerated against the new public key.
- **Public DNS resolves but `502` from Cloudflare** — the in-cluster
  Service name in `cloudflared.ingress` is wrong, or the target pod is
  not Ready. Compare `kubectl get svc -n checkmateplus` against the
  ingress map in `values.yaml`.
- **`postgresql.auth.existingSecret` error on first install** — create
  `checkmateplus-playerservice-secret` (or apply the sealed version)
  *before* `helm install`; Postgres init reads it.