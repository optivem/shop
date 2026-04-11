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

## Phase 3: Add Deployment Target Choice to Repo Initialization

**Where:** Workflow dispatch inputs or a repo-level config file

**Option A (recommended): Config file approach**
- Add a `deploy-config.yml` at repo root:
  ```yaml
  deployment-target: cloud-run   # Options: cloud-run | docker
  cloud-run:
    project-id: my-gcp-project
    region: us-central1
  ```
- Students run a setup script or manually edit this file when they fork/clone the starter
- Workflows read this config to decide which deploy action to call

**Option B: Workflow dispatch input**
- Add a `deployment-target` choice input to QA/Prod stage workflows
- Students pick "Google Cloud Run" or "Docker (local)" each time they trigger

**Recommendation:** Option A -- config file is set-once and avoids repeated manual selection.

### Initialization flow
When a student forks or clones the repo, they run an init script (or follow a setup guide):

```
./init.sh
# or: gh workflow run init -- (if we make it a workflow)

> Choose deployment target:
>   1. Google Cloud Run (real cloud deployment)
>   2. Docker (simulated local deployment) [default]
> 
> [If Cloud Run selected:]
> Enter GCP Project ID: my-project-123
> Enter GCP Region [us-central1]: 
> 
> Writing deploy-config.yml...
> Done! Your pipeline will deploy to Google Cloud Run.
```

---

## Phase 4: Update QA and Production Stage Workflows

**Files affected (24 workflows):**
- `*-qa-stage.yml` (6 files)
- `*-prod-stage.yml` (6 files)  
- `*-acceptance-stage.yml` (6 files) -- acceptance uses simulate-deployment for test environments
- `*-acceptance-stage-legacy.yml` (6 files)

**Change:** Replace the `simulate-deployment` step with conditional logic:

```yaml
- name: Read Deploy Config
  id: config
  run: |
    TARGET=$(yq '.deployment-target' deploy-config.yml 2>/dev/null || echo 'docker')
    echo "target=$TARGET" >> "$GITHUB_OUTPUT"

- name: Deploy to Cloud Run
  if: steps.config.outputs.target == 'cloud-run'
  uses: optivem/actions/deploy-to-cloud-run@v1
  with:
    environment: ${{ inputs.environment || 'qa' }}
    version: ${{ inputs.version }}
    image-urls: ${{ needs.check.outputs.image-urls }}
    project-id: ${{ vars.GCP_PROJECT_ID }}
    region: ${{ vars.GCP_REGION }}
    service-name: shop-monolith-java

- name: Deploy to Docker (Local)
  if: steps.config.outputs.target == 'docker'
  uses: optivem/actions/deploy-to-docker@v1
  with:
    environment: ${{ inputs.environment || 'qa' }}
    version: ${{ inputs.version }}
    image-urls: ${{ needs.check.outputs.image-urls }}
    compose-file: docker-compose.pipeline.monolith.real.yml
    systems: |
      [...]
    working-directory: system-test/java
```

**For acceptance stages:** Keep Docker deployment for test execution (tests run against localhost on the runner). Cloud Run deployment is only for QA and Production stages.

---

## Phase 5: GCP Infrastructure Setup (Per-Student)

Students who choose Cloud Run need:

1. **GCP Project** -- one project per student (free tier eligible)
2. **Enable APIs:**
   - Cloud Run API
   - Artifact Registry API (if not pulling directly from GHCR)
   - Cloud SQL Admin API
   - Secret Manager API
3. **Workload Identity Federation** -- connects GitHub Actions to GCP without storing keys
   - Create a Workload Identity Pool
   - Create a Provider linked to the student's GitHub repo
   - Grant the pool's service account `roles/run.admin`, `roles/iam.serviceAccountUser`
4. **Cloud SQL PostgreSQL instance** -- one per environment (or shared with separate databases)
5. **GitHub repo secrets/variables:**
   - `GCP_PROJECT_ID` (variable)
   - `GCP_REGION` (variable)
   - `GCP_WORKLOAD_IDENTITY_PROVIDER` (variable)
   - `GCP_SERVICE_ACCOUNT` (variable)

### Cost for students
- Cloud Run free tier: 2M requests/month -- more than enough
- Cloud SQL: This is the expensive part (~$7/month minimum for the smallest instance)
  - **Alternative:** Use **Neon** or **Supabase** free-tier PostgreSQL instead of Cloud SQL to keep costs at $0
  - **Recommendation:** Use Neon free tier for database -- students get free PostgreSQL, no Cloud SQL cost

### Estimated monthly cost (Cloud Run + Neon free tier): $0-2/month

---

## Phase 6: Documentation and Setup Guide

1. **`docs/deployment-setup.md`** -- Step-by-step guide for both targets:
   - Docker path: no setup needed (default)
   - Cloud Run path: GCP project creation, API enabling, WIF setup, Neon DB setup
2. **Update `README.md`** -- mention deployment target choice
3. **Terraform/gcloud script** (optional) -- automate GCP setup:
   ```bash
   ./setup-gcp.sh my-project-123 us-central1
   ```

---

## Implementation Order

| Step | What | Effort |
|------|------|--------|
| 1 | Create `deploy-to-cloud-run` action | Medium |
| 2 | Rename `simulate-deployment` to `deploy-to-docker` | Small |
| 3 | Create `deploy-config.yml` + `init.sh` script | Small |
| 4 | Update QA + Prod workflows (12 files) with conditional deploy | Medium |
| 5 | Write GCP setup guide + optional Terraform | Medium |
| 6 | Test end-to-end with one language (Java monolith) | Medium |
| 7 | Roll out to all 6 language/arch combinations | Small (mechanical) |

---

## Open Questions

1. **Acceptance stage:** Should acceptance tests also run against Cloud Run, or always against local Docker? Running against Cloud Run would be more realistic but slower and costlier.
2. **Database choice:** Cloud SQL ($7/mo) vs Neon/Supabase (free) -- recommendation is Neon free tier.
3. **External systems (ERP/Clock/Tax):** Deploy mock servers to Cloud Run too, or keep them as Docker sidecar in test runner? Deploying to Cloud Run is more realistic but adds complexity.
4. **Future targets:** Should we design the config to support AWS/Azure later (e.g. `deployment-target: aws-ecs`)?
