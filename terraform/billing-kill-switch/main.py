"""Billing kill-switch: unlinks the project from its billing account once the month's cost
passes the budget, which stops every billable service in it.

Triggered by the budget's Pub/Sub notifications (see ../billing-kill-switch.tf), following
Google's "Disable billing usage with notifications" example. Budget data lags actual usage
by hours, so the stop is not exact: expect a few cents over the budget in the worst case.
To restore billing, re-run ./setup-gcp.sh with a higher BILLING_KILL_SWITCH_AMOUNT: it raises
the budget and links billing again before Terraform runs (../kill-switch.sh).
"""

import base64
import json
import os
from datetime import datetime, timedelta, timezone

import functions_framework
from google.cloud import billing_v1

PROJECT_NAME = f"projects/{os.environ['TARGET_PROJECT_ID']}"


# Failed deliveries are retried (RETRY_POLICY_RETRY) and keep their original publish time and the
# budget amount of that moment. After the limit is raised, such a retry would still unlink billing
# with the old limit (seen 2026-11-18), so notices older than this are dropped; a fresh one follows
# within minutes.
MAX_NOTICE_AGE = timedelta(hours=1)


@functions_framework.cloud_event
def stop_billing(cloud_event):
    published = datetime.fromisoformat(cloud_event["time"].replace("Z", "+00:00"))
    age = datetime.now(timezone.utc) - published
    if age > MAX_NOTICE_AGE:
        print(f"No action: notice published {published.isoformat()} is {age} old (stale retry).")
        return

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
