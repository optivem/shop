# Plan: Swap Simulated Deployment for Real Cloud Deployment

## Status: DRAFT

## Context

Currently, QA/Production stages use `optivem/actions/simulate-deployment@v1` which just runs `docker compose up -d` on the GitHub Actions runner. It prints a note saying "in real applications, this is where you would deploy to GCP/AWS/Azure/etc."

This plan replaces that simulation with a real deployment to **Google Cloud Run**, while keeping simulated Docker deployment as a fallback option students can choose during repo initialization.

---

## Phase 1: Create `deploy-to-cloud-run` Reusable Action

**Where:** `optivem/actions/deploy-to-cloud-run/action.yml`

Create a new composite action that:
1. Authenticates to Google Cloud using Workload Identity Federation (preferred) or a service account key
2. Pushes the Docker image to Google Artifact Registry (or reuses GHCR image directly)
3. Deploys to Cloud Run using `gcloud run deploy`
4. Waits for the service to be ready and outputs the service URL
5. Runs health checks against the live URL (same `systems` input format as `simulate-deployment`)

**Inputs** (keep interface similar to `simulate-deployment` for easy swapping):
- `environment` - target environment (qa, production)
- `version` - release version
- `image-urls` - Docker image URLs (from GHCR)
- `systems` - JSON array of systems to health check (URLs resolved after deploy)
- `project-id` - GCP project ID
- `region` - GCP region (default: `us-central1`)
- `service-name` - Cloud Run service name

**Outputs:**
- `service-url` - The live Cloud Run URL

### Cloud Run specifics
- Each environment gets its own Cloud Run service (e.g. `shop-monolith-java-qa`, `shop-monolith-java-prod`)
- PostgreSQL via **Cloud SQL** (or AlloyDB) with Cloud SQL Auth Proxy
- External mock services (ERP/Clock/Tax) deployed as separate Cloud Run services
- Environment variables and secrets managed via Cloud Run environment config + Secret Manager
- Allow unauthenticated access for the public-facing URL (or use IAM for internal services)

---

## Phase 2: Create `deploy-to-docker` Action (Rename Current Simulation)

**Where:** `optivem/actions/deploy-to-docker/action.yml`

- Copy current `simulate-deployment/action.yml` logic into `deploy-to-docker`
- Remove the "this is a simulation" messaging -- it's now a legitimate local Docker deployment target
- Keep `simulate-deployment` as a deprecated wrapper that calls `deploy-to-docker` (backward compat)

---

## Phase 3: Add `--deploy` Flag to `gh optivem init`

**Where:** `gh-optivem` (Go CLI extension)

`gh optivem init` already selects workflows based on `--arch` and `--lang`. Add a new flag:

```
--deploy   Deployment target: "docker" (default) or "cloud-run"
```

### How it works

The starter repo contains **both sets** of workflow files side by side:
- `monolith-java-acceptance-stage.yml` -- Docker-based (existing)
- `monolith-java-acceptance-stage-cloud.yml` -- Cloud Run-based (new)

When `gh optivem init` runs, it copies only the matching set:

```
--deploy docker     → copies *-acceptance-stage.yml, *-qa-stage.yml, *-prod-stage.yml
--deploy cloud-run  → copies *-acceptance-stage-cloud.yml, *-qa-stage-cloud.yml, *-prod-stage-cloud.yml
```

Both get renamed to the same target names in the student's repo (e.g. `acceptance-stage.yml`), so the student experience is identical regardless of target. They just have different internal job structures.

### Changes to `gh-optivem`

1. **`config.go`**: Add `Deploy` field to config, parse `--deploy` flag, validate values
2. **`apply_template.go`**: In `CopyWorkflows()`, select the `-cloud.yml` suffix variants when `Deploy == "cloud-run"`
3. **`github_setup.go`**: When `Deploy == "cloud-run"`, also set GCP-related variables (`GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`) -- either from additional flags or by running `setup-gcp.sh` as a post-init step
4. **`.optivem/config.json`**: Store the deploy target so future tooling knows:
   ```json
   {
     "architecture": "monolith",
     "deploy": "cloud-run"
   }
   ```

### Student experience

```bash
# Docker (default, same as today)
gh optivem init --owner myorg --repo my-shop --system-name "My Shop" \
  --arch monolith --lang java

# Cloud Run
gh optivem init --owner myorg --repo my-shop --system-name "My Shop" \
  --arch monolith --lang java --deploy cloud-run
```

If `--deploy cloud-run` is chosen, the tool prints a post-init message:

```
Repository created with Cloud Run deployment pipeline.

Next step: Run the GCP setup script to configure your cloud environment:
  cd my-shop && ./setup-gcp.sh
```

---

## Phase 4: Create Cloud Run Workflow Files in Starter

**Important:** Do NOT modify existing Docker-based workflows. Create new `-cloud.yml` variants alongside them. The job graph is fundamentally different:
- Docker workflows: deployment + tests in a single job (everything on one runner via Docker Compose)
- Cloud Run workflows: deploy jobs in parallel, then fan out test jobs in parallel against live URLs

### New files to create in starter (18 total)
- `*-acceptance-stage-cloud.yml` (6 files)
- `*-qa-stage-cloud.yml` (6 files)
- `*-prod-stage-cloud.yml` (6 files)

### Job graph comparison

**Docker (existing) -- sequential single job:**
```
check → run (deploy + all tests in sequence) → summary
```

**Cloud Run (new) -- parallel multi-job:**
```
check
  → deploy-app          ─┐
  → deploy-external-real ─┤ (parallel deploys)
  → deploy-external-stub ─┘
      → test-smoke-real     ─┐
      → test-smoke-stub     ─┤
      → test-acceptance-api ─┤ (parallel test execution)
      → test-acceptance-ui  ─┤
      → test-contract-real  ─┤
      → test-contract-stub  ─┤
      → test-e2e-api        ─┤
      → test-e2e-ui         ─┘
          → promote-to-rc
              → summary
```

### Example: `monolith-java-acceptance-stage-cloud.yml`

```yaml
name: monolith-java-acceptance-stage-cloud

on:
  workflow_dispatch:

jobs:
  check:
    # Same as existing -- validate config, find latest images, check run conditions
    ...

  deploy-app:
    needs: check
    runs-on: ubuntu-latest
    outputs:
      app-url: ${{ steps.deploy.outputs.service-url }}
    steps:
      - uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ vars.GCP_WORKLOAD_IDENTITY_PROVIDER }}
          service_account: ${{ vars.GCP_SERVICE_ACCOUNT }}
      - uses: optivem/actions/deploy-to-cloud-run@v1
        id: deploy
        with:
          service-name: shop-monolith-java-acceptance
          image-url: ghcr.io/.../monolith-system-java:latest
          project-id: ${{ vars.GCP_PROJECT_ID }}
          region: ${{ vars.GCP_REGION }}

  deploy-external-real:
    needs: check
    runs-on: ubuntu-latest
    outputs:
      external-url: ${{ steps.deploy.outputs.service-url }}
    steps:
      - uses: optivem/actions/deploy-to-cloud-run@v1
        id: deploy
        with:
          service-name: shop-external-real-acceptance
          ...

  deploy-external-stub:
    needs: check
    runs-on: ubuntu-latest
    outputs:
      external-url: ${{ steps.deploy.outputs.service-url }}
    steps:
      - uses: optivem/actions/deploy-to-cloud-run@v1
        id: deploy
        with:
          service-name: shop-external-stub-acceptance
          ...

  # Test jobs run in parallel, each depending on the deploys they need
  test-smoke-real:
    needs: [deploy-app, deploy-external-real]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: optivem/actions/setup-java-gradle@v1
      - run: ./gradlew test -Dtype=smoke -DexternalSystemMode=real -DbaseUrl=${{ needs.deploy-app.outputs.app-url }}
        working-directory: system-test/java

  test-smoke-stub:
    needs: [deploy-app, deploy-external-stub]
    ...

  test-acceptance-api:
    needs: [deploy-app, deploy-external-real]
    ...

  test-acceptance-ui:
    needs: [deploy-app, deploy-external-real]
    ...

  # ... more parallel test jobs ...

  promote-to-rc:
    needs: [test-smoke-real, test-smoke-stub, test-acceptance-api, test-acceptance-ui, ...]
    if: ${{ !failure() && !cancelled() }}
    ...
```

### Verify pipeline

Also create `_verify-pipeline-cloud.yml` reusable workflow that chains the cloud stages, parallel to the existing `_verify-pipeline.yml`. `gh optivem init` copies the matching one.

---

## Phase 5: GCP Infrastructure Setup (Fully Scripted)

The entire GCP setup is automated so students only need to:
1. Have a Google account with billing enabled (free tier is fine)
2. Install `gcloud` CLI
3. Run one command

### Prerequisites (manual, one-time)
- Create a GCP account at https://cloud.google.com (free $300 credit for new accounts)
- Install the `gcloud` CLI: https://cloud.google.com/sdk/docs/install

### The setup script: `setup-gcp.sh`

**Where:** Repo root, runs from the student's terminal after `init.sh` picks Cloud Run.

```bash
./setup-gcp.sh
```

The script does everything automatically:

```
Checking gcloud CLI... OK
Checking gh CLI... OK

Logging in to Google Cloud...
> [Opens browser for OAuth login]

Step 1/7: Creating GCP project...
> Project "shop-deploy-a1b2c3" created (auto-generated unique ID)
> Linked to billing account

Step 2/7: Enabling APIs...
> Cloud Run API .............. enabled
> Artifact Registry API ...... enabled
> Secret Manager API ......... enabled

Step 3/7: Creating Artifact Registry repository...
> Repository "shop-images" created in us-central1

Step 4/7: Setting up Workload Identity Federation...
> Created Workload Identity Pool: "github-actions"
> Created Provider linked to: github.com/<student-org>/<student-repo>
> Created Service Account: "github-deployer@shop-deploy-a1b2c3.iam.gserviceaccount.com"
> Granted roles: run.admin, iam.serviceAccountUser, artifactregistry.writer

Step 5/7: Setting up database (Neon PostgreSQL)...
> [Opens browser to create free Neon account + project]
> Paste your Neon connection string: ********
> Stored in GCP Secret Manager as "db-connection-string"

Step 6/7: Configuring GitHub repo secrets...
> Set variable: GCP_PROJECT_ID = shop-deploy-a1b2c3
> Set variable: GCP_REGION = us-central1
> Set variable: GCP_WORKLOAD_IDENTITY_PROVIDER = projects/.../providers/github
> Set variable: GCP_SERVICE_ACCOUNT = github-deployer@shop-deploy-a1b2c3.iam...
> Set secret: NEON_DATABASE_URL = ********

Step 7/7: Writing deploy-config.yml...
> deployment-target: cloud-run
> cloud-run.project-id: shop-deploy-a1b2c3
> cloud-run.region: us-central1

Done! Your pipeline will deploy to Google Cloud Run.
Run your QA stage to test: gh workflow run monolith-java-qa-stage
```

### What the script calls under the hood

```bash
# 1. Create project with auto-generated unique ID
PROJECT_ID="shop-deploy-$(openssl rand -hex 3)"
gcloud projects create "$PROJECT_ID"
gcloud config set project "$PROJECT_ID"

# Link billing (finds first billing account automatically)
BILLING_ACCOUNT=$(gcloud billing accounts list --format="value(name)" --limit=1)
gcloud billing projects link "$PROJECT_ID" --billing-account="$BILLING_ACCOUNT"

# 2. Enable APIs
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  iamcredentials.googleapis.com

# 3. Create Artifact Registry repo
gcloud artifacts repositories create shop-images \
  --repository-format=docker \
  --location="$REGION"

# 4. Workload Identity Federation (keyless auth from GitHub Actions)
REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')

gcloud iam workload-identity-pools create github-actions \
  --location=global \
  --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github \
  --location=global \
  --workload-identity-pool=github-actions \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='$REPO'"

# Create service account and bind to WIF
gcloud iam service-accounts create github-deployer \
  --display-name="GitHub Actions Deployer"

SA_EMAIL="github-deployer@${PROJECT_ID}.iam.gserviceaccount.com"
WIF_MEMBER="principalSet://iam.googleapis.com/projects/$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')/locations/global/workloadIdentityPools/github-actions/attribute.repository/$REPO"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/run.admin"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/iam.serviceAccountUser"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/artifactregistry.writer"

gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --member="$WIF_MEMBER" \
  --role="roles/iam.workloadIdentityUser"

# 5. Neon DB -- prompt for connection string (account created via browser)
read -rp "Paste your Neon connection string: " NEON_URL
echo -n "$NEON_URL" | gcloud secrets create db-connection-string --data-file=-

# 6. Set GitHub repo variables/secrets via gh CLI
WIF_PROVIDER="projects/$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')/locations/global/workloadIdentityPools/github-actions/providers/github"

gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --body "$WIF_PROVIDER"
gh variable set GCP_SERVICE_ACCOUNT --body "$SA_EMAIL"
gh secret set NEON_DATABASE_URL --body "$NEON_URL"

# 7. Write deploy-config.yml
cat > deploy-config.yml <<EOF
deployment-target: cloud-run
cloud-run:
  project-id: $PROJECT_ID
  region: $REGION
EOF
```

### Teardown script: `teardown-gcp.sh`

Also provide a teardown script so students can clean up when done:

```bash
./teardown-gcp.sh
# Reads project ID from deploy-config.yml
# Deletes the entire GCP project (removes all resources)
# Removes GitHub repo variables/secrets
# Resets deploy-config.yml to docker
```

### The only manual step: Neon database

Neon requires browser-based signup (GitHub OAuth). The script opens the browser, then the student pastes the connection string back. Everything else is fully automated.

**Alternative:** If we want zero manual steps, we could use the Neon API with a CLI token. But the browser signup is a one-time 30-second step that gives students a free PostgreSQL database, so it's a reasonable tradeoff.

### Cost for students
- Cloud Run free tier: 2M requests/month -- more than enough
- Neon free tier: 0.5 GB storage, 190 compute hours/month -- more than enough
- **Estimated monthly cost: $0**
- GCP free trial: $300 credit for 90 days (new accounts)

---

## Phase 6: Documentation

1. **`docs/deployment-setup.md`** -- Explains what the scripts do and troubleshooting
2. **Update `README.md`** -- mention deployment target choice
3. The scripts themselves serve as the primary "guide" -- students shouldn't need to read docs to get set up

---

## Implementation Order

| Step | What | Where | Effort |
|------|------|-------|--------|
| 1 | Create `deploy-to-cloud-run` action | `optivem/actions` | Medium |
| 2 | Rename `simulate-deployment` to `deploy-to-docker` | `optivem/actions` | Small |
| 3 | Create 18 `*-cloud.yml` workflow files in starter | `optivem/starter` | Medium |
| 4 | Create `_verify-pipeline-cloud.yml` | `optivem/starter` | Small |
| 5 | Add `setup-gcp.sh` and `teardown-gcp.sh` scripts | `optivem/starter` | Medium |
| 6 | Add `--deploy` flag to `gh optivem init` | `optivem/gh-optivem` | Medium |
| 7 | Update workflow copy logic for `-cloud.yml` variants | `optivem/gh-optivem` | Small |
| 8 | Test end-to-end with one combo (Java monolith, cloud-run) | All repos | Medium |
| 9 | Roll out remaining 5 language/arch `-cloud.yml` files | `optivem/starter` | Small (mechanical) |

---

## Open Questions

1. **Acceptance stage:** Should acceptance tests also run against Cloud Run, or always against local Docker? Running against Cloud Run would be more realistic but slower and costlier.
2. **External systems (ERP/Clock/Tax):** Deploy mock servers to Cloud Run too, or keep them as Docker sidecar in test runner? Deploying to Cloud Run is more realistic but adds complexity.
3. **Future targets:** Should we design the `--deploy` flag to support AWS/Azure later (e.g. `--deploy aws-ecs`)?
