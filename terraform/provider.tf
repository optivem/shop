terraform {
  required_version = ">= 1.0"
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
