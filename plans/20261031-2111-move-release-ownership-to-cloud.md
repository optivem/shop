# 2026-10-31 21:11:34 UTC — Move release ownership to the cloud prod stages

## TL;DR

**Why:** The on-prem prod stages do all the release bookkeeping today: bare component images, component git tags and `VERSION` bumps. If cloud becomes the only production path, removing on-prem would leave nothing advancing versions, and the bump and prerelease checks would still look for on-prem tags.
**End result:** The cloud prod stages own releases for all six flavors, the on-prem prod stages are gone, and versions, tags and prerelease checks all work from cloud releases. Only one stage owns a version at any time.

## Outcomes

- A cloud prod deploy of any flavor is a complete release: images, git tags and a `VERSION` bump.
- Repeat cloud deploys work, without stopping on "already released".
- `meta-bump-all` and `meta-prerelease-stage` work from cloud release tags, with no on-prem release needed.
- All six flavors switch together, so no flavor has two release owners.
- The on-prem prod stages are removed once cloud has proven itself for every flavor.

## ▶ Next executable step (resume here)

Design remains, not mechanical edits. Run `/refine-plan` on this file to settle the open questions, and do not edit workflows until they are resolved. Step 1 needs `plans/20261031-2109-cloud-prod-component-version-slots.md` to be finished first.

## Steps

- [ ] Step 1: Confirm the prerequisite plan `plans/20261031-2109-cloud-prod-component-version-slots.md` is done and `meta-prerelease-stage` is green.
- [ ] Step 2: Inventory everything that depends on on-prem release tags or on-prem stages: `bump-patch-versions` prefixes, `_meta-prerelease-pipeline.yml` flavor probes, `meta-release-stage.yml`, `drift`, `cross-lang-system-verification`, `resolve-latest-prerelease-with-status`, `gh optivem` scaffolding, and the docs.
- [ ] Step 3: Add release duties to all six cloud prod stages in one change: bare component images, component git tags (on-prem publishes these only for multitier today) and the `*-bump-patch-version.yml` call. Keep the shared `frontend-react` bumps sequential.
- [ ] Step 4: Retarget bump `version-files` prefixes (such as `multitier-java-v`) and the per-flavor tag probes to `*-cloud-v` tags, in the same switch as Step 3.
- [ ] Step 5: Disable the on-prem prod stages in the same switch, so two owners never run side by side.
- [ ] Step 6: Verify on all six flavors: run a cloud prod deploy, confirm tags and bumps, deploy a second time, then confirm `meta-prerelease-stage` is green.
- [ ] Step 7: Delete the disabled on-prem prod stages, and update the docs.

## Open questions

- Should cloud releases keep sharing `VERSION` files with on-prem? Recommendation: yes. Once on-prem is gone, separate files add nothing.
- What happens to the other on-prem stages (acceptance, QA, legacy, sign-off)? Recommendation: keep them until Step 7 and decide per stage, because the drafted scope only covers prod stages.
- Should tags stay named `*-cloud-v*`, or go back to the plain `multitier-java-v*` form after the switch? Recommendation: keep `*-cloud-v*` to avoid touching history, unless the inventory in Step 2 shows renaming is cheaper.
- Inferred: all six flavors switch at once. Do monolith and multitier really need to move together, or could they be two batches if each batch has one owner?
- What triggers this plan: a date, or a decision to drop on-prem? It stays parked until you decide.
- Is cloud the only production path, or the only path of any kind? Recommendation: production only. Keep the Docker-based commit and acceptance stages, because they need no cloud account and catch most regressions at no cloud cost.
- How does this affect `gh-optivem` scaffolds? The prod and bump workflows double as scaffold sources, so cloud-only production may require every scaffolded project to have a GCP account and billing. Check this in the Step 2 inventory.
- What schedule should cloud acceptance have? Its cron is off today ("to keep demo costs down"), and hourly across six flavors would be 144 runs a day. Recommendation: no hourly cron; run it from the prerelease pipeline when there are new commits, plus one daily or weekly heartbeat. Pick the heartbeat after checking real GCP billing.
- Should `bump-patch-versions` in `optivem/actions` also advance when the image exists? This closes the remaining deadlock, but it is a separate repo that other workflows share.
