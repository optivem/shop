# Deploy `shop` for real — Cloud Run + Neon

> 🤖 **Picked up by agent** — `b2d83c7ed0bc` at `2026-10-29T20:03:26Z`

## TL;DR

**Why:** The shop only runs locally (`gh optivem system start`). The existing `multitier-java-*-stage-cloud.yml` workflows cannot work: nothing is provisioned and the deploy steps don't match what the app needs (ports, DB vars, Keycloak, simulator URLs, migrations).
**End result:** `multitier/java` (React frontend + Java backend + simulators + Keycloak) runs on GCP Cloud Run with Neon Postgres, deployed by the manual-dispatch acceptance-cloud workflow, tested by the **TypeScript** system tests, at ~$0 when idle.

## Decisions (all resolved)

1. **Surface:** `multitier/java` only, system tests in **TypeScript**. Other variants later, by templating.
2. **Backend exposure:** public, restricted by CORS (`ALLOWED_ORIGINS`).
3. **DB:** Neon, one project; one database per stack (`<system>_multitier_java_real`, `<system>_multitier_java_stub`) plus one for Keycloak (`<system>_keycloak`); `<system>` is derived from the repo name (see resume block). Neon is free-tier; Cloud SQL is rejected (paid, no free tier).
4. **Region:** `us-central1`.
5. **CI trigger:** manual dispatch for the cloud acceptance/QA/prod stages. The hourly cron stays commented out.
6. **Images to Google:** the workflow **copies** the tested GHCR image (same digest) into Artifact Registry `app-images` (keyless via WIF, no PAT). Cloud Run deploys from there. Applies to our images and to the Keycloak image (quay.io).
7. **Keycloak:** Keycloak 26 on Cloud Run (scale-to-zero) with its own Neon database, realm imported from `docker/keycloak/shop-realm.json` with the frontend URL substituted in. Smoke tests wait longer for its cold start.
8. **Who provisions:** the user runs `setup-gcp.sh`, which provisions GCP (Terraform) and Neon (`neonctl`, user-requested 2026-10-29 instead of console clicks) and sets the GitHub variables. The agent writes code/config, then pauses and hands over exact commands, then dispatches and runs the first cloud acceptance workflow.
9. **Secrets:** DB credentials as **one JSON secret per variant** (Secret Manager bills per version; 6 free). Never in Terraform state: the user adds versions with `gcloud secrets versions add`.

## What's already in place (don't rebuild)

- **Terraform** — project, Artifact Registry `app-images`, secret `db-connection-string`, WIF pool + `github-deployer` roles (`run.admin`, `artifactregistry.writer`, `secretmanager.secretAccessor`, `iam.serviceAccountUser`). `setup-gcp.sh` / `teardown-gcp.sh` wrap it.
- **Images** — the commit stage already publishes `multitier-frontend-react`, `multitier-backend-java` (and the simulator/stub images) to GHCR.
- **Migrations** — `system/db/migrations` (Flyway).
- **Frontend** reads `BACKEND_API_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` at **runtime** (nginx envsubst) — no rebuild per environment. Listens on 3000.
- **Backend** listens on **8081**; needs `POSTGRES_DB_HOST/PORT/NAME/USER/PASSWORD`, `ERP/CLOCK/TAX_API_URL`, `EXTERNAL_SYSTEM_MODE`, `ALLOWED_ORIGINS`, `AUTH_ISSUER_URI`, `AUTH_JWK_SET_URI`, `AUTH_AUDIENCE=shop-backend`. There is no `cloud` Spring profile.
- **TypeScript system tests** read `ENVIRONMENT`, `EXTERNAL_SYSTEM_MODE`, `MYSHOP_UI_BASE_URL_<MODE>`, `MYSHOP_API_BASE_URL_<MODE>`, `ERP/CLOCK/TAX_API_BASE_URL_<MODE>`, `KEYCLOAK_URL(_<MODE>)`; config files `system-test/typescript/config/test-config-<env>-<mode>.json` (only `local` exists besides `acceptance`).

## ▶ Next executable step (resume here)

**Step 7 — USER STEP (provision), step-by-step with the user.** The user runs `./setup-gcp.sh` and pastes output; the agent helps with each ❌ / manual prompt. Wait for "provisioned", then Step 8. Steps 1–6 are done. Terraform passes `terraform validate` + `fmt` (2026-10-29); `setup-gcp.sh` is its first real apply. Workflow not yet linted since the rename (no `actionlint` here). 2026-10-29: Terraform applied (project `shop-3aa1c8`, GitHub variables set); script was stopped at the old manual Neon prompt and the Neon step is now automated — re-run `./setup-gcp.sh` to finish.

Naming is no longer hardcoded to `shop`: Terraform derives `system_name` from the repo name (max 14 chars, override with `SYSTEM_NAME=<name> ./setup-gcp.sh`), and it prefixes the GCP project, Cloud Run services (`<system>-multitier-java-{real,stub}-{frontend,backend,sim,stubs}`, `<system>-multitier-java-keycloak`), secrets (`<system>-multitier-java-acceptance-db`, `<system>-keycloak-db`, `<system>-keycloak-admin-password`) and Neon databases (`<system>_multitier_java_{real,stub}`, `<system>_keycloak`). The runtime SA is plain `runtime@<project>` (30-char SA limit). The workflow reads the prefix from the `SYSTEM_NAME` GitHub variable, which `setup-gcp.sh` sets. `db-connection-string` is kept for the other variants.

## Steps

### 7. USER STEP — provision (agent pauses here)
`./setup-gcp.sh` (devcontainer, repo root) now drives the whole step and tells the user every manual action:
1. Prerequisite checks: gcloud / gh / terraform / jq installed; `gh auth login`, `gcloud auth login`, `gcloud auth application-default login --no-launch-browser` — prints the fix command for each ❌. Needs a GCP billing account.
2. Terraform plan → user types `yes` → apply. Sets repo variables `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`, `SYSTEM_NAME`.
3. Neon via `neonctl` (pinned 8.13.2, npx fallback): signs in with the browser if needed (`neonctl login`, or `NEON_API_KEY`), finds/creates Neon project `<system>` in `aws-us-east-2`, creates any missing databases owned by `neondb_owner`, and pipes the direct-endpoint credentials into both DB secrets; Keycloak admin password is entered or generated. Skips secrets that already have a version (safe to re-run). `NEON_ORG_ID=<id>` if the Neon account has several orgs.
4. Warns if GitHub secrets `GHCR_TOKEN` / `WORKFLOW_TOKEN` are missing and prints the `gh secret set` commands.
5. User tells the agent "provisioned".

### 8. First run
After the user confirms Step 7: dispatch `multitier-java-acceptance-stage-cloud`, hand over the run ID (do not poll), fix failures within scope.

### 9. Follow-ups (not in this plan)
Template to all other variants (monolith + multitier × java/dotnet/typescript) in a new plan once multitier-java's first run is green — user-approved 2026-10-29. Cost guardrails for that: Artifact Registry cleanup policy (keep last N images per variant — the only real growing cost), one shared Keycloak across variants, Secret Manager versions beyond the 6 free (~$0.06/version/month); QA/prod cloud stages for multitier-java; the cloud realm exposes a password-grant test client and test users — acceptable for a demo, revisit before anything real.
