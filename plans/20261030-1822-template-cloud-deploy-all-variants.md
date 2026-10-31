# 2026-10-30 18:22:31 UTC — Template the cloud deploy to all variants

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

**Only the user's prod dispatch remains** (decision 8). Steps 7 and 8 are done: QA is green ([41194541260](https://github.com/optivem/shop/actions/runs/41194541260)), and `shop-qa` and `shop-prod` are live. Sign off the RC with `multitier-java-qa-signoff`, passing `version` explicitly, then dispatch `multitier-java-prod-stage-cloud`.

## Steps


## Decisions (user-approved 2026-10-30)

1. **System tests:** TypeScript tests against every variant.
2. **Neon for QA/prod:** separate databases in the existing project; check the free-tier limits first.
3. **Keycloak for prod:** a separate realm on the shared instance.
4. **Prod tests:** smoke tests that need no login.
5. **Old plan's teardown and re-provision:** already done by the user (reported 2026-10-31), before this plan finished, so the old plan is deleted. Project IDs recorded earlier may be stale: read `GCP_PROJECT_ID` from the GitHub variable. For any later teardown use `./teardown-gcp.sh` (shop only), not `./teardown-everything.sh --execute`, which would also delete the 11 other GCP projects.
6. **Secrets:** one shared `<system>-db` secret (host/port/user/password) plus the Keycloak admin password, 2 versions in total; database names come from convention. Supersedes the old plan's "one secret per variant". The per-variant secrets and the legacy `db-connection-string` go.
7. **Applying Terraform:** the agent runs `./setup-gcp.sh --yes` itself.
8. **Prod dispatch:** the agent does not dispatch the prod stage. It publishes a real tag and a public GitHub Release, and that tag makes acceptance skip until VERSION is bumped. Hand the user the exact command instead.
