# 2026-10-31 21:09:45 UTC — Stop cloud prod stages claiming on-prem component version slots

> 🤖 **Picked up by agent** — `531392d34340` at `2026-11-02T10:29:38Z`

## TL;DR

**Why:** `meta-prerelease-stage` has failed on every scheduled run since the first cloud prod deploy (run 41203994943, 2026-10-31 13:05 UTC). That run pushed `multitier-backend-java:v1.5.216` and `multitier-frontend-react:v1.3.221` to GHCR without git tags or a `VERSION` bump. The prerelease check treats those images as taken versions, and `meta-bump-all` cannot advance them because it only bumps on a git tag.
**End result:** Cloud prod releases are identified only by their `*-cloud-v*` tags, no cloud run can occupy a bare `vX.Y.Z` component version, the two stuck `VERSION` files have moved on, and `meta-prerelease-stage` is green.

## Outcomes

- `meta-prerelease-stage` passes its pre-bump and post-bump version checks again.
- A cloud prod deploy of any of the six flavors leaves no bare `vX.Y.Z` component image behind.
- `multitier-backend-java` is at 1.5.217 and `multitier-frontend-react` is at 1.3.222, past the occupied versions.
- Existing GHCR images stay in place, and no git tags are fabricated.

## ▶ Next executable step (resume here)

Step 5: after the fixes are pushed, dispatch `meta-prerelease-stage` (`gh workflow run meta-prerelease-stage.yml`) and confirm the `check` job passes both version checks and the pipeline continues. Then watch the next cloud prod run for any new bare `vX.Y.Z` component image.

## Root cause

- `.github/workflows/multitier-java-prod-stage-cloud.yml:204` retags images to bare component versions read from `VERSION`. The on-prem stage does the same, but also publishes component git tags (`multitier-java-prod-stage.yml:220-228`) and bumps (`:246-251`). The cloud stage does neither.
- `_meta-prerelease-pipeline.yml` (`Probe Component + Monolith Image Tags`) treats an existing image as a taken version.
- `optivem/actions/bump-patch-versions` advances only when a git tag exists. It logged `Skipped system/multitier/backend-java/VERSION: 1.5.216 (no tag at multitier-backend-java-v1.5.216)`, so `meta-bump-all` run 41241119387 was green but changed nothing.
- Failing runs: 41221045291, 41241065273. Classification: CI/workflow misconfiguration, deterministic, not a flake.
- Verified state: both images exist in GHCR. Git tags `multitier-backend-java-v1.5.216`, `multitier-frontend-react-v1.3.221` and `multitier-java-v1.0.235` do not exist.

## Steps

- [ ] Step 5: Verify:
  - Dispatch `meta-prerelease-stage` and confirm the `check` job passes both version checks and the pipeline continues.
  - On the next cloud prod run, confirm no new bare `vX.Y.Z` component image appears.

## Open questions

None, all resolved 2026-11-02: no consumer needs bare cloud component images, and the existing `v1.5.216`/`v1.3.221` images stay in GHCR.
