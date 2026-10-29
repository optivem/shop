# Deploy `shop` for real — Cloud Run + Neon

> 🤖 **Picked up by agent** — `ValentinaLaptop` at `2026-10-29T12:31:52Z`

## TL;DR

**Why:** The shop only runs locally (`gh optivem system start`). The existing `multitier-java-*-stage-cloud.yml` workflows cannot work: nothing is provisioned and the deploy steps don't match what the app needs (ports, DB vars, Keycloak, simulator URLs, migrations).
**End result:** `multitier/java` (React frontend + Java backend + simulators + Keycloak) runs on GCP Cloud Run with Neon Postgres, deployed by the manual-dispatch acceptance-cloud workflow, tested by the **TypeScript** system tests, at ~$0 when idle.

## Decisions (all resolved)

1. **Surface:** `multitier/java` only, system tests in **TypeScript**. Other variants later, by templating.
2. **Backend exposure:** public, restricted by CORS (`ALLOWED_ORIGINS`).
3. **DB:** Neon, one project; one database per stack (`shop_mt_java_real`, `shop_mt_java_stub`) plus one for Keycloak (`keycloak`). Neon is free-tier; Cloud SQL is rejected (paid, no free tier).
4. **Region:** `us-central1`.
5. **CI trigger:** manual dispatch for the cloud acceptance/QA/prod stages. The hourly cron stays commented out.
6. **Images to Google:** the workflow **copies** the tested GHCR image (same digest) into Artifact Registry `app-images` (keyless via WIF, no PAT). Cloud Run deploys from there. Applies to our images and to the Keycloak image (quay.io).
7. **Keycloak:** Keycloak 26 on Cloud Run (scale-to-zero) with its own Neon database, realm imported from `docker/keycloak/shop-realm.json` with the frontend URL substituted in. Smoke tests wait longer for its cold start.
8. **Who provisions:** the user runs `setup-gcp.sh`, creates Neon, and sets GitHub environment variables. The agent writes code/config, then pauses and hands over exact commands, then dispatches and runs the first cloud acceptance workflow.
9. **Secrets:** DB credentials as **one JSON secret per variant** (Secret Manager bills per version; 6 free). Never in Terraform state: the user adds versions with `gcloud secrets versions add`.

## What's already in place (don't rebuild)

- **Terraform** — project, Artifact Registry `app-images`, secret `db-connection-string`, WIF pool + `github-deployer` roles (`run.admin`, `artifactregistry.writer`, `secretmanager.secretAccessor`, `iam.serviceAccountUser`). `setup-gcp.sh` / `teardown-gcp.sh` wrap it.
- **Images** — the commit stage already publishes `multitier-frontend-react`, `multitier-backend-java` (and the simulator/stub images) to GHCR.
- **Migrations** — `system/db/migrations` (Flyway).
- **Frontend** reads `BACKEND_API_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` at **runtime** (nginx envsubst) — no rebuild per environment. Listens on 3000.
- **Backend** listens on **8081**; needs `POSTGRES_DB_HOST/PORT/NAME/USER/PASSWORD`, `ERP/CLOCK/TAX_API_URL`, `EXTERNAL_SYSTEM_MODE`, `ALLOWED_ORIGINS`, `AUTH_ISSUER_URI`, `AUTH_JWK_SET_URI`, `AUTH_AUDIENCE=shop-backend`. There is no `cloud` Spring profile.
- **TypeScript system tests** read `ENVIRONMENT`, `EXTERNAL_SYSTEM_MODE`, `MYSHOP_UI_BASE_URL_<MODE>`, `MYSHOP_API_BASE_URL_<MODE>`, `ERP/CLOCK/TAX_API_BASE_URL_<MODE>`, `KEYCLOAK_URL(_<MODE>)`; config files `system-test/typescript/config/test-config-<env>-<mode>.json` (only `local` exists besides `acceptance`).

## ▶ Next executable step (resume here)

**Step 7 — USER STEP (provision)**: commands already handed over (see Step 7 below for the ordered checklist); user is running them in the devcontainer. Wait for "provisioned", then Step 8 (first run) after the user confirms. Steps 5–6 are done: `multitier-java-acceptance-stage-cloud.yml` is rewritten (actionlint clean, not yet run) and cost guardrails are in `terraform/README.md`. Step 1 (Terraform) is written but unvalidated — `terraform` is not installed here; `setup-gcp.sh` will validate it. `db-connection-string` is kept for the other variants. The workflow expects Secret Manager secrets `shop-mt-java-acceptance-db`, `keycloak-db` (JSON `{host,port,user,password}`) and `keycloak-admin-password`, and service account `shop-runtime@<project>`.

## Steps

### 7. USER STEP — provision (agent pauses here)
**Status:** commands already handed over; user is reopening in the devcontainer (has gcloud/terraform/gh pinned — no Windows installs needed) to run them. On resume: do NOT re-explain from scratch; ask "provisioned?" and continue at Step 8 once confirmed. If the user is stuck, help from their pasted error.

Order for the user (all in the devcontainer terminal, repo root):
1. Verify `gcloud --version && terraform --version && gh --version`; log in (`gcloud auth login`, `gcloud auth application-default login` — add `--no-launch-browser` if needed — and `gh auth login`; host logins don't carry over). Needs a GCP billing account.
2. `./setup-gcp.sh` (first real validation of the unvalidated Terraform; it creates the secrets `shop-mt-java-acceptance-db`, `keycloak-db`, `keycloak-admin-password` but not their versions). Note the printed project ID, region, WIF provider, deployer SA (`terraform output` in `terraform/` to re-show).
3. Neon: one project, databases `shop_mt_java_real`, `shop_mt_java_stub`, `keycloak`.
4. `echo -n '{"host":"<h>","port":"5432","user":"<u>","password":"<p>"}' | gcloud secrets versions add shop-mt-java-acceptance-db --data-file=-` (same for `keycloak-db`); `echo -n '<pw>' | gcloud secrets versions add keycloak-admin-password --data-file=-`.
5. `gh variable set GCP_PROJECT_ID|GCP_REGION(us-central1)|GCP_WORKLOAD_IDENTITY_PROVIDER|GCP_SERVICE_ACCOUNT --env multitier-java-acceptance --body "<value>"`; ensure the environment and the `GHCR_TOKEN` / `WORKFLOW_TOKEN` secrets exist.
6. Tell the agent "provisioned".

Original summary — agent gives exact commands for: `./setup-gcp.sh`; creating the Neon project, databases `shop_mt_java_real`, `shop_mt_java_stub` and `keycloak`; `gcloud secrets versions add` for the three secrets; setting `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT` on the `multitier-java-acceptance` GitHub environment.

### 8. First run
After the user confirms Step 7: dispatch `multitier-java-acceptance-stage-cloud`, hand over the run ID (do not poll), fix failures within scope.

### 9. Follow-ups (not in this plan)
Template to other variants; QA/prod cloud stages for multitier-java; the cloud realm exposes a password-grant test client and test users — acceptable for a demo, revisit before anything real.
