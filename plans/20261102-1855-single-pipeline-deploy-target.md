# 2026-11-02 18:55:09 UTC — Single pipeline with a deploy target

## TL;DR

**Why:** Each of the six flavors has two parallel sets of acceptance, QA and prod workflows: Docker (`*-acceptance-stage`, `*-qa-stage`, `*-prod-stage`) and cloud (`*-stage-cloud`). That doubles maintenance, and it creates two release owners per flavor. The two-owner problem is the only reason the version-slot and release-ownership plans exist.
**End result:** Each stage is one reusable workflow (`_acceptance-stage.yml`, `_qa-stage.yml`, `_prod-stage.yml`) whose deploy step switches on `DEPLOY_TARGET=cloud|docker`. Each flavor has one release owner and one tag series. Shop runs on cloud, and `gh-optivem` scaffolds default to Docker at zero cost.

## Outcomes

- **One workflow per stage per flavor.** Acceptance, QA and prod each exist once per flavor, as a thin caller of a shared reusable workflow. The `*-stage-cloud.yml` files are gone.
- **The repo variable picks the deploy target.** `DEPLOY_TARGET=cloud` deploys to Cloud Run, Neon and Keycloak-cloud; `DEPLOY_TARGET=docker` deploys with docker compose on the runner. The gate, version and tag logic is the same for both targets.
- **One release owner per flavor.** There are no `*-cloud-v*` tags. Version bumps, prerelease probes, `drift` and `cross-lang-system-verification` work unchanged on the single tag series.
- **Shop runs on cloud; scaffolds default to Docker.** A scaffolded project goes green with no GCP account or billing, and on forks without secrets. Switching to cloud is a variable plus the GCP setup.
- **The docs are honest about what Docker means.** Docker acceptance is a real throwaway environment for the run. Docker QA and prod are a zero-cost stand-in with nothing persistent, and the docs say so.
- **Cloud acceptance cost is bounded.** There's no hourly cron. It runs from the prerelease pipeline when there are new commits, plus a heartbeat.
- **Replaces the old plans.** `20261031-2111-move-release-ownership-to-cloud` is absorbed here, and `20261031-2109-cloud-prod-component-version-slots` (incident fix already landed) is closed. Both were deleted when this plan was added.

## ▶ Next executable step (resume here)

Design remains. Run `/refine-plan` on this file to settle the open questions, especially the variable's scope, the cutover strategy and the heartbeat. Then start Step 1, the inventory. The first code step is Step 2, the multitier-java prototype.

## Steps

- [ ] Step 1: List everything that depends on the current split:
  - The `*-cloud-v*` tag prefixes, the per-flavor tag probes in `_meta-prerelease-pipeline.yml`, and the `version-files` prefixes in `bump-patch-versions`.
  - `meta-release-stage.yml`, `drift`, `cross-lang-system-verification` and `resolve-latest-prerelease-with-status`.
  - The `gh optivem` scaffold sources (prod and bump workflows), and the docs.
  - Record what `_acceptance-stage-cloud.yml` (702 lines) and the inline Docker acceptance stages (about 400 lines each) share and where they differ.
- [ ] Step 2: Prototype `_acceptance-stage.yml` on multitier-java. Pull the shared gate, version and tag logic out of `multitier-java-acceptance-stage.yml` and `_acceptance-stage-cloud.yml`, and switch the deploy and wait steps on `DEPLOY_TARGET`. Prove both targets with one green run each. Leave the other flavors alone for now. Carried over from the deleted version-slots plan: until cutover, after any cloud prod run, confirm it pushed no bare `vX.Y.Z` component image to GHCR. For example, `docker manifest inspect ghcr.io/optivem/shop/multitier-backend-java:v<current VERSION>` should report that the image does not exist.
- [ ] Step 3: Do the same for `_qa-stage.yml` and `_prod-stage.yml` on multitier-java. The prod stage owns release duties for both targets: images, tags and the `VERSION` bump.
- [ ] Step 4: Cut over multitier-java. Replace its six stage files (three Docker, three cloud) with three callers and delete the old pair in the same commit, so the flavor never has two release owners. Retarget its tag probes and bump prefixes in that same change.
- [ ] Step 5: Roll out to the other five flavors, using the same per-flavor single-commit cutover. Keep the shared `frontend-react` bumps sequential.
- [ ] Step 6: Set the cloud acceptance schedule: run on new commits from the prerelease pipeline, plus a heartbeat.
- [ ] Step 7: Make the `gh optivem` scaffolds emit the three callers with `DEPLOY_TARGET=docker` by default, and document how to switch to cloud.
- [ ] Step 8: Update the docs (README and `docs/*.md`), including the note that Docker QA and prod are a stand-in.

## Open questions

- Variable scope: should `DEPLOY_TARGET` be a repo variable, a per-environment variable, or a `workflow_dispatch` input that defaults to the repo variable? Recommendation: a repo variable that `workflow_dispatch` can override, so cloud can be tried manually before switching.
- Inferred: should one repo be able to mix targets per stage (for example Docker acceptance with cloud QA and prod)? Recommendation: no, one target per repo, because mixing brings the two-owner question back.
- Cutover: per flavor (as drafted) or all six at once? Recommendation: per flavor, because each commit still has a single owner and the first cutover is small enough to debug.
- Existing `*-cloud-v*` tags: leave them as history and resume on the plain series from the highest version in either series? Recommendation: yes, and confirm `bump-patch-versions` picks the higher number.
- Heartbeat interval for cloud acceptance (daily or weekly)? Pick it after checking real GCP billing.
- Does `optivem/actions` need changes, for example a deploy action that dispatches on the target? Inferred: probably not, because both deploy paths already exist as separate actions.
