output "project_id" {
  description = "GCP Project ID"
  value       = google_project.this.project_id
}

output "region" {
  description = "GCP Region"
  value       = var.region
}

output "workload_identity_provider" {
  description = "Workload Identity Provider resource name"
  value       = google_iam_workload_identity_pool_provider.github.name
}

output "service_account_email" {
  description = "GitHub Actions service account email"
  value       = google_service_account.github_deployer.email
}

output "artifact_registry_repository" {
  description = "Artifact Registry repository name"
  value       = google_artifact_registry_repository.app_images.name
}

output "runtime_service_account_email" {
  description = "Cloud Run runtime service account email"
  value       = google_service_account.runtime.email
}

output "system_name" {
  description = "Prefix for Cloud Run services, secrets and databases"
  value       = local.system_name
}
