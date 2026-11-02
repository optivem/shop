# GCP Infrastructure with Terraform

This directory contains Terraform configuration for setting up a complete GCP infrastructure for the shop project, including:

- **GCP Project** with automatic naming
- **Artifact Registry** for Docker images
- **Workload Identity Federation** for GitHub Actions authentication
- **Secret Manager** for database connections
- **IAM bindings** for GitHub Actions deployment

## Quick Start

### Prerequisites

- `terraform` CLI (v1.0+) — [Install](https://www.terraform.io/downloads)
- `gcloud` CLI — [Install](https://cloud.google.com/sdk/docs/install)
- `gh` CLI — [Install](https://cli.github.com)
- GCP account with active billing

### Setup (Automated)

From the repo root:

```bash
# Step 1: Authenticate
gh auth login
gcloud auth login

# Step 2: Run setup script
./setup-gcp.sh
```

The script will:
1. Check prerequisites
2. Initialize Terraform
3. Show a plan of resources to create
4. Prompt for confirmation
5. Apply the configuration
6. Configure GitHub repository variables
7. Create a local `deploy-config.yml`

### Manual Terraform Usage

If you prefer to manage Terraform directly:

```bash
cd terraform

# Initialize (downloads providers)
terraform init

# Plan (shows what will be created)
terraform plan -var="github_repo=owner/repo" -var="billing_account_id=XXXXXX-XXXXXX-XXXXXX"

# Apply (creates resources)
terraform apply -var="github_repo=owner/repo" -var="billing_account_id=XXXXXX-XXXXXX-XXXXXX"

# Get outputs
terraform output

# Destroy (when done)
terraform destroy
```

## Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `region` | GCP region | No | `us-central1` |
| `github_repo` | GitHub repo in `owner/repo` format | Yes | `myorg/shop` |
| `billing_account_id` | GCP billing account ID | Yes | `01ABCD-EFGHIJ-KLMNOP` |

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

These are automatically configured in GitHub repository variables.

## State

Terraform state lives in GCS, not on your machine, so setup/teardown work from any clone:

- One **state project** per Google account, labelled `purpose=terraform-state` (created on first `./setup-gcp.sh` as `tfstate-<random>`, shared by every system scaffolded from this repo). The scripts create it, not Terraform, because it holds Terraform's own state; it also gives `gcloud` a project to run the billing lookup against, so setup works on an account with no projects.
- One versioned bucket in it, `gs://<state-project>-tfstate`; each repo uses prefix `<owner>/<repo>`.
- `state-backend.sh` (sourced by both scripts) finds or creates them and runs `terraform init -backend-config=...`.

Manual init: `terraform init -backend-config="bucket=<state-project>-tfstate" -backend-config="prefix=owner/repo"`.

## Teardown

Remove all GCP resources:

```bash
# Automated teardown
./teardown-gcp.sh

# Or manual
cd terraform
terraform destroy -var="github_repo=owner/repo" -var="billing_account_id=XXXXXX-XXXXXX-XXXXXX"
```

## Architecture

```
Google Cloud Project
├── Artifact Registry (Docker images)
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

**Total estimated cost**: ~$0/month at demo scale.

### What is billed

| Service | What you pay for | Free tier (per month) |
|---|---|---|
| **Cloud Run** (request-based billing, the default) | vCPU-seconds and GiB-seconds, but **only while an instance is handling a request** (rounded up to 100 ms), plus a per-request fee | 180,000 vCPU-seconds, 360,000 GiB-seconds, 2M requests |
| **Cloud Run** (instance-based billing, or `min-instances > 0`) | The instance's **whole lifetime**, even when idle | Same pool as above |
| **Artifact Registry** | GB stored per month (~$0.10/GB after the free amount) | 0.5 GB |
| **Secret Manager** | ~$0.06 per active secret version per location, plus access operations (~$0.03 per 10,000) | 6 versions, 10,000 accesses |
| **Network egress** | Data leaving Google to the internet (responses to browsers and test runners); traffic coming in is free | Small allowance (~1 GiB from North America regions) |
| **Cloud Logging** | Log data ingested above the free amount | 50 GiB per project |
| **IAM, Workload Identity Federation, service accounts, enabled APIs, the project itself** | Free | — |

### Not on the GCP bill

- **Neon Postgres** bills separately through the Neon account (see `neon.sh`).
- **Image builds** run in GitHub Actions, not Cloud Build, so there is no Cloud Build charge.

### What keeps idle cost at ~$0

- **Scale to zero**: the shared cloud stages (`_acceptance-stage-cloud.yml`, `_deploy-stage-cloud.yml`) deploy every service with `--min-instances 0`; the per-variant QA/prod stage workflows don't pass the flag and rely on Cloud Run's default of 0. You pay nothing between requests; the trade-off is a cold start on the first request (Keycloak's is the slowest).
- **Small `max-instances`**: 1–2 in the shared cloud stages, 3 in the per-variant QA/prod stages. This caps the cost of a traffic spike or a runaway test.
- **Image cleanup**: the `app-images` repository's cleanup policy (`main.tf`) keeps the 10 newest versions of each image plus every `production-*` tag, and deletes the rest once they are a day old. With many images this can still go past the 0.5 GB free tier, but at ~$0.10/GB the overage is cents.
- **Few secret versions**: one shared `<system>-db` JSON secret plus `<system>-keycloak-admin-password`, 2 versions in total; only 6 are free.
- **Budget alerts**: create two on the billing account (Billing > Budgets & alerts), at €5 and €20 per month, alerting at 50/90/100%. Budgets **only alert; they do not stop spending**.
- **Reset**: `./teardown-gcp.sh` destroys everything; `./setup-gcp.sh` recreates it.

### Stages

multitier-java has manual QA and production stages (`multitier-java-{qa,prod}-stage-cloud.yml`). Each has its own services and database, and shares the one Keycloak through its own realm (`shop-qa`, `shop-prod`; see `docker/keycloak/Dockerfile.cloud`). `shop-prod` has no test client and no users, so add real users in the Keycloak admin console.

### What would start costing money

- Setting `min-instances ≥ 1` on any service: one always-on instance costs a few dollars a month or more.
- Switching a service to instance-based ("CPU always allocated") billing.
- Images piling up if the cleanup policy stops working.
- More secret versions than the free 6.
- Heavy system-test traffic or large responses going past the egress or logging free tiers.

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

# Set as default
gcloud config set billing/quota_project YOUR_BILLING_ACCOUNT_ID
```

### State corruption
If Terraform state gets corrupted, refresh:
```bash
terraform refresh
```

Or restore an earlier state: the bucket is versioned, so `gcloud storage ls -a gs://<state-project>-tfstate/<owner>/<repo>/` lists previous versions to copy back. Don't delete the remote state to "start fresh" — Terraform would then create a second project.

## See Also

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
- [GCP IAM Roles](https://cloud.google.com/iam/docs/understanding-roles)
- [Workload Identity Federation](https://cloud.google.com/docs/authentication/workload-identity-federation)
