# 2026-08-20 12:57:05 UTC — Config gate rejects `kind: component` because gh-optivem v1.6.72 is unreleased

## TL;DR

**Why:** `meta-prerelease-stage` [run 32369796566](https://github.com/optivem/shop/actions/runs/32369796566) failed at *Validate all gh-optivem configs* — `gh-optivem-multitier-clean-java.yaml` uses `kind:`/`component:`, a schema that exists only in gh-optivem commit `f3f003d0` and in **no published release**. CI installs the latest *release* (v1.6.71), which rejects both fields. It passed locally only because the dev machine runs a source build (`gh-optivem dev-f3f003d0`) containing the very commit CI lacks.

**End result:** gh-optivem v1.6.72 is published, so the released CLI understands `kind: component` and shop's meta-prerelease pipeline gets past the config gate with no shop config change. `validate-all-gh-optivem-config.sh` names the CLI it validated with and warns loudly on a `dev-*` build, so a local pass can never again be mistaken for a CI pass.

## Outcomes

What we get out of this — the goals and deliverables:

- `optivem/shop` `meta-prerelease-stage` runs green through *Validate all gh-optivem configs* and reaches *Read VERSION values*, with `gh-optivem-multitier-clean-java.yaml` unchanged.
- gh-optivem **v1.6.72** exists as a published release, carrying the `kind:` discriminator (`f3f003d0`) that shop already depends on.
- `validate-all-gh-optivem-config.sh` prints which `gh optivem` build performed the validation, so every run — local or CI — is self-documenting about what its PASSED rows actually prove.
- Running that script against a `dev-*` build produces an unmissable warning that the result does **not** predict CI, closing the exact gap that let `24eb954e` land a config the released CLI cannot parse.
- The stale "UNBLOCKED — the dependency has landed" claim in `plans/20260818-1326-compile-all-misses-backend-clean-java.md` no longer conflates *committed in gh-optivem* with *published in a release*.
- The "just pin `install-gh-optivem`'s `ref`" shortcut is recorded as **rejected**, with the reason, so it is not re-proposed the next time this gate goes red.

## ▶ Next executable step (resume here)

**Confirm the fix landed in shop CI.** Everything actionable is done: gh-optivem `v1.6.72` is published and `Latest` (2026-08-20T13:15:14Z), and the shop-side provenance guard is committed. What remains is observation, not editing.

Watch for a `meta-prerelease-stage` run that actually reaches the gate:

```bash
gh run list --repo optivem/shop --workflow meta-prerelease-stage.yml --limit 5 \
  --json databaseId,conclusion,createdAt
gh run view <id> --repo optivem/shop     # must list "Validate all gh-optivem configs" as ✓
```

A run that stops at *Decide whether to run* proves nothing — it never installs the CLI. Wait for one with meaningful changes, or re-run 32369796566 (which continues into the full ~4h pipeline, so only do that deliberately).

If the gate passes, delete this plan file — the work is complete. Keep only the skipped-acceptance watch item if you still want it tracked.

## Steps

- [ ] **Step 1 — Confirm shop CI is green.** gh-optivem `v1.6.72` published 2026-08-20T13:15:14Z and is `Latest`, so CI now installs a CLI that understands `kind: component`. Confirm the next `meta-prerelease-stage` run gets past *Validate all gh-optivem configs* to *Read VERSION values*. The gate only executes when *Decide whether to run* finds meaningful changes, so a short-circuiting run proves nothing either way — wait for one that reaches the step, or re-run the failed run 32369796566 (note this continues into the full pipeline, not just the gate).

- [ ] **Step 2 — Watch for fallout from the skipped acceptance matrix** *(added because tests were bypassed)*. `f3f003d0` is ~1500 lines across 19 files and rewrites config parsing for all 13 configs plus adds `kind` refusal guards, but no end-to-end shop run has ever exercised it — the released v1.6.72 is unit-tested only. The first `meta-prerelease-stage` and language commit-stage runs after publication are the de-facto acceptance test. If a `kind: system` config starts being refused, or a command that used to work now errors on kind, suspect `kind_guards.go` and consider dispatching a real `gh-acceptance-stage` retroactively.

## Notes — evidence behind the diagnosis

- **Failure**: `run / check` → *Validate all gh-optivem configs* → exit 1.
  `ERROR: config: parse gh-optivem-multitier-clean-java.yaml: yaml: unmarshal errors: line 20: field kind not found in type projectconfig.Config / line 26: field component not found in type projectconfig.Config`. Summary: that one config `FAILED / SKIPPED`; the other 12 `PASSED / PASSED`.
- **Pinned cause**: `gh-optivem-multitier-clean-java.yaml:20` (`kind: component`) and `:26` (`component:`), introduced by shop commit `24eb954e` (2026-08-18 16:50). The schema they need lives in gh-optivem `f3f003d0`; `git tag --contains f3f003d0` is empty.
- **Why it stayed hidden**: the validate step is gated on `steps.decide.outputs.should_run == 'true'`. Every scheduled run since 2026-08-18 short-circuited at *Decide whether to run* (e.g. run 32354037636 at 09:28 today never reached the gate). Today's `backend-clean-java` commits were the first "meaningful changes" that let it execute — a latent break, not a new one.
- **Not reproducible locally, and that is the finding**: `gh optivem --version` → `gh-optivem dev-f3f003d0`. `24eb954e`'s "Verified: config validate and config preflight pass" was true against that dev build and false against every released CLI.
- **Parity**: no language twins. Single repo-root config; the other twelve are `kind: system`. `compile-all.sh` and `migrate-all-gh-optivem-config.sh` glob the same files but run locally only, never in CI.

## Notes — decisions taken during execution

- **Release published without the acceptance matrix.** The recommendation was a full `gh-acceptance-stage` run, on the grounds that `f3f003d0` rewrites config parsing for every config and adds refusal guards, so a regression would break all three languages at once in a release every consumer auto-installs. The user chose `debug-skip-tests=true` for speed. Recorded here rather than argued: v1.6.72 ships unit-tested but never exercised end-to-end, which is what Step 3 above watches for.
- **Rejected: pinning `install-gh-optivem`'s `ref`.** Making the gate build gh-optivem from unreleased `main` would turn a red gate green while destroying its meaning — it exists to prove shop works with the CLI a student would actually install. This now lives as a comment above the install step in `.github/workflows/_meta-prerelease-pipeline.yml`, so it survives this plan file's deletion.
- **Step dropped as moot: the stale UNBLOCKED note.** `plans/20260818-1326-compile-all-misses-backend-clean-java.md:9` claimed the gh-optivem dependency "has landed" on the strength of a commit. Shop commit `3a7ca66c` completed and deleted that plan file mid-execution, taking the note with it. The commit-vs-release distinction it got wrong is now stated in the header of `validate-all-gh-optivem-config.sh`, which is a better home for it anyway.
