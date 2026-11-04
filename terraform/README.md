# GCP Infrastructure with Terraform

This directory contains the Terraform configuration and helper scripts that set up the GCP and Neon infrastructure for the shop project, including:

- **GCP Project** with automatic naming
- **Artifact Registry** for Docker images
- **Workload Identity Federation** for GitHub Actions authentication
- **Secret Manager** for the database credentials and the Keycloak admin password
- **IAM bindings** for GitHub Actions deployment
- **Neon Postgres** project with one database per variant (created by `neonctl`, not Terraform)
- **Billing kill-switch**: a budget, a Pub/Sub topic and a Cloud Function that unlinks billing at about €1 (see [Cost](#cost))

## Quick Start

### Prerequisites

- `terraform` CLI (v1.0+) — [Install](https://www.terraform.io/downloads)
- `gcloud` CLI — [Install](https://cloud.google.com/sdk/docs/install)
- `gh` CLI — [Install](https://cli.github.com)
- `neonctl` — pinned by `NEONCTL_VERSION` in [neon.sh](neon.sh); falls back to `npx` if it is not installed
- `jq` and Node.js
- GCP account with active billing and a Neon account

The [dev container](../.devcontainer/README.md) has all of these.

### Setup (Automated)

From the repo root:

```bash
# Step 1: Authenticate
gh auth login
gcloud auth login --no-launch-browser
neonctl login   # or: export NEON_API_KEY=<key>

# Step 2: Run setup script
./setup-gcp.sh
```

The script will:
1. Check prerequisites
2. Find or create the shared Terraform state project and bucket
3. Initialize Terraform and show a plan
4. Apply it, prompting first if the plan changes or deletes existing resources (`--yes` skips the prompt)
5. Point Application Default Credentials at the new project as their quota project (skipped without ADC)
6. Configure GitHub repository variables
7. Create the Neon project and the variant databases (a variant added later gets its databases on the next run)
8. Store the Neon credentials and the Keycloak admin password in Secret Manager (the password is generated unless `KC_ADMIN_PASSWORD` is set)
9. Report any missing GitHub secrets (`GHCR_TOKEN`, `WORKFLOW_TOKEN`)

### Manual Terraform Usage

If you prefer to manage Terraform directly:

```bash
cd terraform

# Initialize (downloads providers, connects the remote state)
terraform init -backend-config="bucket=<state-project>-tfstate" -backend-config="prefix=owner/repo"

# Plan (shows what will be created)
terraform plan -var="github_repo=owner/repo" -var="billing_account_id=XXXXXX-XXXXXX-XXXXXX"

# Apply (creates resources)
terraform apply -var="github_repo=owner/repo" -var="billing_account_id=XXXXXX-XXXXXX-XXXXXX"

# Get outputs
terraform output
```

## Naming

Every name derives from the GitHub repo, or from the `SYSTEM_NAME` environment variable given to `./setup-gcp.sh`, so each system scaffolded from shop gets its own. For `optivem/shop`:

| What | Name |
|------|------|
| System name | `shop` |
| GCP project | `shop-<random 6 hex>` |
| Neon project | `shop` |
| Secrets | `shop-db`, `shop-keycloak-admin-password` |
| Databases | `shop_<arch>_<language>_<real\|stub\|qa\|production>` for every variant (`multitier`/`monolith` × `java`/`dotnet`/`typescript`, e.g. `shop_multitier_java_real`), `shop_keycloak` |

Hyphens are valid in GCP names but not in unquoted Postgres identifiers, so database names replace them with underscores. The system name is limited to 14 characters because it prefixes Cloud Run service names, which are limited to 49 (e.g. `<system>-multitier-typescript-real-frontend`). The longest names, `<system>-multitier-typescript-production-{frontend,backend}`, leave only 8 (12 for multitier-dotnet): Terraform warns at plan time and the deploy fails early with the name if it's longer. The names can't simply be shortened, because the imported Keycloak realms hold redirect URIs built from them.

## Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `region` | GCP region | No | `us-central1` |
| `github_repo` | GitHub repo in `owner/repo` format | Yes | `myorg/shop` |
| `billing_account_id` | GCP billing account ID | Yes | `01ABCD-EFGHIJ-KLMNOP` |
| `system_name` | Overrides the name derived from the repo (4-14 chars) | No | `shop` |
| `billing_kill_switch_amount` | Monthly cost (billing account currency) at which billing is unlinked; `BILLING_KILL_SWITCH_AMOUNT` for `./setup-gcp.sh` | No | `1` |

Find your billing account ID:
```bash
gcloud billing accounts list
```

## Outputs

After applying, Terraform outputs:
- `project_id` — GCP project ID
- `region` — GCP region
- `workload_identity_provider` — WIF provider resource name
- `service_account_email` — GitHub Actions service account
- `artifact_registry_repository` — Artifact Registry repository name
- `runtime_service_account_email` — Cloud Run runtime service account
- `system_name` — prefix for Cloud Run services, secrets and databases

`./setup-gcp.sh` copies these to GitHub repository variables: `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT` and `SYSTEM_NAME`.

## State

Terraform state lives in GCS, not on your machine, so setup/teardown work from any clone:

- One **state project** per Google account, labelled `purpose=terraform-state` (created on first `./setup-gcp.sh` as `tfstate-<random>`, shared by every system scaffolded from this repo). The scripts create it, not Terraform, because it holds Terraform's own state; it also gives `gcloud` a project to run the billing lookup against, so setup works on an account with no projects.
- One versioned bucket in it, `gs://<state-project>-tfstate`; each repo uses prefix `<owner>/<repo>`.
- `state-backend.sh` (sourced by both scripts) finds or creates them and runs `terraform init -backend-config=...`.

## Teardown

Remove the GCP project, the Neon project and the GitHub variables:

```bash
./teardown-gcp.sh
```

It asks you to type the GCP project ID before deleting anything, and is safe to re-run: with nothing left it prints "Nothing to tear down — already clean." The state project is kept for other systems. Use `./teardown-gcp.sh` rather than a bare `terraform destroy`, which removes only the GCP side and leaves the Neon project and the GitHub variables behind.

## Architecture

```
Google Cloud Project
├── Artifact Registry (Docker images, last 10 versions per image)
├── Workload Identity Pool (GitHub OIDC)
│   └── Service Account (github-deployer)
├── Secret Manager (<system>-db, <system>-keycloak-admin-password)
├── Service Account (runtime, Cloud Run runtime identity)
└── IAM Roles
    ├── Cloud Run Admin
    ├── Artifact Registry Writer
    ├── Secret Manager Accessor
    └── Service Account User
```

## Cost

Google Cloud bills **per resource, by usage**. Each service has its own meter (time, storage, requests, data transferred), and the charges add up on the billing account linked to the project. There is no flat fee. Many services have a monthly free tier, counted per billing account.

**In normal use the project costs $0**, and a **kill-switch** caps the worst case at about €1 a month plus a few cents. Strictly guaranteed $0 isn't possible on GCP: budget data lags usage by hours, so spending can run on a little before the kill-switch fires. The Docker Compose acceptance gates run on GitHub's runners and never touch GCP; they are the always-free path.

### What drives the cost

| Driver | When it happens | Why it stays at $0 |
|---|---|---|
| **Cloud Run** requests (vCPU-s, GiB-s, requests) | Only while a cloud stage runs tests or someone uses QA/production. Every cloud stage is manual (`workflow_dispatch`); the cloud acceptance cron is commented out, and QA/prod have none. | Request-based billing: an instance is billed only while it handles a request, and every service scales to zero (`--min-instances 0`). The free tier is 180,000 vCPU-s, 360,000 GiB-s and 2M requests a month. A cloud acceptance run keeps roughly 5–7 services (Keycloak, two stacks of system or backend+frontend, simulators, stubs) busy for the length of its suites, about 5,000–10,000 vCPU-s at 1 vCPU, so a few dozen runs a month fit. QA and production runs are only a deploy plus smoke tests. |
| **Network egress** | Responses leaving Google: pages and API responses to the test runners and browsers, and Cloud Run's queries to Neon (AWS us-east-2), which also leave Google. | The free allowance is about 1 GiB a month from North America. Database traffic is a few MB per run; UI suites loading the frontend bundle are the larger part. Past the allowance, egress costs about $0.12/GiB, i.e. cents. |
| **Image storage** | — | None: Cloud Run deploys the public GHCR images directly (`ghcr.io/<owner>/<repo>/...`, including the shared `shop-*` images), and Cloud Run imports an image once per deploy, not per instance. GitHub doesn't bill public package storage or pulls. The only Artifact Registry storage left is the kill-switch function's build image (a few MB, 0.5 GB free). Until the old `app-images` repository is removed from `main.tf` (held back until several cloud runs have deployed from GHCR), the cloud stages still copy images into it, unused, within its cleanup policy. |
| **Secret Manager** | Every deploy reads `<system>-db`. | 2 secret versions (6 free) and a few accesses per run (10,000 free). |
| **Cloud Logging** | Cloud Run request and container logs. | 50 GiB per project free. |
| **Kill-switch** (budget, Pub/Sub, Cloud Function) | A few budget notifications a day. | Budgets are free; Pub/Sub carries a few KB (10 GiB free); the function runs a few times a day (2M invocations free), and its source zip sits in a small Cloud Storage bucket (5 GB free). |
| **IAM, Workload Identity Federation, service accounts, enabled APIs, the project itself** | — | Free. |

Not on the GCP bill: **Neon Postgres** bills through the Neon account (free plan; see `neon.sh`), and **image builds** run in GitHub Actions, not Cloud Build. (The kill-switch function's one-off build does run in Cloud Build, within its free minutes.)

### Kill-switch

`billing-kill-switch.tf` creates a budget on this project at `billing_kill_switch_amount` (default 1, in the billing account's currency, so about €1 for a EUR account). Every budget update goes to a Pub/Sub topic, and the `billing-kill-switch` Cloud Function **unlinks the project from its billing account** as soon as the month's cost passes the budget. That stops everything billable in the project: Cloud Run services stop serving, and the cloud stages fail until billing is back.

- **To restore billing**, re-run `./setup-gcp.sh`: Terraform links the billing account again (it shows as a change to confirm). Raise the limit first (`BILLING_KILL_SWITCH_AMOUNT=5 ./setup-gcp.sh`), or wait for the next month, because the budget still counts this month's cost and the next notification would unlink it again.
- Google may delete resources of a project that stays without billing for a long time; `./setup-gcp.sh` recreates the GCP side.
- Keep the two **budget alerts** on the billing account (Billing > Budgets & alerts), at €5 and €20 per month, alerting at 50/90/100%. They cover every project on the account and only alert.

### Scale and limits

- **Scale to zero**: every service is deployed with `--min-instances 0`. The trade-off is a cold start on the first request (Keycloak's is the slowest).
- **Small `max-instances`**: 1–2 per service, so a traffic spike or a runaway test can't fan out.

### Stages

Every flavor has manual QA and production stages (`<flavor>-{qa,prod}-stage.yml`, thin callers of `_qa-stage.yml` / `_prod-stage.yml`). Each has its own services and database, and shares the one Keycloak through its own realm (`shop-qa`, `shop-prod`; see `docker/keycloak/Dockerfile`). `shop-prod` has no test client and no users, so add real users in the Keycloak admin console.

### What would start costing money

- Uncommenting the cloud acceptance cron (`<flavor>-acceptance-stage.yml`): hourly runs would use up the Cloud Run free tier within days, and the kill-switch would then stop the project.
- Setting `min-instances ≥ 1` on any service, or switching a service to instance-based ("CPU always allocated") billing: one always-on instance costs a few dollars a month or more.
- Heavy system-test traffic or large responses going past the egress or logging free tiers.
- More secret versions than the free 6.

Prices and free tiers change. Check the [Cloud Run pricing page](https://cloud.google.com/run/pricing) and the project's **Billing → Reports** for current figures.

## Troubleshooting

### Authentication issues
```bash
# Re-authenticate to Google Cloud
gcloud auth login

# Verify authentication
gcloud auth list
```

### Billing account not found
```bash
# List all billing accounts
gcloud billing accounts list
```

If none is listed, set one up at <https://console.cloud.google.com/billing> and re-run `./setup-gcp.sh`.

### State corruption
If Terraform state gets corrupted, refresh:
```bash
terraform refresh
```

Or restore an earlier state: the bucket is versioned, so `gcloud storage ls -a gs://<state-project>-tfstate/<owner>/<repo>/` lists previous versions to copy back. Don't delete the remote state to "start fresh" — Terraform would then create a second project.

## See Also

- [Terraform Google Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
- [GCP IAM Roles](https://cloud.google.com/iam/docs/understanding-roles)
- [Workload Identity Federation](https://cloud.google.com/docs/authentication/workload-identity-federation)
