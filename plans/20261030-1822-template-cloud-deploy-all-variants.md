# 2026-10-30 18:22:31 UTC — Template the cloud deploy to all variants

> 🤖 **Picked up by agent** — `49d10304c35f` at `2026-10-31T08:48:59Z`

## TL;DR

**Why:** Only `multitier/java` deploys to Cloud Run, and only through its acceptance stage (first green run: [41098534927](https://github.com/optivem/shop/actions/runs/41098534927)). The other five variants have no working cloud deploy, multitier-java has no QA/prod cloud stages, and copying the current setup as-is would multiply Keycloak instances, secrets and images.
**End result:** All six variants (monolith + multitier × java/dotnet/typescript) deploy to Cloud Run from one shared workflow, multitier-java also has QA/prod cloud stages, cost stays ~$0 when idle, and the password-grant test client and test users are not exposed anywhere real.

## Outcomes

What we get out of this — the goals and deliverables:

- A green cloud acceptance run for each of the six variants, by manual dispatch
- Adding a variant is a small config change, not a copied workflow
- Artifact Registry stops growing without limit: only the last N images per variant are kept (`app-images` is 926 MiB with one variant so far)
- One Keycloak serves all variants, instead of one per variant
- Secret Manager stays within the 6 free versions, or the overage is known and accepted
- multitier-java can be promoted to QA and prod in the cloud by manual dispatch
- Prod has no password-grant test client and no test users

## ▶ Next executable step (resume here)

**Step 5:** roll out to multitier dotnet and typescript, one green run each, by replacing `.github/workflows/multitier-{dotnet,typescript}-acceptance-stage-cloud.yml` with thin callers of `_acceptance-stage-cloud.yml`, shaped like `multitier-java-acceptance-stage-cloud.yml` (last green: [41118734035](https://github.com/optivem/shop/actions/runs/41118734035)). First extend the database loop in `ensure_neon` in `setup-gcp.sh`, which still creates only the multitier-java and Keycloak databases, and run `./setup-gcp.sh --yes`. Everything per-variant comes from `<architecture>-<language>`, including the GitHub environment `<architecture>-<language>-acceptance`, so check those environments exist. `docker/keycloak/cloud-realm.jq` already lists all six variants × real|stub, and the images are tagged by tree hash so an unchanged Keycloak is not redeployed. The other `*-stage-cloud.yml` workflows still read the removed `db-connection-string` until Steps 5 to 7 replace them. Needs `gh`, `gcloud` and `neonctl` logged in.

## Steps

- [ ] Step 5: Roll out to multitier dotnet and typescript, one green run each, replacing the existing `multitier-*-acceptance-stage-cloud.yml`
- [ ] Step 6: Roll out to monolith java, dotnet and typescript, one green run each (no separate backend service, so the workflow must handle a different service shape), replacing the existing `monolith-*-acceptance-stage-cloud.yml`
- [ ] Step 7: QA/prod cloud stages for multitier-java (manual dispatch): rewrite the existing `multitier-java-{qa,prod}-stage-cloud.yml`; get QA green, and stop before dispatching prod (see decision 8)
- [ ] Step 8: Realm hardening: prod realm without the password-grant test client and test users

## Decisions (user-approved 2026-10-30)

1. **System tests:** TypeScript tests against every variant.
2. **Neon for QA/prod:** separate databases in the existing project; check the free-tier limits first.
3. **Keycloak for prod:** a separate realm on the shared instance.
4. **Prod tests:** smoke tests that need no login.
5. **Old plan's teardown and re-provision:** already done by the user (reported 2026-10-31), before this plan finished, so the old plan is deleted. Project IDs recorded earlier may be stale: read `GCP_PROJECT_ID` from the GitHub variable. For any later teardown use `./teardown-gcp.sh` (shop only), not `./teardown-everything.sh --execute`, which would also delete the 11 other GCP projects.
6. **Secrets:** one shared `<system>-db` secret (host/port/user/password) plus the Keycloak admin password, 2 versions in total; database names come from convention. Supersedes the old plan's "one secret per variant". The per-variant secrets and the legacy `db-connection-string` go.
7. **Applying Terraform:** the agent runs `./setup-gcp.sh --yes` itself.
8. **Prod dispatch:** the agent does not dispatch the prod stage. It publishes a real tag and a public GitHub Release, and that tag makes acceptance skip until VERSION is bumped. Hand the user the exact command instead.
