# 2026-11-02 18:55:09 UTC — Single cloud pipeline (Docker commented out)

## TL;DR

**Why:** Each of the six flavors has two parallel sets of acceptance, QA and prod workflows: Docker (`*-acceptance-stage`, `*-qa-stage`, `*-prod-stage`) and cloud (`*-stage-cloud`). That doubles maintenance, and it creates two release owners per flavor. The two-owner problem is the only reason the version-slot and release-ownership plans exist.
**End result:** Each stage is one reusable workflow (`_acceptance-stage.yml`, `_qa-stage.yml`, `_prod-stage.yml`) that deploys to the real cloud. The Docker deploy steps stay in the file, commented out, for anyone who wants guaranteed zero cost. Each flavor has one release owner and one tag series.

## Outcomes

- **One workflow per stage per flavor.** Acceptance, QA and prod each exist once per flavor, as a thin caller of a shared reusable workflow. The `*-stage-cloud.yml` files are gone.
- **Cloud is the code; Docker is commented out.** The active deploy steps target Cloud Run, Neon and Keycloak-cloud, because that is the only real option in practice. The docker compose deploy and wait steps sit next to them, commented out, with a short note on how to switch: uncomment Docker and comment out cloud. There is no variable or input. The gate, version and tag logic is the same either way. (Decided 2026-11-02.)
- **One release owner per flavor.** There are no `*-cloud-v*` tags. Version bumps, prerelease probes, `drift` and `cross-lang-system-verification` work unchanged on the single tag series.
- **The docs are honest about what Docker means.** If someone uncomments Docker, acceptance runs in a real environment that is thrown away after the run, while QA and prod are only a zero-cost stand-in with nothing persistent. The comment and the docs both say so.
- **Cloud is $0 in normal use, with a hard stop.** Cloud acceptance has no cron and no heartbeat, and runs only from the prerelease pipeline when there are new commits. QA and prod are manual. Manual `workflow_dispatch` is always available. Cloud Run deploys the public GHCR images directly, so nothing is stored in Artifact Registry. A billing kill-switch disables project billing at about €1. Normal use costs $0, and the worst case is a few cents, because GCP budget data lags before the kill-switch fires. Strictly guaranteed $0 isn't possible on GCP; that remains the commented Docker block. (Decided 2026-11-02: option 1, as close to $0 as possible on GCP.)
- **Replaces the old plans.** `20261031-2111-move-release-ownership-to-cloud` is absorbed here, and `20261031-2109-cloud-prod-component-version-slots` (incident fix already landed) is closed. Both were deleted when this plan was added.

## ▶ Next executable step (resume here)

All open questions were settled on 2026-11-02. Start Step 1, the inventory. It's read-only: list every dependency on the docker/cloud split and on `*-cloud-v*` tags, and record the shared and different parts of `_acceptance-stage-cloud.yml` and `multitier-java-acceptance-stage.yml`. It unblocks Step 2, the multitier-java prototype, which is the first workflow edit.

## Steps

- [ ] Step 1: List everything that depends on the current split:
  - The `*-cloud-v*` tag prefixes, the per-flavor tag probes in `_meta-prerelease-pipeline.yml`, and the `version-files` prefixes in `bump-patch-versions`.
  - `meta-release-stage.yml`, `drift`, `cross-lang-system-verification` and `resolve-latest-prerelease-with-status`.
  - The `gh optivem` scaffold sources (prod and bump workflows), and the docs.
  - Record what `_acceptance-stage-cloud.yml` (702 lines) and the inline Docker acceptance stages (about 400 lines each) share and where they differ.
- [ ] Step 2: Prototype `_acceptance-stage.yml` on multitier-java. Pull the shared gate, version and tag logic out of `multitier-java-acceptance-stage.yml` and `_acceptance-stage-cloud.yml`, keep the cloud deploy and wait steps active, and add the Docker ones next to them, commented out. Prove cloud with a green run. Prove the commented Docker block once by uncommenting it on a throwaway branch. After that, accept that it may drift: `lint-workflows` and actionlint don't check commented YAML, and it's opt-in. (Decided 2026-11-02.) Leave the other flavors alone for now. Carried over from the deleted version-slots plan: until cutover, after any cloud prod run, confirm it pushed no bare `vX.Y.Z` component image to GHCR. For example, `docker manifest inspect ghcr.io/optivem/shop/multitier-backend-java:v<current VERSION>` should report that the image does not exist.
- [ ] Step 3: Do the same for `_qa-stage.yml` and `_prod-stage.yml` on multitier-java. The prod stage owns release duties (images, tags and the `VERSION` bump), and they stay active whichever deploy block is uncommented.
- [ ] Step 4: Cut over multitier-java. Replace its six stage files (three Docker, three cloud) with three callers and delete the old pair in the same commit, so the flavor never has two release owners. Retarget its tag probes and bump prefixes in that same change. Once the cutover is green, delete the flavor's `*-cloud-v*` tags and releases (decided 2026-11-02: cloud tags are deleted, not kept as history). On 2026-11-02, origin had exactly one: tag `multitier-java-cloud-v1.0.235` with a non-prerelease GitHub release. The 13 `*-cloud-v*-rc.*` tags exist only in local clones; remove them with `git fetch --prune --prune-tags`. Also check GHCR for images tagged with cloud versions, and delete those too.
- [ ] Step 5: Roll out to the other five flavors, using the same per-flavor single-commit cutover (decided 2026-11-02: one flavor at a time, not all six together). During rollout, the prerelease probes must handle flavors on both the old and the new tag scheme. Keep the shared `frontend-react` bumps sequential.
- [ ] Step 6: Cost minimization:
  - Remove every `schedule:` cron from the cloud stages. Cloud acceptance is triggered only by the prerelease pipeline when there are new commits; QA and prod are manual only.
  - Deploy Cloud Run straight from the public GHCR images, which removes Artifact Registry storage entirely (decided 2026-11-02, replacing the Docker Hub idea). Checked 2026-11-02: Cloud Run's docs list public GHCR and Docker Hub images as directly deployable. Cloud Run imports the image once at deploy time ("Container images are not pulled from their container repository when a new Cloud Run instance is started"), so pulls happen once per deploy, not once per instance. The shop GHCR images already allow anonymous pulls (verified for `multitier-backend-java` and `multitier-frontend-react`). Confirm the shared images (keycloak, simulators, stubs, db-migrate) are public too. No Docker Hub publishing is needed, and GitHub doesn't bill public package storage or pulls.
  - Pull limits: GitHub publishes no hard anonymous-pull limit for public GHCR images. For reference, Docker Hub allows 100 pulls per 6 h per IP anonymously and 200 per 6 h signed in, which a few deploys per commit would stay far below. Cloud Run caches public GHCR and Docker Hub images for up to an hour.
  - Way back if pulls fail: there's no fallback built in, but switching back must stay a single `git revert`. Do it in two commits: first switch the deploy to GHCR (prove it on multitier-java, then all flavors), and only after several green cloud runs, remove the `promote-images` copy and the `app-images` repository from `terraform/main.tf` in a separate commit. If pulls fail, revert the switch commit and stop and ask the user.
  - Add the billing kill-switch: a budget plus a Pub/Sub-triggered Cloud Function that disables billing on the project at about €1, as documented by Google, provisioned through terraform/`setup-gcp.sh`. Keep the €5/€20 budget alerts. Check that the function itself runs within the free tier.
  - Verify inside the free tier: the Cloud Run free quota covers expected commit-triggered runs, and egress from Cloud Run to Neon (AWS us-east-2) stays within the free networking allowance.
  - Document the cost model in `terraform/README.md`, replacing the "~$0/month" line with the actual drivers.
- [ ] Step 7: Update the `gh optivem` scaffolds to emit the three callers, with cloud active, as in shop. The scaffold README says a fresh project's acceptance stage fails until GCP is set up, or until the Docker block is uncommented. (Decided 2026-11-02: option (a), scaffolds inherit cloud as is.)
- [ ] Step 8: Delete the GitHub environments the cutover leaves unused (such as `multitier-java-qa-cloud`).
- [ ] Step 9: Update the docs (README and `docs/*.md`): how to switch to Docker by uncommenting, and the note that Docker QA and prod are a stand-in.

## Out of scope

- Later, not part of this plan: the user may remove the commented-out Docker block entirely. Keep it self-contained (one block per workflow, clearly marked) so that removal is a clean delete.
