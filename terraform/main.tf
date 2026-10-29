# GCP Project
resource "google_project" "my_shop" {
  name            = "my-company-my-shop"
  project_id      = "my-company-my-shop-${random_id.project_suffix.hex}"
  auto_create_network = false
}

resource "random_id" "project_suffix" {
  byte_length = 3
}

# Link billing account
resource "google_billing_project_info" "my_shop" {
  billing_account_id = var.billing_account_id
  project_id         = google_project.my_shop.project_id
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

  project            = google_project.my_shop.project_id
  service            = each.value
  disable_on_destroy = false
}

# Artifact Registry repository
resource "google_artifact_registry_repository" "app_images" {
  project      = google_project.my_shop.project_id
  location     = var.region
  repository_id = "app-images"
  format       = "DOCKER"

  depends_on = [google_project_service.required_apis]
}

# Workload Identity Pool
resource "google_iam_workload_identity_pool" "github_actions" {
  project                   = google_project.my_shop.project_id
  workload_identity_pool_id = "github-actions"
  location                  = "global"
  display_name              = "GitHub Actions"

  depends_on = [google_project_service.required_apis]
}

# OIDC Provider
resource "google_iam_workload_identity_pool_provider" "github" {
  project                            = google_project.my_shop.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.github_actions.workload_identity_pool_id
  workload_identity_pool_provider_id = "github"
  location                           = "global"
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
  project      = google_project.my_shop.project_id
  account_id   = "github-deployer"
  display_name = "GitHub Actions Deployer"

  depends_on = [google_project_service.required_apis]
}

# IAM Binding: WIF to SA
resource "google_service_account_iam_member" "github_wif_workload_identity" {
  service_account_id = google_service_account.github_deployer.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/projects/${google_project.my_shop.number}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.github_actions.workload_identity_pool_id}/attribute.repository/${var.github_repo}"
}

# IAM Bindings: Project roles
locals {
  github_wif_principal = "principalSet://iam.googleapis.com/projects/${google_project.my_shop.number}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.github_actions.workload_identity_pool_id}/attribute.repository/${var.github_repo}"
}

resource "google_project_iam_member" "github_run_admin" {
  project = google_project.my_shop.project_id
  role    = "roles/run.admin"
  member  = local.github_wif_principal
}

resource "google_project_iam_member" "github_sa_user" {
  project = google_project.my_shop.project_id
  role    = "roles/iam.serviceAccountUser"
  member  = local.github_wif_principal
}

resource "google_project_iam_member" "github_artifact_registry_writer" {
  project = google_project.my_shop.project_id
  role    = "roles/artifactregistry.writer"
  member  = local.github_wif_principal
}

resource "google_project_iam_member" "github_secret_accessor" {
  project = google_project.my_shop.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = local.github_wif_principal
}

# Secret Manager: Database connection
resource "google_secret_manager_secret" "db_connection_string" {
  project   = google_project.my_shop.project_id
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
resource "google_service_account" "shop_runtime" {
  project      = google_project.my_shop.project_id
  account_id   = "shop-runtime"
  display_name = "Shop Cloud Run runtime"

  depends_on = [google_project_service.required_apis]
}

# Secret versions are added out of band (gcloud secrets versions add) so values stay out of state
resource "google_secret_manager_secret" "shop_runtime" {
  for_each  = toset(["shop-mt-java-acceptance-db", "keycloak-db", "keycloak-admin-password"])
  project   = google_project.my_shop.project_id
  secret_id = each.value

  replication {
    auto {}
  }

  depends_on = [google_project_service.required_apis]
}

resource "google_secret_manager_secret_iam_member" "shop_runtime_accessor" {
  for_each  = google_secret_manager_secret.shop_runtime
  project   = google_project.my_shop.project_id
  secret_id = each.value.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.shop_runtime.email}"
}

# Deployer may deploy services that run as the runtime SA
resource "google_service_account_iam_member" "deployer_acts_as_runtime" {
  service_account_id = google_service_account.shop_runtime.name
  role               = "roles/iam.serviceAccountUser"
  member             = local.github_wif_principal
}
