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

All decisions are made (see Decisions). Start with Step 1 (read-only lifecycle check). Step 2 (DNS) is done; Steps 3–7 are edits to P2-owned files, allowed now because P2's bundle test is done (2026-11-06). Step 8 is the cloud proof.

## Steps

- [ ] Step 1: Confirm the current lifecycle: show that `_acceptance-stage.yml` service names are stable per variant and stack (`${p}`, `${p}-backend`, `${p}-frontend`), and that no workflow or script deletes them (`teardown-everything.sh` aside). Record the exact service names behind each domain.
- [ ] Step 2: DNS and domain verification for `optivem.com`: verify domain ownership for the GCP project if not already done, and add CNAME records `shop-acc` and `shop-e2e` → `ghs.googlehosted.com`. Document this as a manual one-time step in `docs/` (the user owns DNS).
- [ ] Step 3: Add the domain-mapping logic to the shared `_acceptance-stage.yml` (`deploy-stack` job): optional inputs/vars such as `acc-domain` / `e2e-domain`, used only when set. Idempotently ensure a Cloud Run domain mapping (or a load balancer, see open questions) for the stack's public service: frontend for multitier, system for monolith.
- [ ] Step 4: Wire the reference variant's caller (`multitier-java-acceptance-stage.yml`) to pass the two domains. The other variant callers stay unchanged.
- [ ] Step 5: Update `Resolve System URLs` in the `run` job so it uses the custom domain when one is set and falls back to `run.app` otherwise. For multitier, decide how the backend URL is exposed (see open questions).
- [ ] Step 6: Keycloak: add `https://shop-acc.optivem.com/*` and `https://shop-e2e.optivem.com/*` to the client's redirect URIs and web origins in `deploy-keycloak-cloud`, driven by the same vars.
- [ ] Step 7: Grant the deployer SA the permissions it needs for domain mappings (`run.domainmappings.*`), via `setup-gcp.sh`.
- [ ] Step 8: Prove it end to end: dispatch cloud acceptance for the reference variant, check that both URLs serve the new revision with valid TLS, that tests ran against them, and that a second run keeps the same URLs.
- [ ] Step 9: Docs: README links to `docs/` describing the environments (acc / e2e / qa / prod) and their URLs. No GitHub Pages.

## Decisions (user, 2026-11-06: accept all recommendations)

Open questions: none — all resolved before execution.

- **Stack ↔ domain:** `shop-acc` = `stub` stack (acceptance suites); `shop-e2e` = `real` stack (e2e suites against simulators).
- **"Permanent":** a stable named URL that stays up; min-instances stays 0 (cold starts are fine, cost stays ~$0).
- **Variant:** multitier-java only (the reference variant). The other five flavors stay on `run.app`.
- **Ordering:** don't wait for event-hub's custom-domains plan; use the same opt-in repo-variable pattern (domains unset = no behaviour change).
- **Mechanism:** Cloud Run domain mappings (free; `us-central1` supports them). No load balancer.
- **Multitier backend URL:** stays on its `run.app` URL; only the frontend gets the custom domain.
- **Concurrency:** the existing per-workflow `concurrency` group is enough.
- **DNS (Step 2) is done by the user (2026-11-06):** `optivem.com` verified for the user's Google account (Search Console, TXT record); CNAMEs `shop-acc` and `shop-e2e` → `ghs.googlehosted.com` resolve publicly. Still to check in Step 7: the deployer SA can create mappings for a domain verified by the user's account (it may need to be added as a verified owner in Search Console, or the mapping created once by the user).
