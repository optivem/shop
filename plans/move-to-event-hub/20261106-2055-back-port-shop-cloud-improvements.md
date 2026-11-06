# 2026-11-06 20:55 UTC — Back-port shop's cloud and devcontainer improvements to event-hub

## TL;DR

**Why:** shop's GCP scripts and devcontainer were merged with event-hub's on 2026-11-03 (shop plan `20261102-2020-sync-gcp-cloud-scripts-from-event-hub.md`, now deleted). Shop is ahead on a few points, and unless they are ported back the two repos will keep drifting apart.
**End result:** event-hub has each item below, or a written reason why it doesn't apply there.

## ▶ Next executable step (resume here)

Move this file into event-hub's `plans/` and run it from an event-hub session. Start with Step 1, after checking it against event-hub's cold-start plan (`20261102-1953-reduce-cloud-run-cold-start-latency.md`).

## Steps

Collected while merging (2026-11-03). For each item, compare with shop's current file before porting.

- [ ] Step 1 — `.github/actions/deploy-keycloak-cloud`: `--min-instances 0` on Keycloak (shop: `.github/actions/deploy-keycloak`). Check it against event-hub's cold-start plan first.
- [ ] Step 2 — `.devcontainer/devcontainer.json`: pin the `gh` CLI feature version. Note: in shop's rebuilt container (2026-11-06) the pin `2.67.0` doesn't take effect, and `gh --version` reports 2.103.3 at `/usr/bin/gh`. Check why before copying the pin.
- [ ] Step 3 — `.devcontainer/fix-git-credential-helpers.sh`: repoint the helpers to `!gh auth git-credential` only when the container's `gh` is logged in. Otherwise just drop them, so VS Code's forwarded credentials apply instead of a `gh` that can't answer.
- [ ] Step 4 — `.devcontainer/post-create.sh`: chown the whole `@anthropic-ai` scope folder, not just the package (npm writes the new version next to it), as shop has done since `ad349cf1`. event-hub's `75aec36` may already cover this (scope folder + `bin/claude` link); if so, close this step.
- [ ] Step 5 — `terraform/README.md`: shop's expanded Cost section (`c3fd9b8e`): billed services, what keeps idle cost at ~$0, what would start costing money.
