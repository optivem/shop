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

**Step 5 — rewrite `multitier-java-acceptance-stage-cloud.yml`** (below). Already in place for it: `system/db/Dockerfile` (migrations), `docker/keycloak/Dockerfile.cloud` (needs env `FRONTEND_URL_REAL`/`FRONTEND_URL_STUB`, `KC_HOSTNAME`, `KC_DB_URL/USERNAME/PASSWORD`, `KC_BOOTSTRAP_ADMIN_*`; build context `docker/keycloak`), `external-systems/stubs/Dockerfile` (WireMock stubs, port 8080) and `external-systems/simulators/Dockerfile` (port 9000) — simulators/stubs are NOT published to GHCR, so the workflow must build+push them to Artifact Registry. `test-config-acceptance-{real,stub}.json` already exist (URLs come from env overrides), so no config work. Then Step 6; Step 7 needs the user. Step 1 (Terraform) is written but unvalidated — `terraform` is not installed on this machine; `setup-gcp.sh` will validate it in Step 7. `db-connection-string` is kept: the other variants' workflows still use it.

## Steps

### 5. Rewrite `multitier-java-acceptance-stage-cloud.yml`
**Principle: logically equivalent to `multitier-java-acceptance-stage.yml` except where cloud genuinely differs.** Parity requirements:
- **Gates:** keep `preflight`; add the "release already published" check (`check-tag-exists`, `multitier-java-v<base>`) and `evaluate-run-gate`; a manual `commit-sha` bypasses the "no new artifacts/changes" skip, as in non-cloud.
- **Two stacks**, as in non-cloud (`EXTERNAL_SYSTEM_MODE` is deploy-time): a **real** stack (frontend, backend, simulators, DB `shop_mt_java_real`) and a **stub** stack (frontend, backend, stubs, DB `shop_mt_java_stub`). Secrets: one JSON DB secret per stack. Keycloak is shared; its realm lists both frontend URLs.
- **Suites:** run the same 11 suites in the same order as non-cloud, via `gh optivem system-test setup` / `gh optivem system-test run --suite <id>` (TypeScript), **sequentially** (isolated suites need exclusive state), with env overrides `ENVIRONMENT=acceptance`, `MYSHOP_UI_BASE_URL_<MODE>`, `MYSHOP_API_BASE_URL_<MODE>`, `ERP/CLOCK/TAX_API_BASE_URL_<MODE>`, `KEYCLOAK_URL`. No raw Gradle/Playwright flags.
- **After green:** compose RC version, tag images, publish git tag, summary.

Intentional differences from non-cloud: Cloud Run deploy instead of docker compose; no Docker Hub login; `promote-images` step (GHCR to Artifact Registry, same digest); Flyway Cloud Run job before each backend deploy; longer wait for Keycloak cold start; RC tag prefix `multitier-java-cloud`; no Sonar job (would only repeat the non-cloud analysis of the same test code); `check-changes-since-tag` path `system-test/typescript` and the cloud workflow file.

Job order: `check` → `promote-images` → `deploy-keycloak` → `deploy-real-stack` / `deploy-stub-stack` (simulators/stubs, migrate, backend `--port=8081`, frontend `--port=3000`) → `run` (sequential suites) → `promote` / `publish-tag` → `summary`. Validate with `actionlint` / `lint-workflows.yml` locally.

### 6. Cost guardrails
Document in `terraform/README.md`: GCP budget alerts (€5/€20), `min-instances=0` everywhere, `teardown-gcp.sh` for reset.

### 7. USER STEP — provision (agent pauses here)
Agent gives exact commands for: `./setup-gcp.sh`; creating the Neon project, databases `shop_mt_java_real`, `shop_mt_java_stub` and `keycloak`; `gcloud secrets versions add` for the three secrets; setting `GCP_PROJECT_ID`, `GCP_REGION`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT` on the `multitier-java-acceptance` GitHub environment.

### 8. First run
After the user confirms Step 7: dispatch `multitier-java-acceptance-stage-cloud`, hand over the run ID (do not poll), fix failures within scope.

### 9. Follow-ups (not in this plan)
Template to other variants; QA/prod cloud stages for multitier-java; the cloud realm exposes a password-grant test client and test users — acceptable for a demo, revisit before anything real.
