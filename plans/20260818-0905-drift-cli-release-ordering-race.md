# 2026-08-18 09:05:37 UTC — Recover drift run 32118470425 and prevent CLI/config release-ordering races

## TL;DR

**Why:** Scheduled drift run [32118470425](https://github.com/optivem/shop/actions/runs/32118470425) failed both jobs at `gh optivem system-test setup` with `system-test.paths.domain-value-types required`. This is not a shop defect — shop's configs were renamed to the new key `common-domain` ~4 minutes *before* the gh-optivem release that teaches the CLI that key was published, and `drift.yml` installs the latest published CLI release.

**End result:** The drift workflow is green again (re-run picks up gh-optivem v1.6.69), and the ordering rule that would have prevented the gap — release the CLI *before* landing the matching shop config edit — is written down in gh-optivem's `path-keys.md` doctrine.

## Outcomes

What we get out of this — the goals and deliverables:

- Drift run 32118470425 (or a fresh drift run) is green on both `monolith — Java → TS schema interop` and `multitier — Java → TS schema interop`.
- Confirmation on record that no shop file needed changing: all 12 `gh-optivem-*.yaml` configs already carry the correct `common-domain` key and validate clean.
- A written release-ordering rule in gh-optivem doctrine: a rename or addition of a **required** config key must ship as a published CLI release **before** the matching shop config edit lands, because every consumer workflow installs the latest release and breaks in the gap.
- The next person who renames a Family B path key finds that rule where they are already reading (`internal/kernel/projectconfig/path-keys.md`), not in a postmortem.

## ▶ Next executable step (resume here)

Re-run the failed drift jobs and confirm they go green:

```bash
gh run rerun 32118470425 --repo optivem/shop --failed
```

Then poll (sleep ≥2 min between checks, per CLI conventions) with
`gh run view 32118470425 --repo optivem/shop` until both jobs report success. The re-run installs gh-optivem **v1.6.69** (published 2026-08-18T08:56:15Z, the first release containing the `common-domain` rename), so the config validation that failed at 08:52:36Z now passes. If a newer scheduled drift run has already gone green in the meantime, record that as the verification instead and skip the re-run. This unblocks Step 3 (the doctrine note).

## Context — root cause

| Time (UTC) | Event |
|---|---|
| 08:47:38 | gh-optivem `261cf54a` renames path key `domain-value-types` → `common-domain` (`internal/kernel/projectconfig/paths_defaults.go:171`) |
| 08:47:56 | shop `739a65d8` renames the key in all 12 `gh-optivem-*.yaml` configs (e.g. `gh-optivem-monolith-java.yaml:27`, `gh-optivem-multitier-java.yaml:33`) |
| **08:52:36** | **drift run 32118470425 executes** — `.github/workflows/drift.yml:105-108` uses `optivem/actions/install-gh-optivem@v1` with no `ref`, i.e. **latest published release** = **v1.6.68** (2026-08-17T23:47:42Z, pre-rename) → config validation fails on both jobs |
| 08:56:15 | **v1.6.69 published** — first release containing the rename (`git tag --contains 261cf54a` → `v1.6.69`) |

**Reproduction:** did *not* reproduce locally. All 12 configs return `is valid` from `gh optivem config validate` against the locally installed CLI dev build at gh-optivem HEAD (same content as v1.6.69).

**All languages checked:** `common-domain` appears exactly once in each of the 12 configs (java / dotnet / typescript × monolith / multitier × latest / legacy). No language was missed; no shop config edit is required.

**Classification:** transient CI/infra failure from cross-repo release ordering — self-healing once v1.6.69 is the latest release.

## Steps

- [ ] Step 1: Re-run the failed jobs — `gh run rerun 32118470425 --repo optivem/shop --failed`. (Skip if a later scheduled drift run has already gone green; note which run is the evidence.)
- [ ] Step 2: Verify the re-run is green on both jobs via `gh run view 32118470425 --repo optivem/shop` (wait ≥2 min between polls). Confirm the `Setup Java test harness` step passes and the downstream smoke-stub steps run.
- [ ] Step 3: Add the release-ordering doctrine note to the sibling repo at `<academy>/gh-optivem/internal/kernel/projectconfig/path-keys.md` (resolve the path dynamically — do **not** hardcode a local root). A short subsection near the existing migrate/back-fill doctrine, stating: renaming or adding a **required** canonical path key is a breaking config-schema change; publish the gh-optivem release carrying the new key **first**, then land the consumer config edits (shop's 12 `gh-optivem-*.yaml`). Cite this incident (drift run 32118470425, v1.6.68 → v1.6.69 gap) as the worked example in one line.
- [ ] Step 4: Re-validate all 12 shop configs as a final sanity check — `for f in gh-optivem-*.yaml; do GH_OPTIVEM_CONFIG=$f gh optivem config validate; done` — expect 12 × `is valid`.
- [ ] Step 5: Commit the doctrine note in the **gh-optivem** repo (separate repo from this plan) via the `/commit` skill, and delete this plan file from shop once complete.

## Notes

- **No shop source or config change.** `compile-all.sh` and `--sample` system-test runs are not needed — nothing in `system/**` or `system-test/**` is touched.
- **Cross-repo plan.** This plan lives in shop (where the failure surfaced) but Step 3's only edit is in the sibling `gh-optivem` repo. Commit each repo separately.
- **Rejected alternative:** adding a deprecated-alias grace period in the CLI validator (accept both `domain-value-types` and `common-domain` for a window). Rejected because an alias leaves permanent cruft in a repo students clone as a template, and required-key renames are rare enough that an ordering rule is the cheaper guard.
