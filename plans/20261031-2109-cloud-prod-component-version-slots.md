# 2026-10-31 21:09:45 UTC — Stop cloud prod stages claiming on-prem component version slots

## TL;DR

**Why:** `meta-prerelease-stage` has failed on every scheduled run since the first cloud prod deploy (run 41203994943, 2026-10-31 13:05 UTC). That run pushed `multitier-backend-java:v1.5.216` and `multitier-frontend-react:v1.3.221` to GHCR without git tags or a `VERSION` bump. The prerelease check treats those images as taken versions, and `meta-bump-all` cannot advance them because it only bumps on a git tag.
**End result:** Cloud prod releases are identified only by their `*-cloud-v*` tags, no cloud run can occupy a bare `vX.Y.Z` component version, the two stuck `VERSION` files have moved on, and `meta-prerelease-stage` is green.

## Outcomes

- `meta-prerelease-stage` passes its pre-bump and post-bump version checks again.
- A cloud prod deploy of any of the six flavors leaves no bare `vX.Y.Z` component image behind.
- `multitier-backend-java` is at 1.5.217 and `multitier-frontend-react` is at 1.3.222, past the occupied versions.
- Existing GHCR images stay in place, and no git tags are fabricated.

## ▶ Next executable step (resume here)

Step 1: in `.github/workflows/multitier-java-prod-stage-cloud.yml`, comment out lines 185-208 (`Read Component Base Versions`, `Compose Component Tags`, `Tag Docker Images with Component Release Versions`) rather than deleting them, with the explanatory comment from Step 1 above the block, first checking that no later step references the `read-component-versions` or `compose-component-tags` ids. Do the same in the other five cloud prod workflows (Step 2) so all six land in one commit. Do not commit the recovery bump (Step 3) before these changes, or another cloud run could occupy the new versions.

## Root cause

- `.github/workflows/multitier-java-prod-stage-cloud.yml:204` retags images to bare component versions read from `VERSION`. The on-prem stage does the same, but also publishes component git tags (`multitier-java-prod-stage.yml:220-228`) and bumps (`:246-251`). The cloud stage does neither.
- `_meta-prerelease-pipeline.yml` (`Probe Component + Monolith Image Tags`) treats an existing image as a taken version.
- `optivem/actions/bump-patch-versions` advances only when a git tag exists. It logged `Skipped system/multitier/backend-java/VERSION: 1.5.216 (no tag at multitier-backend-java-v1.5.216)`, so `meta-bump-all` run 41241119387 was green but changed nothing.
- Failing runs: 41221045291, 41241065273. Classification: CI/workflow misconfiguration, deterministic, not a flake.
- Verified state: both images exist in GHCR. Git tags `multitier-backend-java-v1.5.216`, `multitier-frontend-react-v1.3.221` and `multitier-java-v1.0.235` do not exist.

## Steps

- [ ] Step 1: Comment out, do not delete, the three component-tagging steps in `multitier-java-prod-stage-cloud.yml` (lines 185-208). Put a comment above the block saying the steps are disabled because cloud prod must not claim bare `vX.Y.Z` component versions: this stage creates no component git tags and no `VERSION` bump, so `bump-patch-versions` skips those versions and `meta-prerelease-stage` deadlocks. Say to re-enable them only together with those git tags and the bump, as in `plans/20261031-2111-move-release-ownership-to-cloud.md`, and name failing run 41241065273.
- [ ] Step 2: Comment out the same three steps, with the same comment, in the other five workflows, reading each block first and confirming no remaining active step uses their ids:
  - `multitier-dotnet-prod-stage-cloud.yml` (starts at line 294, `Tag Docker Images…` step at 313)
  - `multitier-typescript-prod-stage-cloud.yml` (starts at line 294, `Tag Docker Images…` step at 313)
  - `monolith-java-prod-stage-cloud.yml` (starts at line 252, `Tag Docker Images…` step at 268)
  - `monolith-dotnet-prod-stage-cloud.yml` (starts at line 252, `Tag Docker Images…` step at 268)
  - `monolith-typescript-prod-stage-cloud.yml` (starts at line 252, `Tag Docker Images…` step at 268)
- [ ] Step 3: One-time recovery: set `system/multitier/backend-java/VERSION` to `1.5.217` and `system/multitier/frontend-react/VERSION` to `1.3.222`. First re-check that those versions are free in GHCR and git.
- [ ] Step 4: In `_meta-prerelease-pipeline.yml`, correct the post-bump failure text. It currently says `meta-bump-all did not advance the relevant VERSION files` without explaining why; it should say an image exists with no matching git tag, so the bump skipped it and a manual `VERSION` bump is needed.
- [ ] Step 5: Verify:
  - `grep -lE '^\s*- name: .*Component Release Versions' .github/workflows/*prod-stage-cloud.yml` returns nothing, so no active step remains (the commented-out ones start with `#`).
  - All six workflows parse (run `actionlint` if available).
  - Dispatch `meta-prerelease-stage` and confirm the `check` job passes both version checks and the pipeline continues.
  - On the next cloud prod run, confirm no new bare `vX.Y.Z` component image appears.

## Open questions

- Inferred, please confirm: nothing needs bare `vX.Y.Z` component images from cloud releases. No consumer was found in `_deploy-stage-cloud`, `drift`, `cross-lang-system-verification` or `meta-release-stage`, but consumers outside this repo were not checked.
- Should the `v1.5.216` and `v1.3.221` images be left in GHCR? This plan leaves them: they are harmless once `VERSION` has moved on, and they match the build that is live in cloud prod.
