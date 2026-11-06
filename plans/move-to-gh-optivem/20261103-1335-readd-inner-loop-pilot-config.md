# 2026-11-03 13:35 UTC — Re-add the inner-loop pilot keys to gh-optivem-multitier-java.yaml

> ⚠️ **Requires a gh-optivem release first.** The blocker is in the `optivem/gh-optivem` repo, not in shop. Do the shop step only after a gh-optivem release that accepts `inner-loop` is out.

## TL;DR

**Why:** `cf2869ad` ("backend-java: PIT mutation suite and test-after inner-loop pilot config") added `inner-loop: {mode: test-after}` and `paths.component-test` to the backend tier of `gh-optivem-multitier-java.yaml`. The published gh-optivem (v1.6.105, 2026-10-31) rejects the key with `field inner-loop not found in type projectconfig.TierSpec`. That failed every prerelease pipeline at `check` (e.g. `meta-prerelease-stage` run 41637810423), so `3f658c5a` removed the two keys again on 2026-11-03. The `build.gradle` (PIT) and `component-tests.yaml` parts of `cf2869ad` stayed in.
**End result:** gh-optivem supports `inner-loop` and `paths.component-test` in a published release, shop's config has the keys back, and CI validates it.

## ▶ Next executable step (resume here)

Step 1: in `optivem/gh-optivem`, check whether `TierSpec` (package `projectconfig`) has an `inner-loop` field and `paths.component-test` on `main`, and whether a release newer than v1.6.105 ships them (`gh release list -R optivem/gh-optivem -L 3`).

## Steps

- [ ] Step 1 — **gh-optivem (other repo):** add (or finish) `inner-loop` (`mode: test-after`, …) and `paths.component-test` on `TierSpec`, and publish a release. If this work already has a gh-optivem plan, follow that plan instead.
- [ ] Step 2 — **shop:** restore the keys under `system.backend` in `gh-optivem-multitier-java.yaml`, as they were in `cf2869ad`, plus the `mutation-threshold` set from the first nightly baseline (see Notes):
  ```yaml
          inner-loop:
              mode: test-after
              mutation-threshold: 50
          paths:
              component-test: system/multitier/backend-java/src/componentTest/java/com/mycompany/myshop/backend
  ```
- [ ] Step 3 — **Verify:** `gh optivem config validate -c gh-optivem-multitier-java.yaml` locally, using the new gh-optivem version. Then push and confirm that `prerelease-pipeline-multitier-java` gets past its `check` job.

## Notes

- CI installs gh-optivem via `optivem/actions/install-gh-optivem@v1`. Confirm it picks up the new release (latest vs a pinned version) before pushing Step 2.
- **Mutation threshold 50 (set 2026-11-05).** First full baseline from `multitier-backend-java-mutation-nightly` (run 41993634354, `--all`, gh-optivem main): 307 mutants, 164 killed = **53%**; 71 no-coverage; test strength 69%; line coverage of mutated classes 77%; PIT run 4m57s. The threshold sits just below the baseline so the changed-class gate starts green on current code; ratchet it up as the nightly score rises. It cannot go into the config before Step 1's release (the released CLI rejects `inner-loop`).
