# Billing kill-switch: a budget on this project publishes to Pub/Sub, and a Cloud Function unlinks
# the project from its billing account once the month's cost passes var.billing_kill_switch_amount
# (in the billing account's currency, about €1 by default). That stops everything billable here.
# Budget data lags usage by hours, so the worst case is a few cents over. Re-running setup-gcp.sh
# links billing again; raise the amount first, or the next notification that month disables it again.
# The €5/€20 budget alerts on the billing account stay as they are (README: Cost).
#
# Free tier: a few notifications a day trigger the function (Cloud Run functions: 2M invocations
# free), the topic carries a few KB (Pub/Sub: 10 GiB free), and the source zip and the function's
# build image are a few MB each (Cloud Storage 5 GB, Artifact Registry 0.5 GB free).

resource "google_pubsub_topic" "billing_kill_switch" {
  project = google_project.this.project_id
  name    = "billing-kill-switch"

  depends_on = [google_project_service.required_apis]
}

resource "google_billing_budget" "kill_switch" {
  billing_account = var.billing_account_id
  display_name    = "${local.system_name} kill-switch"

  budget_filter {
    projects = ["projects/${google_project.this.number}"]
  }

  # No currency_code: the amount is in the billing account's currency.
  amount {
    specified_amount {
      units = tostring(var.billing_kill_switch_amount)
    }
  }

  threshold_rules {
    threshold_percent = 1.0
  }

  all_updates_rule {
    pubsub_topic = google_pubsub_topic.billing_kill_switch.id
  }

  depends_on = [google_project_service.required_apis]
}

# The function's identity: may unlink this project's billing, nothing else.
resource "google_service_account" "billing_kill_switch" {
  project      = google_project.this.project_id
  account_id   = "billing-kill-switch"
  display_name = "Billing kill-switch function"

  depends_on = [google_project_service.required_apis]
}

resource "google_project_iam_member" "billing_kill_switch_project_billing_manager" {
  project = google_project.this.project_id
  role    = "roles/billing.projectManager"
  member  = "serviceAccount:${google_service_account.billing_kill_switch.email}"
}

# Eventarc delivers the Pub/Sub messages as this identity, so it must be able to invoke the function.
resource "google_project_iam_member" "billing_kill_switch_invoker" {
  project = google_project.this.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.billing_kill_switch.email}"
}

# Builds the function with its own identity, not the Compute Engine default service account,
# which doesn't exist here (the Compute API is never enabled).
resource "google_service_account" "billing_kill_switch_build" {
  project      = google_project.this.project_id
  account_id   = "billing-kill-switch-build"
  display_name = "Billing kill-switch function build"

  depends_on = [google_project_service.required_apis]
}

resource "google_project_iam_member" "billing_kill_switch_build" {
  for_each = toset([
    "roles/cloudbuild.builds.builder",
    "roles/logging.logWriter",
    "roles/artifactregistry.writer",
    "roles/storage.objectViewer",
  ])
  project = google_project.this.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.billing_kill_switch_build.email}"
}

resource "google_storage_bucket" "functions_source" {
  project                     = google_project.this.project_id
  name                        = "${google_project.this.project_id}-functions-source"
  location                    = var.region
  uniform_bucket_level_access = true
  force_destroy               = true

  depends_on = [google_project_service.required_apis]
}

data "archive_file" "billing_kill_switch" {
  type        = "zip"
  source_dir  = "${path.module}/billing-kill-switch"
  output_path = "${path.module}/.build/billing-kill-switch.zip"
}

resource "google_storage_bucket_object" "billing_kill_switch" {
  # The hash in the name makes a source change redeploy the function.
  name   = "billing-kill-switch-${data.archive_file.billing_kill_switch.output_md5}.zip"
  bucket = google_storage_bucket.functions_source.name
  source = data.archive_file.billing_kill_switch.output_path
}

resource "google_cloudfunctions2_function" "billing_kill_switch" {
  project  = google_project.this.project_id
  name     = "billing-kill-switch"
  location = var.region

  build_config {
    runtime         = "python312"
    entry_point     = "stop_billing"
    service_account = google_service_account.billing_kill_switch_build.id
    source {
      storage_source {
        bucket = google_storage_bucket.functions_source.name
        object = google_storage_bucket_object.billing_kill_switch.name
      }
    }
  }

  service_config {
    max_instance_count    = 1
    available_memory      = "256M"
    timeout_seconds       = 60
    service_account_email = google_service_account.billing_kill_switch.email
    environment_variables = {
      TARGET_PROJECT_ID = google_project.this.project_id
    }
  }

  event_trigger {
    trigger_region        = var.region
    event_type            = "google.cloud.pubsub.topic.v1.messagePublished"
    pubsub_topic          = google_pubsub_topic.billing_kill_switch.id
    retry_policy          = "RETRY_POLICY_RETRY"
    service_account_email = google_service_account.billing_kill_switch.email
  }

  depends_on = [
    google_project_iam_member.billing_kill_switch_build,
    google_project_iam_member.billing_kill_switch_invoker,
  ]
}
