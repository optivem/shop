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

**Step 1:** add an Artifact Registry cleanup policy for `app-images` in `terraform/main.tf`, apply it through `setup-gcp.sh`, and confirm the old images go.

## Steps

- [ ] Step 1: Cost guardrail: Artifact Registry cleanup policy (keep the last N images per variant) in Terraform
- [ ] Step 2: Shared Keycloak: split it out of the multitier-java stack and rename `shop-multitier-java-keycloak`; realm import lists every variant's frontend URL
- [ ] Step 3: Secrets: decide the layout that stays within the 6 free versions, then update Terraform and `setup-gcp.sh`
- [ ] Step 4: Extract a reusable workflow from `multitier-java-acceptance-stage-cloud.yml`, keeping multitier-java green on it
- [ ] Step 5: Roll out to multitier dotnet and typescript, one green run each, replacing the existing `multitier-*-acceptance-stage-cloud.yml`
- [ ] Step 6: Roll out to monolith java, dotnet and typescript, one green run each (no separate backend service, so the workflow must handle a different service shape), replacing the existing `monolith-*-acceptance-stage-cloud.yml`
- [ ] Step 7: QA/prod cloud stages for multitier-java (manual dispatch): rewrite the existing `multitier-java-{qa,prod}-stage-cloud.yml`
- [ ] Step 8: Realm hardening: prod realm without the password-grant test client and test users

## Decisions (user-approved 2026-10-30)

1. **System tests:** TypeScript tests against every variant.
2. **Neon for QA/prod:** separate databases in the existing project; check the free-tier limits first.
3. **Keycloak for prod:** a separate realm on the shared instance.
4. **Prod tests:** smoke tests that need no login.
5. **Old plan's Step 10:** runs after this plan, with `./teardown-gcp.sh` (shop only). Not `./teardown-everything.sh --execute`, which would also delete the 11 other GCP projects.
