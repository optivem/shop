# 2026-11-03 13:35 UTC — Re-add the inner-loop pilot keys to gh-optivem-multitier-java.yaml

> ⚠️ **Waits for a gh-optivem release.** The gh-optivem half moved to `gh-optivem/plans/20261103-1335-tierspec-inner-loop-component-test.md` on 2026-11-12. Do this shop step only after a gh-optivem release that accepts `inner-loop` is out.

## TL;DR

**Why:** `cf2869ad` added `inner-loop: {mode: test-after}` and `paths.component-test` to the backend tier of `gh-optivem-multitier-java.yaml`, but the published gh-optivem rejected the key, so `3f658c5a` removed them again on 2026-11-03. The `build.gradle` (PIT) and `component-tests.yaml` parts of `cf2869ad` stayed in.
**End result:** shop's config has the keys back, and CI validates them with the new gh-optivem release.

## ▶ Next executable step (resume here)

Blocked. Check `gh release list -R optivem/gh-optivem -L 3` for a release that ships the gh-optivem plan above. Once there is one, do Step 1.

## Steps

- [ ] Step 1: Restore the keys under `system.backend` in `gh-optivem-multitier-java.yaml`, as they were in `cf2869ad`, plus the mutation threshold:
  ```yaml
          inner-loop:
              mode: test-after
              mutation-threshold: 50
          paths:
              component-test: system/multitier/backend-java/src/componentTest/java/com/mycompany/myshop/backend
  ```
- [ ] Step 2: Verify. Run `gh optivem config validate -c gh-optivem-multitier-java.yaml` locally with the new gh-optivem version. Then push and confirm that `prerelease-pipeline-multitier-java` gets past its `check` job.

## Notes

- **Mutation threshold 50 (set 2026-11-05).** First full baseline from `multitier-backend-java-mutation-nightly` (run 41993634354, `--all`): 307 mutants, 164 killed = **53%**; 71 no-coverage; test strength 69%. The threshold sits just below the baseline so the changed-class gate starts green on current code. Ratchet it up as the nightly score rises.
