"""Billing kill-switch: unlinks the project from its billing account once the month's cost
passes the budget, which stops every billable service in it.

Triggered by the budget's Pub/Sub notifications (see ../billing-kill-switch.tf), following
Google's "Disable billing usage with notifications" example. Budget data lags actual usage
by hours, so the stop is not exact: expect a few cents over the budget in the worst case.
To restore billing, re-run ./setup-gcp.sh (Terraform links the billing account again).
"""

import base64
import json
import os

import functions_framework
from google.cloud import billing_v1

PROJECT_NAME = f"projects/{os.environ['TARGET_PROJECT_ID']}"


@functions_framework.cloud_event
def stop_billing(cloud_event):
    data = json.loads(base64.b64decode(cloud_event.data["message"]["data"]).decode("utf-8"))
    cost = data["costAmount"]
    budget = data["budgetAmount"]
    if cost <= budget:
        print(f"No action: cost {cost} is within the budget {budget}.")
        return

    client = billing_v1.CloudBillingClient()
    if not client.get_project_billing_info(name=PROJECT_NAME).billing_enabled:
        print(f"Billing is already disabled for {PROJECT_NAME}.")
        return

    client.update_project_billing_info(
        name=PROJECT_NAME,
        project_billing_info=billing_v1.ProjectBillingInfo(billing_account_name=""),
    )
    print(f"Billing disabled for {PROJECT_NAME}: cost {cost} is over the budget {budget}.")
