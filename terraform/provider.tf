terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = google_project.this.project_id
  region  = var.region
}

provider "google-beta" {
  project = google_project.this.project_id
  region  = var.region
}
