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

Design only, no mechanical edits yet: settle the open questions below (especially stack ↔ domain mapping, which variant, and ordering against the custom-domains plan) with `/refine-plan` on this file. After that, Step 1 is the first executable unit.

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

## Open questions

- **Stack ↔ domain mapping (inferred):** I assumed `shop-acc` = `stub` stack (acceptance suites) and `shop-e2e` = `real` stack (e2e-api / e2e-ui suites against simulators). Is that right?
- **"Permanent" meaning (inferred):** the services already persist across runs (no teardown in the workflow). I read "permanent" as *stable named URL that stays up*, not *always-warm (min-instances ≥ 1)*. Correct? Min-instances 1 would cost money and conflict with the ~$0 goal.
- **Which variant:** only multitier-java (matching the decision that only one reference variant gets custom domains), or all six flavors (which needs a naming scheme like `shop-acc-monolith-dotnet`)?
- **Ordering vs. the custom-domains plan:** the custom-domains follow-up (QA/prod + `auth.`) waits on event-hub's `20261102-1954-custom-domains-for-cloud-run.md`. Should this plan wait for it, or be folded into it as an extra step?
- **Mapping mechanism:** Cloud Run domain mappings (free, preview, limited regions) vs. a global HTTPS load balancer (~$18/month, breaks the ~$0 goal). The answer depends on whether `GCP_REGION` supports domain mappings.
- **Multitier backend URL:** keep the backend on `run.app`, add `api.shop-acc` / `api.shop-e2e` hosts, or route `/api` through the frontend?
- **Concurrency:** two acceptance runs could redeploy the same permanent services at once. Is the existing workflow `concurrency` group enough, or should it be explicit per environment?
