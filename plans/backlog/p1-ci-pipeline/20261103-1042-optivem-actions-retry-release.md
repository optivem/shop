# 2026-11-03 10:42 UTC — Release the retry classification fix in optivem/actions

> ⚠️ **Requires the `optivem/actions` repo.** All work happens in the sibling `optivem/actions` repo (`../actions` from shop), not in shop. That checkout isn't in this devcontainer as of 2026-11-03, so clone it first. Moving the `v1` tag affects **every** repo that uses `optivem/actions/*@v1`, not just shop.

## TL;DR

**Why:** Shop uses `optivem/actions/retry@v1` in about 40 places. The fix `cd0432c` ("Classify retry failures on messages, not stack frames") is on `optivem/actions` `main` but not in `v1` (`v1` = `d02d32b`, one commit behind). So a SonarCloud HTTP failure whose trace passes through `DefaultScannerWsClient.call` can still be classified as a hard fail.
**End result:** `optivem/actions` `v1` includes `cd0432c`, and consumer runs resolve `@v1` to the new SHA.

## ▶ Next executable step (resume here)

Step 1: in the `optivem/actions` checkout, confirm that `v1` is still one commit behind `main` and that the only new commit is `cd0432c`.

## Steps

- [ ] Step 1 — **Check:** `git fetch --tags` in `optivem/actions`, then confirm `git log v1..origin/main` is only `cd0432c` (or review whatever else would ship with it).
- [ ] Step 2 — **Release:** run that repo's `update-v1.yml` workflow (don't move the tag by hand).
- [ ] Step 3 — **Verify:** re-run one shop commit stage that uses `retry@v1` and confirm in the log that `optivem/actions@v1` resolves to the new SHA.
- [ ] Step 4 — **Optional (only if per-call tuning is ever needed):** add `attempts` / `delays` inputs to `retry/action.yml` (`shared/retry.sh:43` hardcodes `_RETRY_DELAYS=(5 15 45)`, `retry-core.sh:49` `_RETRY_CORE_ATTEMPTS=4`), cover them in `shared/_test-retry.sh`, and release via `update-v1.yml`. The shop Sonar outage plan doesn't need this.

## Decisions

- Split out of `20260917-1949-sonar-outage-survives-commit-stage.md` (its former Steps 1 and 3) on 2026-11-03, so that the shop meta-plan never pushes to `optivem/actions`. The user asked for that.
- Releasing to all `@v1` consumers needs the user's go-ahead when this plan is executed.
