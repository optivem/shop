terraform {
  required_version = ">= 1.0"

  # Bucket/prefix supplied at init by state-backend.sh (shared state project, prefix = owner/repo)
  backend "gcs" {}

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

# No default project: it is created here (a provider can't depend on a resource); every resource sets project explicitly
provider "google" {
  region = var.region
}

provider "google-beta" {
  region = var.region
}
