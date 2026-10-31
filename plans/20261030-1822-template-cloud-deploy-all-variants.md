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

**Step 9:** take multitier-java to prod in the cloud. Dispatch `multitier-java-acceptance-stage-cloud` on main to get a fresh RC on the current VERSION (the latest, `multitier-java-cloud-v1.0.234-rc.8`, is behind VERSION 1.0.235). If it skips with nothing new, use rc.8, which QA already passed. Then run the rest of Step 9 in order. Needs `gh` logged in. Check that no other cloud run is in progress first.

## Steps

- [ ] Step 9: Release multitier-java to prod in the cloud (user-approved 2026-10-31, see decision 8). Set `RC` to the tag from the acceptance run, then:
  1. If `RC` is new, QA: `gh workflow run multitier-java-qa-stage-cloud.yml --ref main -f version=$RC`. Wait for green, with no skipped jobs. Skip this for rc.8, which is already green in [41194541260](https://github.com/optivem/shop/actions/runs/41194541260).
  2. Signoff: `gh workflow run multitier-java-qa-signoff.yml --ref main -f version=$RC -f result=approved`. Pass `version` explicitly, because it can't resolve cloud RCs by itself. Wait for green.
  3. Prod: `gh workflow run multitier-java-prod-stage-cloud.yml --ref main -f version=$RC`. Wait for it to finish.
  4. Verify: all jobs succeeded and none skipped; smoke passed count > 0; the tag `${RC%-rc.*}` and its GitHub Release exist; the prod frontend returns 200 and backend `/health` returns UP.
  - **Stop and report, without retrying or rolling back by hand, if prod fails.** This is the workflow's first real run. Smoke runs after the deploy, so a smoke failure leaves the new version live with no tag or release. A retry after a partial publish may fail with "already released".
  - When Step 9 is verified, delete this plan file. Report that `shop-prod` has no users, and that the 10 stale QA/prod cloud workflows (monoliths, multitier dotnet and typescript) still read the removed `db-connection-string`.

## Decisions (user-approved 2026-10-30)

1. **System tests:** TypeScript tests against every variant.
2. **Neon for QA/prod:** separate databases in the existing project; check the free-tier limits first.
3. **Keycloak for prod:** a separate realm on the shared instance.
4. **Prod tests:** smoke tests that need no login.
5. **Old plan's teardown and re-provision:** already done by the user (reported 2026-10-31), before this plan finished, so the old plan is deleted. Project IDs recorded earlier may be stale: read `GCP_PROJECT_ID` from the GitHub variable. For any later teardown use `./teardown-gcp.sh` (shop only), not `./teardown-everything.sh --execute`, which would also delete the 11 other GCP projects.
6. **Secrets:** one shared `<system>-db` secret (host/port/user/password) plus the Keycloak admin password, 2 versions in total; database names come from convention. Supersedes the old plan's "one secret per variant". The per-variant secrets and the legacy `db-connection-string` go.
7. **Applying Terraform:** the agent runs `./setup-gcp.sh --yes` itself.
8. **Prod dispatch:** the agent does dispatch the prod stage, for Step 9 only (user-approved 2026-10-31, replacing the earlier "hand the user the command"). It publishes a real tag and a public GitHub Release. The cloud tag does not make acceptance skip, because acceptance checks `<prefix>-v<base>`; but a second cloud release on the same base fails as "already released" until VERSION is bumped.
