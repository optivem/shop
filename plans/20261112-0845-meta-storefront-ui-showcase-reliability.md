# 2026-11-12 08:45 UTC — Plan coordination meta-plan: storefront, Market UI, interview showcase, reliability

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-12T08:55:00Z`

## TL;DR

**Why:** Five active plans from `20261107-1705` onwards are in flight at once: the storefront orchestrator (SMETA), the Market UI redesign (MUI), the interview showcase (SHOW), the reliability follow-ups (REL) and the previous meta-plan (META2). They share no plan items, but three of them need the **one local docker host** for system tests, and two of them have already broken each other's runs that way (REL #36, #43, #45).
**End result:** Every unit runs in a safe order. Docker-bound work runs in one serial lane, and everything else runs in parallel next to it. MUI's own regression (#43) is fixed before its full `test-all` gate. META2 is retired, and this file takes over its last items (UC1 Track D, Track C's C2 sign-off).

## ▶ Next executable step (resume here)

**Top priority (user, 2026-11-12): get the CI pipeline green on `main` before continuing Phase B.** Fixes pushed 2026-11-12: `8d4fd72d` (coupon-list UI tests isolated, ×3), `24723c16` (React pact page-1 interaction), `828af8c0` (TS `hasCustomerFilter` waits for the page), `c71dd2f6` (.NET shipping-address read via API), `d01ea39f` (backend-typescript integration spec vs breaker: this commit stage had been red since `8ee90751`, which is why multitier TS acceptance lacked Retry-After). monolith-dotnet acceptance's red (a071aac8) should be covered by `228991cc` + `38324a28`. **Docker-compose stages: all green (2026-11-12, 20:xx UTC)**: latest ×6 and legacy ×6 on `80fe56c1`, after `81be91db` (.NET xunit `maxParallelThreads` "0.5x"; monolith .NET UI had lost 6 tests to Keycloak-login timeouts under 4-way parallel load) and `80fe56c1` (REL #43 for real: the TS monolith header no longer streams in its own Suspense boundary). **Cloud `meta-prerelease-stage` run 43299156147 (dispatched 2026-11-12, before the fix below), at shutdown:** commit and local stages, drift and most cross-lang jobs green; the 6 cloud `pipeline` jobs were still running. 4 cross-lang jobs red: (a) java tests vs monolith TS: 6 BrowseOrderHistory UI tests, 'UI channel cannot send customer'; (b) dotnet tests vs monolith Java: `AdminCustomerFilterShouldExcludeOtherCustomersOrders` [UI], same error. **Fixed after the run started** by the Java/.NET `OrderHistoryPage.hasCustomerFilter` page wait (latest commit on main, the same fix as `828af8c0`). (c) dotnet tests vs multitier Java: `ShouldCalculateBasePriceAsProductOfUnitPriceAndQuantity` [UI], 'An unexpected error occurred' after 2m26s, likely a flake, not investigated; (d) dotnet tests vs monolith TS: log was not available yet (404 BlobNotFound), so inspect it with `gh api repos/optivem/shop/actions/jobs/<id>/logs`. **Next:** check run 43299156147's final result, including the pipeline jobs, and look at (d); fix any real red; re-dispatch `meta-prerelease-stage` (user approved cloud runs for this pass) and get it green; then resume Phase B below.

**Phase B, the testing pass.** Phase A coding is all on `main`, and so are REL #50, #51, #53, #54 (`d13b4a79`, `9ac5ed87`, `3c6545c0`, `c88c7ddc`).
- **Java pass: done (2026-11-12).** Monolith Java is green. One flaky UC-007.4 result fails the spec gate: 52/53 verified, lenient. On multitier Java, 3 React UI tests hang on browser navigation (REL #61). Fixes: `365de274`, `2963db9b`, `132e7557`, `6b07a16b` (a React `useOrders` product bug), and `a071aac8` (the stub breaker thresholds, under a mislabelled commit message).
- **.NET pass: in progress.** The first attempt was killed when the system ran low on memory. It fixed `109d80f4`, `228991cc` and `38324a28` (Java got the last one too). The local Postgres volumes were wiped on 2026-11-12, and a second attempt was started. That attempt also moves the parallel UI tests that read a coupon back from the list into the isolated classes, in all three languages. **If it never reported:** check `git log` for its commits, then rerun the .NET pass (monolith full acceptance, smoke, contract and e2e, then multitier, one stack at a time) and `spec status --config spec-dotnet.yaml`.
- **Then:** the TS pass (REL #43 fixed, see U2; REL #60 TS breaker, k6 smoke per REL #63) → `./test-all.sh` → flip the use cases that pass to `implemented` → UC1 D4 → the user's MUI visual check → delete MUI, SHOW and this file.
- **Memory is tight.** Use one stack at a time, and keep free RAM at 5 GB or more before starting.
- The checklist of what to verify for each change is in **Phase B checklist** below.

## Target state

When every unit here has run:
- **Storefront finished.** System tests get a fresh Keycloak customer per test (test-kit capability in Java, .NET and TypeScript, stub/local realm only). With it, `@UC-009.10`, UC-010, UC-011 and the UC-002/003/005/012 gaps are covered, and every use case UC-001…012 is `status: implemented`. SPEC, BUILD and SMETA are deleted.
- **Spec gate back on.** Track C is closed on gh-optivem `v1.6.108`. UC1 Track D re-enables `spec-gate` for Java, .NET and TypeScript over every `implemented` use case, with recounted criteria rather than UC-001's old 14. UC1 is deleted.
- **Market UI verified.** The TS monolith renders one cart badge again (#43), Bootstrap is self-hosted in the monoliths (#44), `./test-all.sh` is green for latest + legacy in all three languages, and the user has done the desktop/phone visual check. MUI holds only its deferred Step 2 and follow-up ideas.
- **Interview showcase advanced.** The resilience changes are verified with `--sample`, slow/down-ERP acceptance tests exist in three languages, and k6, the OpenAPI/Stryker parity sweep, the SBOM release assets and the outbox/frontend-testing spin-off plans follow. `docs/tour.md` gets a row for each.
- **Reliability ideas #32–#46 settled.** The `actions` fixes fail loudly on 401/403, show the probe's stderr, and retry the containerd pull once. gh-optivem gets a plan for the stack lock, the commit autostash, the `--test`/`--suite` filter fixes and the local Playwright binary. A1 is scoped to API wire DTOs, and the TS lint baseline is clean. REL is left with only its deferred ideas.
- **What the user sees:** one `/execute-plan` on this file. A plan-only first commit (META2 deleted, decisions written into SMETA/REL/UC1, SHOW's stale marker removed), then the docker lane SHOW Step 3 → MUI → SMETA → Track D, with the `actions`/gh-optivem sessions running next to it. Stops happen only at gates: local system tests, cloud cost, the visual check.
- **Unchanged:** no plan's item content is rewritten, only the decisions above are recorded. REL's deferred ideas (#5, #15, #17, #20, #21, #26), MUI's deferred Step 2, `backlog/` and plans sorted before `20261107-1705` are untouched. Nothing adds infrastructure cost, and the second machine is not used as a docker lane.

## How to run

This is a self-driving parent meta-plan: `/execute-plan plans/20261112-0845-meta-storefront-ui-showcase-reliability.md`. The orchestrator runs the pre-execute checks, reads **Wave status**, gives each unit to a fresh `general-purpose` subagent (never a worktree), keeps the gates (user decisions, cloud cost, prod, local system tests), updates **Wave status**, and stops at each wave's exit gate. Run the same command again to resume.

**Orchestrator rules:**
- **Docker lane:** at most one unit at a time runs `gh optivem system start` / `system-test` / `test-all.sh` on a given machine (REL #36, #45). Before a lane step, check `docker ps` for `my-shop-*` stacks owned by another session.
- **Plan-file ownership:** SMETA owns SPEC/BUILD edits, and each unit edits only its own plan. Anyone appending to REL does a plain `git pull` first and commits the REL edit on its own (carried over from META2 Conflict #1).
- **Incidental issues** go into REL as new numbered ideas (SMETA's rule, which applies to every unit here).

## Wave status

| Unit | State |
|---|---|
| U0 plan-only first pass | **done** 2026-11-12: META2 deleted, SHOW marker removed, decisions written into SMETA/REL, Track C closed in UC1 |
| U1 SHOW | **Phase A done** 2026-11-12 (`3fb4cb7e`…`849c927c`, compile-all green); Phase B: run the resilience tests, k6 smoke, OpenAPI checks; Step 4b blocked by REL #47 |
| U2 MUI (+ REL #43, #44) | coded (#43 fixed 2026-11-12: the header's own Suspense boundary removed from `StoreLayout.tsx`, UC-008.2 UI 5/5 locally, cart UI tests 38/38; `70e174d7` had wrongly said "no code change"; `ae34c9dc` #44); Phase B: test-all + visual check |
| U3 SMETA (+ REL #39, #41/#46, #49, #52) | **coded** 2026-11-12: `aed41283` `5137a909` `30df8177` `b19b17ff` `b6b1f262` `e300e52c` `5ee705f9` `071d2256`; nothing flipped; Phase B next |
| U4 UC1 Track D | D1–D3 coded `f13a365c` (per-language spec-<lang>.yaml, lenient mode); D4 after Phase B |
| U5 REL #11 | user, second machine |
| X1 external | **done**: actions `21972ff` (#32, #33; `v1` tag not moved yet, user to decide), shop `a3bdc9a8` (#34), gh-optivem plan `4ab210be` (#36–#45) |

**Plans analysed:** 5 in-scope, 4 referenced-only

- In scope:
  - **SMETA** `20261107-1705-meta-storefront-specs-build.md`
  - **REL** `20261110-1207-system-test-reliability-followups.md`
  - **META2** `20261111-0733-meta-storefront-reliability-cutover.md`
  - **MUI** `20261111-1814-market-ui-redesign-all-screens.md`
  - **SHOW** `20261111-1831-interview-showcase-skill-gaps.md`
- Referenced-only (not in coordination scope; read so the graph is complete):
  - **SPEC** `20261107-1638-storefront-use-cases-rules-acceptance.md`: only Step 10 is left, and it is a standing rule. SMETA drives it.
  - **BUILD** `20261107-1657-storefront-implementation.md`: Steps 8 and 10 are left. SMETA drives them.
  - **UC1** `20261106-1533-use-cases-match-substack-articles.md`: Track D (D1–D4) is left. Its C1/C1b/C1c boxes are still unchecked even though they shipped in gh-optivem `v1.6.108` (META2 X1). The file is stale, not open.
  - `gh-optivem/plans/20261107-1300-spec-parse-gherkin-criteria.md` (Track C): only C2 is left, a user confirmation.

## Per-plan status snapshot

| Plan | Status | Items done / total | Touched files (primary, remaining work) | Notes |
|---|---|---|---|---|
| SMETA `1705` | Steps 1–6 done; Step 7 part-way (UC-006…009 done, UC-009 not flipped) | 6 / 9 (7, 7b, 8 left) | `system-test/{java,dotnet,typescript}/**` latest acceptance tests + test kit/drivers, `docs/shared/specs/use-cases/UC-0{09,10,11,02,03,05,12}-*/acceptance-criteria.md` (status flips), SPEC/BUILD/SMETA plan files, appends to REL | **Open question:** fresh customer per test (recommended: a Keycloak-admin test-kit capability in three languages). It blocks `@UC-009.10`, UC-010, Step 7b's UC-002 and UC-005. |
| REL `1207` | #1–#31 decided and done apart from #11 (user, second machine) and 6 deferred; **#32–#46 undecided** | 23 done / 25 "do"; 15 new ideas | In-repo: #39 `system-test/typescript/**/keycloak-token-provider.ts`; #41/#46 .NET/TS architecture tests + system-test request DTOs; #43 Next.js monolith layout; #44 monolith page templates (Bootstrap). External: #32/#33/#34 `actions` repo; #36/#37/#38/#40/#42/#45 gh-optivem | Its resume line still asks to "re-run `meta-prerelease-stage` to confirm #30 and #31", but META2 records run 43052472314 green after both. Treat it as done. |
| META2 `0733` | U2 (KC) and U3 (REL #29) done; X1 practically done | 3 / 5 units | Its own plan file only | Its remaining items (U1 = SMETA, U4 = UC1 Track D, C2 confirm, REL #32–35) are all covered by this meta-plan. See consolidation #1. |
| MUI `1814` | Steps 1, 3–8 done; Step 2 deferred; Step 9 half done | compile-all green; `test-all.sh` + visual check left | No file edits planned for Step 9. **#43's fix touches the Next.js monolith layout** (two cart badges since `81566bb0`). | Its own resume block says to run `test-all.sh` only once no other session uses the docker stacks and the UC-009 edits are committed (they are now: `06bec4eb`). |
| SHOW `1831` | Steps 1, 2, 4 done; Step 3 backend done, `--sample` not run, acceptance test not written | 3 / 9 (3, 4b, 5, 6, 7, 8 left) | Step 3: system-test ERP stub driver/DSL + new resilience acceptance tests ×3; Step 4b: new release workflow; Step 5: k6 script + `workflow_dispatch` workflow; Step 6: .NET/TS backend OpenAPI + Stryker config + nightly workflow; Step 7: two new plan files; Step 8: `docs/tour.md` | **Picked up** by an agent on `Valentina_Desk` (2026-11-11T19:45Z). Its backend breaker changes are already on main (SMETA verified the breaker fix `7177bbcc` on Java/.NET). |

## Dependency graph

```
U0 user decisions ──► SMETA Step 7 (fresh-customer unit → @UC-009.10 + UC-009 flip → UC-010 → UC-011)
                       ──► Step 7b (BUILD 10) ──► Step 8 close-out ──► UC1 Track D (D1→D4, recount criteria)
                                                                    ▲
Track C C2 (user confirms v1.6.108) ────────────────────────────────┘

REL #43 fix (Next.js badge) ──► MUI Step 9 (test-all.sh + visual check)

SHOW Step 3 (--sample + resilience acceptance test) ──► Step 5 ──► Step 6 ──► Step 7   (Step 8 runs alongside; 4b free-floating)
```

Edges come from the plans' own wording: SMETA "Later steps depend on earlier ones", "Step 7b after Step 7", the open question "decide at the start of the next session"; UC1 "D waits for C", META2 "run it after U1's close-out and recount"; REL #43 "fix it in the Next.js layout … then re-run UC-008 on the TS monolith", and MUI's gate that `test-all.sh` must pass; SHOW "Execution order: 3 → 5 → 6 → 7". No cycles.

## Conflicts

### 1. Local docker host — coordination conflict (SMETA Step 7/7b ↔ MUI Step 9 ↔ SHOW Steps 3, 5)
- SMETA Step 7/7b runs `--sample` and suite runs for every use case. MUI Step 9 runs `./test-all.sh`, which takes over every stack for a long time. SHOW Step 3 needs `--sample` per language, and Step 5 runs k6 against the local stack.
- **Why it's a conflict:** no source files are shared, but it already went wrong: a parallel session replaced a running stack (REL #36), concurrent `system-test setup` corrupted `system-test/java/build` (REL #45), and one session's uncommitted edits ended up in another's images.
- **Resolution (recommended):** a single serial **docker lane** per machine, in the order SHOW Step 3 → MUI Step 9 → SMETA Step 7 → 7b. **Why:** SHOW's unverified breaker changes are already on main and affect every other run, so verify them first. MUI's `test-all` then checks everything that landed up to that point. SMETA's long tail comes last.

### 2. `system-test/**` test kit and ERP stub driver — soft conflict (SHOW Step 3 ↔ SMETA Step 7 ↔ REL #39, #41/#46)
- SHOW Step 3 adds slow/down ERP stub behaviour (`ErpStubDriver` etc.) and resilience acceptance tests in three languages. SMETA Step 7 adds the fresh-customer test-kit capability plus UC-010/011 tests in the same drivers and DSL. REL #39 edits `keycloak-token-provider.ts`, which the fresh-customer capability will also touch. REL #41/#46 may reshape the UI-driver request DTOs (`ManageCartRequest`, `ViewProductRequest`, `CheckOutRequest`) that SMETA owns.
- **Why soft:** different functions, but in the same driver/DSL files. Parallel sessions would see stale context.
- **Resolution (recommended):** serialise through the docker lane (Conflict #1 already orders SHOW Step 3 before SMETA Step 7). Fold REL #39 and #41/#46 into SMETA Step 7's fresh-customer subagent brief (consolidation #3). **Why:** it touches those files anyway, and #39's own recommendation is "fix it in the next system-test/typescript touch".

### 3. Next.js monolith UI — dependency inversion (MUI Step 9 ↔ REL #43)
- MUI says "markup and test hooks unchanged … acceptance tests stay green", but its Step 7 commit `81566bb0` renders two `[aria-label='Cart Item Count']` badges on the TS monolith (REL #43). UC-008 fails there every time.
- **Why it's an inversion:** MUI's Step 9 gate assumes the regression isn't there. `test-all.sh` can't go green until #43 lands, and #43 lives in REL, waiting for a decision.
- **Resolution (recommended):** consolidation #2. #43 runs inside the MUI unit, before `test-all.sh`.

### 4. Monolith page templates — soft conflict (MUI ↔ REL #44)
- REL #44 self-hosts Bootstrap in the monolith pages, which are the same `<head>`/layout files MUI just re-themed. MUI already self-hosted Figtree the same way.
- **Resolution (recommended):** consolidation #2. **Why:** same files, same pattern, and it removes a CDN flake source before MUI's full `test-all` run.

### 5. Two meta-plans driving the same units — coordination conflict (META2 ↔ this meta-plan)
- META2 still lists SMETA as U1, UC1 Track D as U4, and REL #32–35 as user decisions. This meta-plan covers the same items.
- **Resolution (recommended):** consolidation #1.

## Consolidation findings (decided)

### 1. META2 → retired; this meta-plan takes over its remaining items
- META2's open items are all user-side or pointers to other units: C2 confirm, push gh-optivem `628fa21d`, SMETA, UC1 Track D, REL #32–35.
- **Resolution (recommended):** the orchestrator's first step deletes `plans/20261111-0733-meta-storefront-reliability-cutover.md` (user-approved, plan-only commit) and carries those items here (U0, U3, U4). **Why:** two live meta-plans over the same units would reintroduce the double-orchestration problem META2 itself warned against. Per `[[feedback_new_plan_not_extend]]`, this is a fresh file, not an edit of META2.

### 2. REL #43 + #44 → atomic with MUI Step 9 (unit U2)
- #43 is MUI's own regression. #44 touches the same monolith layout files. Both have to land before MUI's `test-all` gate means anything.
- **Resolution (recommended):** keep both plans as written, and run #43 (then #44, if the user approves it in U0) and MUI Step 9 in one session. That session deletes #43/#44 from REL afterwards (pull first). **Why:** one executor owns the monolith layout files and the gate, so nothing is left red between sessions.
- **Alternative considered:** a separate REL session for #43. Rejected because it would take the docker lane twice for a fix that only matters to MUI's gate.

### 3. REL #39 + #41/#46 → folded into SMETA Step 7 (unit U3)
- The same test-kit and request-DTO files (Conflict #2).
- **Resolution (recommended):** add them to the subagent brief for SMETA Step 7's fresh-customer unit, once U0 approves them. Delete them from REL after that. **Why:** that subagent already edits `keycloak-token-provider.ts` and the driver DTOs, and running them separately would only add another serial pass over the same files.

### 4. SHOW — no change (serial inside)
- Its order (3 → 5 → 6 → 7) is its own.
- **Resolution (recommended):** keep SHOW as written and run it as one unit (U1) through this orchestrator; Steps 3 and 5 take docker-lane slots. **Why:** its internal order is its own, and the 2026-11-11 claim turned out stale (decision #5).

## Execution units (post-consolidation)

| Unit | Plans / items | Type | Touched files (primary) |
|---|---|---|---|
| U0 | Plan-only first pass applying Resolved decisions #1–#5 | orchestrator, plan-only commit | META2 (delete), SHOW marker, SMETA Open question, REL Resolved decisions, UC1 Track C boxes |
| U1 | SHOW Steps 3, 5, 6, 7, 8, 4b | standalone, serial inside (stale claim, decision #5); Steps 3 and 5 use the docker lane | ERP stub driver/DSL + resilience tests ×3; k6 + workflow; .NET/TS OpenAPI + Stryker; `docs/tour.md`; new release workflow; two new plan files |
| U2 | MUI Step 9 + REL #43 (+ #44) | atomic single session, docker lane | Next.js monolith layout (#43); monolith page templates (#44); MUI + REL plan files |
| U3 | SMETA Steps 7, 7b, 8 (drives BUILD 8, 10; SPEC) + REL #39, #41/#46 | standalone orchestrated run, serial inside, docker lane | `system-test/**` latest tests + test kit, `acceptance-criteria.md` flips, SPEC/BUILD/SMETA plan files, REL |
| U4 | UC1 Track D (ref-only plan, carried over from META2) | standalone | `.github/workflows/_acceptance-stage-docker-compose.yml`, `spec.yaml`, UC1 plan file |
| U5 | REL #11 (user, second machine) + any approved REL idea not folded elsewhere | user / small session | REL plan file |
| X1 | REL #32, #33, #34 → `actions`; #36/#45, #37, #38/#42, #40 → gh-optivem | external, one plan per repo | other repos only (plus deleting the moved ideas from REL) |

## Resolved decisions

1. **Fresh customer per test (SMETA's open question)** — user, 2026-11-12: the test kit creates a fresh Keycloak CUSTOMER user per test through the Keycloak admin API (stub/local realm only, Java, .NET and TypeScript). It runs as its own unit at the start of U3, before `@UC-009.10`, the UC-009 flip and UC-010. **Why:** it unblocks UC-009.10, UC-010, Step 7b's UC-002 and UC-005 at once, and removes test coupling through the two shared customers. The U3 orchestrator writes this decision into SMETA's Open question block on its first pass.
2. **REL #32–#46** — user, 2026-11-12: accept every idea's written recommendation. Routing: #43 + #44 → U2; #39, #41/#46 → U3; #32, #33, #34 → X1 `actions`; #36/#45, #37, #38/#42, #40 → X1 gh-optivem; #35 → leave the bootstrap admin as is (record and delete from REL); #40, #34 are "retry / investigate if it recurs" (#34 gets the single retry on the exact containerd message). #41/#46 offers two options, so it is settled here: **exempt UI-driver request DTOs from A1** (scope A1 to API wire DTOs) in .NET and TS. **Why:** the rule's stated intent is wire DTOs, and reshaping UI-driver inputs to strings would only satisfy the rule's letter. The orchestrator writes these into REL's "Resolved decisions" (pull first, own commit).
3. **Track C C2** — user, 2026-11-12: gh-optivem `v1.6.108` counts as Track C's release. Track C is closed, and U4 no longer waits on anything external. Still user-side: push gh-optivem `628fa21d` once the gh-optivem plan edits are committed. U4 ticks C1/C1b/C1c/C2 in UC1 as its first edit.
4. **Retire META2** — user, 2026-11-12: the orchestrator deletes `plans/20261111-0733-meta-storefront-reliability-cutover.md` in a plan-only commit as its first step (consolidation #1).
5. **SHOW claim is stale** — user, 2026-11-12: the 2026-11-11T19:45Z marker on `Valentina_Desk` is not a live session (the tree there was clean). This orchestrator runs U1 through subagents: it removes the stale marker from SHOW, starts with Step 3's `--sample` verification as the first docker-lane step, and then follows SHOW's own order (3 → 5 → 6 → 7, Step 8 alongside, 4b off the lane). Consolidation #4 still holds, minus the "claiming session".

6. **One docker lane** (was Needs-decision #1) — user, 2026-11-12: one serial lane on one machine, in the order SHOW Step 3 → MUI `test-all` → SMETA. The second machine is not used as a second lane (it only does REL #11). **Why:** MUI's gate is only meaningful against a settled `main`, and a parallel run would likely have to be repeated after SMETA lands its tests.

## Execution waves

### Wave 0 — orchestrator's first pass (plan-only commit)

- **U0** (decisions made 2026-11-12, see Resolved decisions): delete META2; remove SHOW's stale pickup marker; write decision #1 into SMETA's Open question block; write decision #2 into REL's "Resolved decisions" (pull first); tick C1/C1b/C1c/C2 in UC1. User-side, whenever convenient: push gh-optivem `628fa21d`.

### Run decisions (user, 2026-11-12, at execute start)

- Blanket yes for local system tests and per-unit commits during this run. Full run, stopping only at gates. X1: fix `actions` #32–#34 directly; gh-optivem items become one gh-optivem plan.
- **All coding first, all testing at the end** (docker is slow). Coding units are verified by compile only (`./compile-all.sh`, or per-project `./gradlew build` / `dotnet build` / `npx tsc --noEmit`, which includes unit tests). All work happens on `main`, and **no branches are created** (user, 2026-11-12). **Each Phase A unit is committed and pushed to `main` as soon as it compiles** (user chose option 1, 2026-11-12). Phase B then tests everything and fixes forward. `main`/CI may be red in between, which the user accepted for speed. Plan-only edits and the `actions` repo push to `main` immediately.

### Wave 1 / Phase A — coding (no docker), serial subagents on `main`

1. **X1 `actions`:** REL #32, #33, #34 (pushed directly; script-only).
2. **X1 gh-optivem plan:** REL #36/#45, #37, #38/#42, #40 → one plan in `gh-optivem/plans/`.
3. **U1 SHOW:** Step 3 slow/down ERP acceptance tests ×3 (written, compiled); Step 5 k6 script + workflow; Step 6 OpenAPI .NET/TS + Stryker; Step 4b SBOM release workflow; Step 7 spin-off plans; Step 8 `docs/tour.md` rows.
4. **U2 MUI:** REL #43 (one cart badge in the Next.js monolith) + #44 (self-host Bootstrap in the monoliths).
5. **U3 SMETA coding:** fresh-customer test-kit capability (+ REL #39, #41/#46) → `@UC-009.10` → UC-010 → UC-011 → Step 7b tests. Status flips stay unflipped until Phase B passes.
- **U5:** REL #11 on the second machine (user).

### Wave 1 / Phase B — testing at the end (one docker lane)

1. Per language: `system start --restart` → `--sample` → the suites for the new tests (SHOW Step 3 resilience; UC-008 on the TS monolith for #43; UC-009.10, UC-010, UC-011, the Step 7b gaps), plus a k6 smoke run against one stack.
2. `./test-all.sh` (latest + legacy, all three languages). This is MUI's Step 9 gate and the final gate for everything in Phase A.
3. Fix failures (up to 2 retries per failure), then flip the use cases whose tests pass to `implemented`, fast-forward `main`, push, and hand the user MUI's desktop/phone visual check.

### Wave 2 — after U3's close-out

- **U4 UC1 Track D:** `/execute-plan plans/20261106-1533-use-cases-match-substack-articles.md`. First tick C1/C1b/C1c as shipped in `v1.6.108`, then D1 → D4, recounting the criteria of every `implemented` use case (META2 measured 35 gated, UC-001 = 16, not 14).
- When U2, U3 and U4 are done and their plans are deleted, delete this meta-plan.

## Pre-execute checks (apply before any wave starts)

- `git fetch && git status -sb`. Origin is the source of truth (two machines).
- `grep -ln "Picked up by agent\|PICKUP\|claimed by" plans/*.md plans/*/*.md`. SHOW's stale marker is removed in U0. Check for any other claim.
- `docker ps --filter name=my-shop`. Before every docker-lane step, make sure no other session's stack is running.
- `git stash list`. SMETA still flags two old `autostash` entries for review.

## Out of scope of this meta-plan

- Whether any plan's content is correct (use each plan's refine cycle).
- REL's deferred ideas (#5, #15, #17, #20, #21, #26), MUI's deferred Step 2 and follow-up ideas, `backlog/`, `move-to-*`, and plans sorted before `20261107-1705` (except the referenced-only ones above).
- Actually running anything (use `/execute-plan` on each unit).

## Phase B checklist

All commits below are on `main`, coded and compiled but never run against a stack.

### SHOW (interview showcase)
- `3fb4cb7e` Resilience: `PlaceOrderNegativeIsolatedTest` `shouldFailFastWithServiceUnavailableWhenErpIsDown` / `...WhenErpIsSlow` (UC-001.14, API only), ×3 languages. These need the backend breaker/timeout changes. Breaker open time in the stub compose files: Java/TS 100 ms, .NET 500 ms. Bounds: ≤5 s down, ≤12 s slow (the stub delay is 20 s).
- `4eb62d6a` k6: `perf/k6/browse-and-place-order.js`. Run a short smoke against one running monolith stack (e.g. the `grafana/k6:1.0.0` image, 1 VU, 10 s).
- `e6f8cf65` OpenAPI: `/v3/api-docs` for signed-in callers on the .NET and TS backends. NestJS `authorization.component.spec.ts` needs docker and was never run.

### MUI (Market UI)
- `70e174d7` #43: the TS monolith must render exactly one `[aria-label='Cart Item Count']`. Run UC-008 on the TS monolith, especially `shouldRaiseTheQuantityOfAProductAlreadyInTheCart`.
- `ae34c9dc` #44: Bootstrap is self-hosted in the Java (`/css/lib/bootstrap/...`) and TS (`/lib/bootstrap/...`) monoliths. Pages must still style and work, including for signed-out visitors.

### SMETA fresh customer + UC-009.10 (`aed41283`, `5137a909`, `30df8177`)
- Keycloak 26.0 user creation (firstName, lastName, email, requiredActions empty), the password grant on `shop-system-test`, the UI login as the new user, and users deleted on close.
- Role lookup via `role-mappings/realm/available` on localhost.
- Prefill of first and last name (Ana Novak) on the first-order form in all four UIs.
- Unticking "Save as Default Address" works in every UI. Unticked saves nothing, and the profile then shows empty address fields.
- Tests: `CheckOutNegativeIsolatedTest` UC-009.10 ×3.

### UC-010 (`b19b17ff`)
- Profile page readiness and field reads in all four UIs. React and Next.js load the profile asynchronously and render the email `readonly disabled`. Thymeleaf's tax note contains newlines.
- The profile save notification comes as `field: message` with API field names (e.g. `postalCode`). The empty-country case selects the `""` option.
- UC-010.6: after signing in from `/account`, every UI returns to `/account` (React especially).
- UC-010.5 Quick order: a GB order needs the tax stub's built-in GB rate, and the UI country field keeps its prefill.

### UC-011 (`b6b1f262`)
- Every admin order-history browse waits for the customer filter to load. Watch for slowdowns and timeouts.
- Races between the reload after choosing a customer and the reload after a reset, in the monoliths (no stale-response guard: REL #50).
- The admin customer-only filter on customer1 (many shared orders): the new order must land on page 1.

### Step 7b (`e300e52c`)
- `placedAt` must drive `orderTimestamp` on all six backends. The Next.js monolith's `now` source was not traced.
- Order-history UI parsing in all four UIs: "Order Date" as `M/d/yyyy, h:mm:ss AM` (narrow/NBSP before AM/PM is handled) and "Total Price" as `$99.00`.
- The lower-case partial order-number filter through the UI search.
- Cancel/Deliver offered-or-not detection, read right after the details page loads.
- Isolation: the `*PositiveIsolated` classes rely on nothing else publishing coupons or changing the clock at the same time.
- TS coupon setup change (`arrangeGiven declaredOnly`): UC-005.5, UC-005.6 and the plain browse must still pass.
- Every backend reports `page` and `size` in the order-history response (UC-002.3).

### REL #49 (`5ee705f9`)
- Checkout acceptance tests using `confirmsOrdersFor` / `orderFor(...)` on UI and API in Java and .NET (`CheckOutPositiveIsolatedTest`, `CheckOutNegativeIsolatedTest`). Results should be unchanged.

### REL #52 (`071d2256`)
- The new UC-012.3 UI test. UC-012.4 on UI (a customer finds the button missing, reported as permission denied). UC-012.1–.3 on UI as admin (the button still renders and works).

### Track D (`f13a365c`)
- After each language's acceptance run, `gh optivem spec status --config spec-<language>.yaml` must find the JUnit files at the configured globs (.NET: `system-test/dotnet/SystemTests/TestResults/junit/*.xml`). Report Verified/Failing counts per language for the 53 gated criteria.
