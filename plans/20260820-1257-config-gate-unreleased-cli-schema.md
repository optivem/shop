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

**Step 1 — publish gh-optivem v1.6.72.** This is the fix that turns shop green; everything else is hardening. It runs in the sibling `optivem/gh-optivem` checkout, not in shop, and it needs the user's go-ahead because it publishes a release.

Verify nothing changed since diagnosis, then dispatch:

```bash
ROOT="$(cd "$(git rev-parse --show-toplevel)/.." && pwd)"
cd "$ROOT/gh-optivem"
cat VERSION                       # expect 1.6.72
git log --oneline v1.6.71..origin/main   # expect f3f003d0 as the only code commit
gh release list --limit 3         # expect v1.6.71 still Latest
```

Then ask the user before dispatching `gh-acceptance-stage` (it is `workflow_dispatch`-only — the schedule is commented out at `.github/workflows/gh-acceptance-stage.yml:7-8`), wait for it, and follow with `gh-release-stage`. Stop and report once `v1.6.72` appears in `gh release list`.

Unblocks: shop's next scheduled `meta-prerelease-stage` clears the config gate.

## Steps

- [ ] **Step 1 — Publish gh-optivem v1.6.72** *(external repo; user-gated)*. In the `optivem/gh-optivem` checkout: confirm `VERSION` is `1.6.72` and that `f3f003d0` is the only code commit since `v1.6.71` (`53b174d9`, `abaf29ec`, `cfe50630`, `79d8cb66` are plan/doc/version-bump only). Ask the user before dispatching — this publishes a release. Dispatch `gh-acceptance-stage` (workflow_dispatch-only), let it finish, then `gh-release-stage`. Confirm `v1.6.72` is `Latest`. No deadlock risk: gh-optivem's acceptance stage tests against shop's latest `meta-v*` tag (`meta-v1.0.184`, 2026-08-18T20:11Z), which predates the offending config commit.

- [ ] **Step 2 — Add a CLI-provenance banner to `validate-all-gh-optivem-config.sh`.** Before the per-config loop, print `gh optivem --version` (do **not** redirect its stderr to `/dev/null`) so every run records which build produced its PASSED rows. Keep the existing summary table untouched.

- [ ] **Step 3 — Warn loudly on a `dev-*` build in the same script.** When the version string matches a dev build (e.g. `gh-optivem dev-<sha>`), emit a prominent warning that this run does **not** reflect CI, because CI installs the latest *published release* via `optivem/actions/install-gh-optivem@v1` with `ref` unset. Keep it a **warning, not a hard failure** — validating against a local source build is a legitimate workflow; the defect was silence about it, not the practice.

- [ ] **Step 4 — Fix the stale UNBLOCKED note.** Correct `plans/20260818-1326-compile-all-misses-backend-clean-java.md:9`, which asserts the gh-optivem dependency "has landed" on the strength of commit `f3f003d0`. Restate it in terms of the *published release* that actually gates CI (v1.6.72), since commit-vs-release is precisely the confusion that caused this failure.

- [ ] **Step 5 — Record the rejected alternative.** Note in this plan (and, if it earns a durable home, alongside the install step in `.github/workflows/_meta-prerelease-pipeline.yml:161-165`) that setting `install-gh-optivem`'s `ref` input to build gh-optivem from source is **rejected**: the gate exists to prove shop works with the CLI a student would actually install, and sourcing it from unreleased `main` would make the gate assert something no student can reproduce.

- [ ] **Step 6 — Verify.** Run `bash ./validate-all-gh-optivem-config.sh` locally: the version banner appears, the dev-build warning fires (local CLI is `dev-f3f003d0`), and all 13 configs still report PASSED/PASSED. After v1.6.72 is published, confirm the next `meta-prerelease-stage` run gets past *Validate all gh-optivem configs* to *Read VERSION values*. No `compile-all.sh` sweep and no system tests — this plan changes no product code.

## Notes — evidence behind the diagnosis

- **Failure**: `run / check` → *Validate all gh-optivem configs* → exit 1.
  `ERROR: config: parse gh-optivem-multitier-clean-java.yaml: yaml: unmarshal errors: line 20: field kind not found in type projectconfig.Config / line 26: field component not found in type projectconfig.Config`. Summary: that one config `FAILED / SKIPPED`; the other 12 `PASSED / PASSED`.
- **Pinned cause**: `gh-optivem-multitier-clean-java.yaml:20` (`kind: component`) and `:26` (`component:`), introduced by shop commit `24eb954e` (2026-08-18 16:50). The schema they need lives in gh-optivem `f3f003d0`; `git tag --contains f3f003d0` is empty.
- **Why it stayed hidden**: the validate step is gated on `steps.decide.outputs.should_run == 'true'`. Every scheduled run since 2026-08-18 short-circuited at *Decide whether to run* (e.g. run 32354037636 at 09:28 today never reached the gate). Today's `backend-clean-java` commits were the first "meaningful changes" that let it execute — a latent break, not a new one.
- **Not reproducible locally, and that is the finding**: `gh optivem --version` → `gh-optivem dev-f3f003d0`. `24eb954e`'s "Verified: config validate and config preflight pass" was true against that dev build and false against every released CLI.
- **Parity**: no language twins. Single repo-root config; the other twelve are `kind: system`. `compile-all.sh` and `migrate-all-gh-optivem-config.sh` glob the same files but run locally only, never in CI.
