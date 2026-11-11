# 2026-11-11 07:33 UTC — Plan coordination meta-plan: storefront, reliability, Keycloak cutover, spec gate

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-11T17:24:04Z`

## TL;DR

**Why:** Six active plans overlap: storefront specs/build, the Keycloak cutover, reliability follow-ups and the spec gate. Production login is broken, and some units share cloud resources or plan files, so running them by hand in any order risks confusing results and collisions.
**End result:** Production login works, the storefront is finished with every use case implemented, #29 has an explanation, and the spec gate is back on for every implemented use case. All the in-scope plans are deleted.

## Execution protocol

`/execute-plan` on this file: run the pre-execute checks, read **Wave status**, delegate each unit to a subagent (or a CI dispatch), honour the gates (prod dispatch, cloud cost, local system tests and releases all need the user's approval), update **Wave status**, and stop at the wave's exit gate. Re-run the same command to resume.

## Wave status

| Unit | State |
|---|---|
| U2 KC cutover | **done** 2026-11-11: `multitier-typescript-v1.0.238` in PROD; user signed in on https://shop.optivem.com as `customer1` (shop-prod) via `shop-auth.optivem.com` and returned signed in. KC QA runs 42415102639/42415234619 green. KC plan deleted |
| X1 Track C | effectively done (2026-11-11): C1/C1b/C1c shipped in gh-optivem `7399d6f2`, which is in release `v1.6.108`. `spec validate` in shop gives 12 use cases valid, 35 criteria gated, UC-001 = 16 criteria. Plan moved to `gh-optivem/plans/20261107-1300-...` (shop `248d5e4a`; gh-optivem `628fa21d` is **unpushed**, because the pull is blocked by the user's uncommitted gh-optivem plan edits). C2 needs only the user to confirm v1.6.108 counts as the release |
| U3 REL #29 → #30/#31 | #29 done 2026-11-11. **#30 done** (`actions` `f87d6d5`: poll the post-dispatch run read on 404 only). **#31 done** (.NET `PageClient.WaitForUrlChangeAsync` polls `location.href`; the root cause was a race in the .NET `WaitForURLAsync`, not hydration). **Left:** the `meta-prerelease-stage` re-run (user approved the cost). New REL ideas #32/#33 still need deciding |
| U1 SMETA | user chose a separate session: `/execute-plan plans/20261107-1705-meta-storefront-specs-build.md` |
| U4 UC1 Track D | unblocked (v1.6.108 already reads the layout); per Conflict #3, run it after U1's close-out and recount the criteria then (UC-001 now has 16, not 14) |

## ▶ Next executable step (resume here)

**Goal: confirm `meta-prerelease-stage` is green.** #30 (`actions` `f87d6d5`) and #31 (shop `2c235677`) are fixed. The re-run is **run 43052472314** on `2c235677` (dispatched 2026-11-11; the user approved the cloud cost, so no need to ask again).
1. Check it: `gh run view 43052472314 --json status,conclusion,jobs`. If it's still running, wait in the background (poll at least every 3 min). If it's green, mark U3 done in Wave status. If it's red, triage each failing job into REL as a new numbered idea (don't fix in this meta-plan). Already triaged: `run / local (monolith-dotnet)` red from a runner-side docker pull flake → REL #34.

Also still open, all user-side: pushing gh-optivem `628fa21d` once your gh-optivem plan edits are committed, and confirming v1.6.108 as X1's C2 release. U1 (SMETA) stays in its own session.

## Target state

When every unit here has run:
- **Production login works.** Signing in on `shop.optivem.com` goes through `shop-auth.optivem.com` and comes back signed in. The other five flavors pick up the new issuer on their next normal release. (U2)
- **The storefront is finished and its plans are gone.** UC-006…011 have acceptance tests (UI + API) in Java, .NET and TypeScript, and so do the gaps in UC-002/003/005/012. Every use case UC-001…012 is `status: implemented`. `./test-all.sh` passes for monolith and multitier. SPEC, BUILD and SMETA are deleted. (U1)
- **The cloud-only monolith UI failures are explained.** #29's re-runs, done after the Keycloak cutover, either come back green (treated as cloud UI latency) or have a diagnosis recorded in REL. REL then holds only its deferred ideas and #11 (user-side). (U3)
- **The spec gate is back on.** gh-optivem reads `@UC-NNN.M` Gherkin scenarios in the folder-per-use-case layout and skips `status: planned` (Track C, released). The shop's `spec-gate` step runs for Java, .NET and TypeScript and checks every `implemented` use case, not only UC-001's 14 criteria, at the `spec.yaml` threshold. UC1 is deleted. (X1 → U4)

What the user sees: one `/execute-plan` on SMETA, a short Keycloak sign-off and prod run, one gh-optivem session for Track C, then #29's two cloud re-runs (with your approval for the cost) and finally Track D.

Unchanged: no plan's content is changed by this meta-plan. REL's deferred ideas (#5, #15, #17, #20, #21, #26), `backlog/` and the plans dated before `20261106-1533` are untouched. Nothing adds infrastructure cost apart from #29's re-runs.

**Plans analysed:** 6 in-scope, 2 referenced-only

- In scope (`20261106-1533` onwards):
  - **UC1** `20261106-1533-use-cases-match-substack-articles.md`
  - **SPEC** `20261107-1638-storefront-use-cases-rules-acceptance.md`
  - **BUILD** `20261107-1657-storefront-implementation.md`
  - **SMETA** `20261107-1705-meta-storefront-specs-build.md` (existing meta-plan that drives SPEC + BUILD)
  - **KC** `20261107-1936-keycloak-custom-domain.md`
  - **REL** `20261110-1207-system-test-reliability-followups.md`
- Referenced-only (not in coordination scope):
  - `plans/move-to-gh-optivem/20261107-1300-spec-parse-gherkin-criteria.md` — UC1 Track C (C1, C1b, C1c, C2). It is still in shop's `move-to-gh-optivem/` folder, so it hasn't been moved into gh-optivem or started there yet.
  - `gh-optivem/plans/20261110-1115-system-start-stale-images.md` — SMETA's stale-image fix; it doesn't gate anything (the `--restart` workaround is already the rule).
- Out of scope (sort before `20261106-1533`): `20261103-1049`, `20261104-1148`, `backlog/`, `move-to-event-hub/`.

## Per-plan status snapshot

| Plan | Status | Items done / total | Touched files (primary, remaining work) | Notes |
|---|---|---|---|---|
| UC1 `20261106-1533` | Tracks A and B done; C is external (gh-optivem), D blocked on C | A, B done; C 0/4; D 0/4 | D: `.github/workflows/_acceptance-stage-docker-compose.yml`, `spec.yaml` | D4 still expects "14 criteria, UC-001 only" (see Conflict #3). |
| SPEC `20261107-1638` | **Its file is stale.** Steps 8 and 9 show as unchecked, but they landed in SMETA unit U3: UC-001's narrative now takes a shipping address (4b = address/country), and `storefront-ux.md` links to the specs. | effectively done; Step 10 is a standing rule; the user's review of UC-002…012 is still listed as a gate | none left | The SMETA orchestrator owns edits to this file; it should delete Steps 8 and 9 on its next pass (see pre-execute checks). |
| BUILD `20261107-1657` | Steps 1–6 and 9 done | 7 / 10 | Step 7: none (verify only). Step 8: `system-test/{java,dotnet,typescript}/**` latest acceptance tests + test kit/drivers for UC-006…011, `acceptance-criteria.md` status flips. Step 10: order and coupon acceptance tests + DSL, flips of UC-002/003/005/012 | Driven by SMETA, not run on its own. |
| SMETA `20261107-1705` | Steps 1–5 done | 5 / 9 (Steps 6, 7, 7b, 8 left) | Through BUILD Steps 7, 8, 10; edits SPEC, BUILD and REL (appends new ideas) | Step 6 needs only `./test-all.sh -a multitier` for three languages. |
| KC `20261107-1936` | Steps 1–7 done; Step 8 remainder left | ~7 / 8 | No repo files: CI dispatches (`multitier-typescript-qa-signoff.yml`, `multitier-typescript-prod-stage.yml`) and a browser check | **Production login on `shop.optivem.com` has been broken since the 2026-11-07 cutover** (old issuer). It is user-gated. The RC it names (`v1.0.238-rc.3402`) may have been superseded by newer RCs since. |
| REL `20261110-1207` | Done apart from #29 (next), #11 (user-side) and 6 deferred ideas | 21 decided "do", 19 done | #29: CI re-runs only (`monolith-java-acceptance-stage.yml`, `monolith-dotnet-acceptance-stage.yml`), then this plan file | #29 costs a little in the cloud; ask the user first. |

## Dependency graph

```
[external] Track C (gh-optivem: C1, C1b, C1c, C2 release) ──► UC1 Track D (re-enable spec gate)

SMETA Step 6 (verify multitier) ──► Step 7 (BUILD 8: UC-006…011 tests) ──► Step 7b (BUILD 10) ──► Step 8 (close out SPEC + BUILD + SMETA)
                                                                                                          │
                                                                                                          └─► hands off UC1 Track D (informational)

KC Step 8 remainder (QA sign-off ──► prod stage ──► browser check)          (no plan edges)
REL #29 (re-run 2 cloud acceptance stages ──► triage)                        (no plan edges)
```

Edges come from the plans' own wording: UC1 "D waits for C"; SMETA "Later steps depend on earlier ones" and Step 7b "after Step 7"; SMETA Step 8 "Report UC1 Track D as the remaining follow-up". SMETA says Track D "can land any time after gh-optivem C1, C1b and C1c are released", so it has **no** hard edge on SMETA's close-out. No cycles.

## Conflicts

### 1. `plans/20261110-1207-system-test-reliability-followups.md` — soft conflict (SMETA orchestrator ↔ REL session)
- SMETA's rule "Record incidental issues" appends new numbered ideas to REL after each step.
- A REL session (#29) also edits the file: it updates the resume block and deletes or records #29.
- **Why soft:** they touch different sections (appended ideas vs. #29 and the resume block). It's only unsafe if both sessions write the file without pulling first.
- **Resolution (recommended):** both sessions do a plain `git pull` right before they edit REL, and commit their REL edit on its own. **Why:** the edits don't overlap, so this costs nothing and avoids holding up either track.

### 2. Shared cloud resources — coordination conflict (KC Step 8 ↔ REL #29 ↔ SMETA Step 6 CI evidence)
- KC dispatches QA sign-off and the prod stage for multitier-typescript against the one shared Keycloak (capped at one Cloud Run instance by `0dea9564`).
- REL #29 re-runs the monolith-java and monolith-dotnet cloud acceptance stages. Their failures were UI-only (a Playwright timeout on the smoke test, and an order-status UI check), and monolith UI logins go through the same Keycloak.
- SMETA Step 6 uses `meta-prerelease-stage` run 42885354673 as evidence for multitier.
- **Why it's a conflict:** no files are shared, but if the KC cutover and #29's re-runs hit the shared Keycloak at the same time, a red #29 can't be told apart from load on Keycloak. That defeats #29's "re-run first, then triage" logic.
- **Resolution (recommended):** re-order. KC's prod cutover goes first (production login is broken, so it's the most urgent item), and #29's re-runs are dispatched only once KC's prod stage has finished. **Why:** #29's result then reflects the state after the cutover and isn't confounded by it.

### 3. Spec-gate scope moved under UC1 Track D — coordination conflict (UC1 D4 ↔ SPEC/BUILD status flips)
- UC1 D4 checks for "all 14 criteria (UC-001.1 to UC-001.14)". Since then UC-001 was amended (SMETA U3), and UC-004 and UC-006 are now `status: implemented`. BUILD Steps 8 and 10 will flip UC-002, 003, 005 and 007…012 too.
- **Why it's a conflict:** whenever Track D runs, the gate covers more than UC-001, and the expected count depends on how many flips have landed by then. D4's note already flags this ("the UC-001 count may change").
- **Resolution (recommended):** keep it unordered, as SMETA decided. Whoever runs D4 recounts the criteria of every `implemented` use case at that moment instead of expecting 14. **Why:** Track D is blocked on an external release anyway, and decision 5 (flip a status when its tests land) keeps the gate green whatever the order.

## Consolidation findings (decided)

### 1. SPEC + BUILD + SMETA — no change (already one unit)
- SMETA already makes SPEC and BUILD one orchestrated unit, and it owns their plan edits. SPEC has no work left of its own.
- **Resolution (recommended):** keep it as is, and drive everything through `/execute-plan` on SMETA. **Why:** a second orchestrator would bring back the plan-file collisions SMETA was written to remove.

### 2. REL #29 ↔ SMETA Step 6 — no change
- Both are about `meta-prerelease-stage` going red in the cloud, but #29 is monolith UI and Step 6 is local multitier verification. SMETA's own rule sends incidental CI issues to REL.
- **Resolution (recommended):** keep them separate, with the ordering in Conflict #2. **Why:** merging them would put cloud triage into the storefront orchestrator, which SMETA's rule rules out.

## Execution units (post-consolidation)

| Unit | Plans | Type | Touched files (primary) |
|---|---|---|---|
| U1 | SMETA (drives SPEC + BUILD) | standalone orchestrated run, serial inside | `system-test/**` latest acceptance tests + test kit, `docs/shared/specs/use-cases/*/acceptance-criteria.md` (status flips), SPEC/BUILD/SMETA plan files, appends to REL |
| U2 | KC Step 8 remainder | standalone, user-gated, CI only | KC plan file only |
| U3 | REL #29 (+ #11 user-side) | standalone, CI only | REL plan file (and whatever #29's triage leads to — it would add a new idea, not edit code in this run) |
| U4 | UC1 Track D | standalone, blocked on external Track C | `.github/workflows/_acceptance-stage-docker-compose.yml`, `spec.yaml`, UC1 plan file |
| X1 | `move-to-gh-optivem/20261107-1300` (Track C) | external, in a gh-optivem session | gh-optivem repo only (and removing the file from shop's `move-to-gh-optivem/` once it's moved) |

## Execution waves

### Wave 1 — can start now

**Batch A (parallel-safe, 3 sessions; disjoint files):**
- **U2 — KC prod cutover (user first).** Check whether `multitier-typescript-v1.0.238-rc.3402` is still the latest RC; if a newer one exists, sign that one off instead. Then `multitier-typescript-qa-signoff.yml` → `multitier-typescript-prod-stage.yml` → browser login on `https://shop.optivem.com` (should go via `shop-auth.optivem.com` and come back signed in). Touches only the KC plan file. Mostly user actions; one short session, or do it by hand.
- **U1 — SMETA.** `/clear` then `/execute-plan plans/20261107-1705-meta-storefront-specs-build.md`. It starts with Step 6: `./test-all.sh -a multitier -l <lang>` for dotnet, java and typescript, one at a time, each with `system start --restart` / `system stop` (ideally from a terminal outside Claude Code, per REL #22–25). Then Steps 7 → 7b → 8, serial (shared test kit). It owns `system-test/**`, the spec status flips and the SPEC/BUILD/SMETA plan files.
- **X1 — Track C (external).** In a gh-optivem session: move `plans/move-to-gh-optivem/20261107-1300-spec-parse-gherkin-criteria.md` into gh-optivem's `plans/`, run it (C1, C1b, C1c), and publish a release (C2). It touches no shop files apart from removing the moved plan.

**Batch B (serial after U2's prod stage finishes — Conflict #2):**
- **U3 — REL #29.** Ask the user (it costs a little in the cloud), then re-run `monolith-java-acceptance-stage.yml` and `monolith-dotnet-acceptance-stage.yml` on current `main` and triage as #29 recommends. `git pull` before editing REL (Conflict #1). It can run while U1 is still going.

### Wave 2 — after X1's gh-optivem release

- **U4 — UC1 Track D** (D1 → D4). Never while U1 is in the middle of a step that flips a status: start it between SMETA steps, or after SMETA's close-out (recommended, so the count in D4 is final). Recount the expected criteria across all `implemented` use cases (Conflict #3).

## Pre-execute checks (apply before any wave starts)

- `git fetch && git status -sb` — origin is the source of truth (two machines). The working tree was clean at `d05f005d`.
- `grep -ln "PICKUP\|in-flight\|claimed by" plans/*.md plans/*/*.md` — check that no other session has claimed a unit.
- **SMETA's first pass:** delete SPEC Steps 8 and 9 (they already landed in U3 — UC-001's narrative has the shipping address, and `storefront-ux.md` has the spec links), and refresh SPEC's ▶ Next block. Make this a plan-only commit by the SMETA orchestrator, since SMETA owns SPEC edits.
- `git stash list` — SMETA still flags two old `autostash` entries for review.
- KC: confirm QA runs 42415102639 and 42415234619 (monolith-dotnet and multitier-dotnet) ended green, as KC's resume block asks.

## Out of scope of this meta-plan

- Whether any plan's content is correct (use each plan's refine cycle).
- REL's deferred ideas (#5, #15, #17, #20, #21, #26) and the `backlog/` plans.
- Actually running anything (use `/execute-plan` on each unit).
