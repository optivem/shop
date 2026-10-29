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
| `neon_database_url` | Optional Neon connection string | No | `postgres://...` |

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
├── Secret Manager (db-connection-string, shop-mt-java-acceptance-db, keycloak-db, keycloak-admin-password)
├── Service Account (shop-runtime, Cloud Run runtime identity)
└── IAM Roles
    ├── Cloud Run Admin
    ├── Artifact Registry Writer
    ├── Secret Manager Accessor
    └── Service Account User
```

## Cost

- **Free tier eligible**: Most resources are free or have generous free quotas
- **Artifact Registry**: 0.5 GB free storage per month
- **Secret Manager**: $0.06/month per active secret version per location; first 6 versions free per billing account (1 secret ≈ $0)
- **Cloud Run**: Free tier includes 180,000 GB-seconds/month

**Total estimated cost**: ~$0/month at demo scale

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

Or start fresh:
```bash
rm -rf .terraform terraform.tfstate* .terraform.lock.hcl
terraform init
```

## See Also

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
- [GCP IAM Roles](https://cloud.google.com/iam/docs/understanding-roles)
- [Workload Identity Federation](https://cloud.google.com/docs/authentication/workload-identity-federation)
