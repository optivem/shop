variable "region" {
  description = "GCP region for resources"
  type        = string
  default     = "us-central1"
}

variable "github_repo" {
  description = "GitHub repository in format 'owner/repo'"
  type        = string
}

variable "system_name" {
  description = "Name for the GCP project (project ID gets a random suffix). Empty = derived from github_repo"
  type        = string
  default     = ""

  validation {
    condition     = var.system_name == "" || can(regex("^[a-z][a-z0-9-]{2,12}[a-z0-9]$", var.system_name))
    error_message = "system_name must be 4-14 chars: lowercase letters, digits, hyphens; start with a letter."
  }
}

variable "billing_account_id" {
  description = "GCP billing account ID"
  type        = string
}

variable "billing_kill_switch_amount" {
  description = "Monthly cost (in the billing account's currency) at which the kill-switch unlinks the project from billing"
  type        = number
  default     = 1

  validation {
    condition     = var.billing_kill_switch_amount >= 1 && floor(var.billing_kill_switch_amount) == var.billing_kill_switch_amount
    error_message = "billing_kill_switch_amount must be a whole number of at least 1."
  }
}
