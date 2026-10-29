# GCP Project
# Named after the GitHub repo (or var.system_name), so scaffolded systems each get their own project.
# Max 14 chars: it prefixes Cloud Run service names (49-char limit), e.g. <system>-multitier-typescript-real-frontend
locals {
  system_name = var.system_name != "" ? var.system_name : trim(substr(replace(lower(element(split("/", var.github_repo), 1)), "/[^a-z0-9-]/", "-"), 0, 14), "-")
}

resource "google_project" "this" {
  name       = local.system_name
  project_id = "${local.system_name}-${random_id.project_suffix.hex}"
  # Set on the project itself so billing is linked before any API is enabled (Cloud Run requires it)
  billing_account     = var.billing_account_id
  auto_create_network = false

  # Google places the project under the creator's organization/folder (e.g. a Workspace domain);
  # without this, the next plan would try to move it out of that org (org_id -> null)
  lifecycle {
    ignore_changes = [org_id, folder_id]
  }
}

resource "random_id" "project_suffix" {
  byte_length = 3
}

# Enable required APIs
resource "google_project_service" "required_apis" {
  for_each = toset([
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "secretmanager.googleapis.com",
    "iamcredentials.googleapis.com",
    "iam.googleapis.com",
    "cloudresourcemanager.googleapis.com",
  ])

  project            = google_project.this.project_id
  service            = each.value
  disable_on_destroy = false
}

# Artifact Registry repository
resource "google_artifact_registry_repository" "app_images" {
  project       = google_project.this.project_id
  location      = var.region
  repository_id = "app-images"
  format        = "DOCKER"

  depends_on = [google_project_service.required_apis]
}

# Workload Identity Pool
resource "google_iam_workload_identity_pool" "github_actions" {
  project                   = google_project.this.project_id
  workload_identity_pool_id = "github-actions"
  display_name              = "GitHub Actions"

  depends_on = [google_project_service.required_apis]
}

# OIDC Provider
resource "google_iam_workload_identity_pool_provider" "github" {
  project                            = google_project.this.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.github_actions.workload_identity_pool_id
  workload_identity_pool_provider_id = "github"
  display_name                       = "GitHub"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
  }

  attribute_condition = "assertion.repository == '${var.github_repo}'"

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

# Service Account for GitHub Actions
resource "google_service_account" "github_deployer" {
  project      = google_project.this.project_id
  account_id   = "github-deployer"
  display_name = "GitHub Actions Deployer"

  depends_on = [google_project_service.required_apis]
}

# IAM Binding: WIF to SA
resource "google_service_account_iam_member" "github_wif_workload_identity" {
  service_account_id = google_service_account.github_deployer.name
  role               = "roles/iam.workloadIdentityUser"
  member             = local.github_wif_principal
}

# IAM Bindings: Project roles
locals {
  # The workflows impersonate github-deployer (auth action's service_account input), so the deploy roles
  # belong to that service account; the WIF principal only gets workloadIdentityUser on it
  github_deployer_member = "serviceAccount:${google_service_account.github_deployer.email}"
  github_wif_principal   = "principalSet://iam.googleapis.com/projects/${google_project.this.number}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.github_actions.workload_identity_pool_id}/attribute.repository/${var.github_repo}"
}

resource "google_project_iam_member" "github_run_admin" {
  project = google_project.this.project_id
  role    = "roles/run.admin"
  member  = local.github_deployer_member
}

resource "google_project_iam_member" "github_sa_user" {
  project = google_project.this.project_id
  role    = "roles/iam.serviceAccountUser"
  member  = local.github_deployer_member
}

resource "google_project_iam_member" "github_artifact_registry_writer" {
  project = google_project.this.project_id
  role    = "roles/artifactregistry.writer"
  member  = local.github_deployer_member
}

resource "google_project_iam_member" "github_secret_accessor" {
  project = google_project.this.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = local.github_deployer_member
}

# Secret Manager: Database connection
resource "google_secret_manager_secret" "db_connection_string" {
  project   = google_project.this.project_id
  secret_id = "db-connection-string"

  replication {
    auto {}
  }

  depends_on = [google_project_service.required_apis]
}

resource "google_secret_manager_secret_version" "db_connection_string" {
  count       = var.neon_database_url != "" ? 1 : 0
  secret      = google_secret_manager_secret.db_connection_string.id
  secret_data = var.neon_database_url
}

# Runtime identity for the Cloud Run services (separate from the deployer)
resource "google_service_account" "runtime" {
  project      = google_project.this.project_id
  account_id   = "runtime"
  display_name = "Cloud Run runtime"

  depends_on = [google_project_service.required_apis]
}

# Secret versions are added out of band (gcloud secrets versions add) so values stay out of state
resource "google_secret_manager_secret" "runtime" {
  for_each  = toset([for s in ["multitier-java-acceptance-db", "keycloak-db", "keycloak-admin-password"] : "${local.system_name}-${s}"])
  project   = google_project.this.project_id
  secret_id = each.value

  replication {
    auto {}
  }

  depends_on = [google_project_service.required_apis]
}

resource "google_secret_manager_secret_iam_member" "runtime_accessor" {
  for_each  = google_secret_manager_secret.runtime
  project   = google_project.this.project_id
  secret_id = each.value.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.runtime.email}"
}

# Deployer may deploy services that run as the runtime SA
resource "google_service_account_iam_member" "deployer_acts_as_runtime" {
  service_account_id = google_service_account.runtime.name
  role               = "roles/iam.serviceAccountUser"
  member             = local.github_deployer_member
}
