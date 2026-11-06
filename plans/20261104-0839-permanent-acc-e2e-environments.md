# 2026-11-04 08:39:27 UTC — Permanent acceptance and E2E environments on shop-acc / shop-e2e.optivem.com

## TL;DR

**Why:** Cloud acceptance (`_acceptance-stage.yml`) deploys two Cloud Run stacks per run: `stub` (system + stubs) and `real` (system + simulators). Their only addresses are generated `*-<project-number>.<region>.run.app` URLs, so nobody can treat them as named, long-lived environments to open, demo, or point tests at, the way `shop-qa` and `shop` will be.
**End result:** The acceptance-test environment is always reachable at `https://shop-acc.optivem.com`, and the E2E-test environment at `https://shop-e2e.optivem.com`. Both are permanent Cloud Run services that each acceptance run redeploys in place, never tears down. Tests and Keycloak redirects use the custom domains.

## Outcomes

- `https://shop-acc.optivem.com` serves the **stub** stack (acceptance tests: system + stubs) of the reference variant, and stays up between runs.
- `https://shop-e2e.optivem.com` serves the **real** stack (E2E tests: system + simulators) of the reference variant, and stays up between runs.
- Each cloud acceptance run redeploys these same services (a new revision). It never creates per-run services and never deletes any, so the URLs and DNS never change.
- `gh optivem system-test run` in the cloud acceptance stage targets the custom-domain URLs for the reference variant (`Resolve System URLs`), so the tests exercise the real public entry point, including TLS and the domain mapping.
- Keycloak accepts logins from both domains (redirect URIs / web origins include `shop-acc` and `shop-e2e`).
- Domains are opt-in through repo variables, same pattern as the planned `CUSTOM_DOMAIN`. Scaffolded repos and the other variants stay on `*.run.app` without changes.
- Cost stays ~$0: Cloud Run with min-instances 0, so "permanent" means a stable URL and a cold start, not an always-warm instance.

## ▶ Next executable step (resume here)

Step 8, the cloud proof. Wait until meta-prerelease run 42224541232 has finished and the local P5 commits are pushed. Then:

1. Check the certificates: `gcloud beta run domain-mappings describe --domain shop-acc.optivem.com --region us-central1 --project shop-90b6e2` (and `shop-e2e`) must show `Ready: True`. Then run `ACC_DOMAIN=shop-acc.optivem.com E2E_DOMAIN=shop-e2e.optivem.com ./setup-custom-domains.sh`, which sets the `ACC_DOMAIN` / `E2E_DOMAIN` repo variables only once each mapping is ready (or set them with `gh variable set` after checking Ready yourself).
2. Dispatch `gh workflow run multitier-java-acceptance-stage.yml` (it is skipped by the run gate unless there are new artifacts or test/workflow changes since the last RC; the P5 commit touches `_acceptance-stage.yml`, so the first run after the push goes ahead).

## Steps

- [ ] Step 8: Prove it end to end. In the run, check: `deploy-keycloak` logs `shop-frontend now accepts: https://shop-acc.optivem.com https://shop-e2e.optivem.com` (later runs: `already accepts`); `deploy-stack` passes `Wait for Custom Domain (stub)` and `(real)`; `run` → `Resolve System URLs` / `Point systems.yaml` show `https://shop-acc.optivem.com` and `https://shop-e2e.optivem.com` as the UI URLs, and the e2e/acceptance UI suites pass (they log in through Keycloak, so this proves the redirect URIs). Afterwards `curl -sI https://shop-acc.optivem.com/` and `https://shop-e2e.optivem.com/` return 200 with a valid certificate. A second dispatch (with `commit-sha` set, to pass the run gate) keeps the same URLs and mapping. If it goes red only because of the domains, delete the two variables to fall back to `run.app`.

## Done (2026-11-06, unpushed until meta-prerelease 42224541232 finishes)

- Step 1: service names are stable per variant and stack (`${SYSTEM}-${PREFIX}-${STACK}[-frontend|-backend|-stubs|-sim]`), redeployed in place by `gcloud run deploy`; no workflow or script deletes them (only `teardown-gcp.sh` / `teardown-everything.sh`, which remove the whole project). Behind the domains: `shop-acc.optivem.com` → `shop-multitier-java-stub-frontend`, `shop-e2e.optivem.com` → `shop-multitier-java-real-frontend` (both exist).
- Step 2: DNS done by the user; documented as the one-time setup in `docs/environments.md`.
- Steps 3–6: the mapping is created on the user side, not in the workflow (see Decisions): `setup-custom-domains.sh` (called by `setup-gcp.sh` when `ACC_DOMAIN`/`E2E_DOMAIN` are set) creates the two mappings idempotently and sets the repo variables once each is Ready. `_acceptance-stage.yml` gains `acc-domain` / `e2e-domain` inputs (default empty): the domain is added to the backend's `ALLOWED_ORIGINS` (needed: nginx forwards the browser's `Origin`), a `Wait for Custom Domain` step checks TLS + route, and `Resolve System URLs` uses it as the multitier UI URL; the backend stays on `run.app`; monolith with a domain fails fast. `deploy-keycloak` gains `frontend-origins`: an add-only admin-API reconcile of the `shop` realm's `shop-frontend` client (the realm file never reaches an existing realm). Only `multitier-java-acceptance-stage.yml` passes `vars.ACC_DOMAIN` / `vars.E2E_DOMAIN`. The two mappings were created on 2026-11-06 with the user's gcloud identity (certificates pending at the time).
- Step 7: dropped. The deployer already has `roles/run.admin` (includes `run.domainmappings.*`); the blocker was domain ownership, not IAM, and the workflow no longer creates mappings.
- Step 9: `docs/environments.md` (acc / e2e / qa / prod, service names, URLs, one-time setup), linked from README and `docs/pipeline/acceptance-stage.md`.

## Decisions (user, 2026-11-06: accept all recommendations)

Open questions: none — all resolved before execution.

- **Stack ↔ domain:** `shop-acc` = `stub` stack (acceptance suites); `shop-e2e` = `real` stack (e2e suites against simulators).
- **"Permanent":** a stable named URL that stays up; min-instances stays 0 (cold starts are fine, cost stays ~$0).
- **Variant:** multitier-java only (the reference variant). The other five flavors stay on `run.app`.
- **Ordering:** don't wait for event-hub's custom-domains plan; use the same opt-in repo-variable pattern (domains unset = no behaviour change).
- **Mechanism:** Cloud Run domain mappings (free; `us-central1` supports them). No load balancer.
- **Multitier backend URL:** stays on its `run.app` URL; only the frontend gets the custom domain.
- **Concurrency:** the existing per-workflow `concurrency` group is enough.
- **DNS (Step 2) is done by the user (2026-11-06):** `optivem.com` verified for the user's Google account (Search Console, TXT record); CNAMEs `shop-acc` and `shop-e2e` → `ghs.googlehosted.com` resolve publicly.
- **Who creates the mapping (2026-11-06, during execution):** the user's identity, once, via `setup-custom-domains.sh` — the deployer SA isn't a verified owner, and a mapping on a permanent service survives every redeploy. So the workflow only consumes the domains.
