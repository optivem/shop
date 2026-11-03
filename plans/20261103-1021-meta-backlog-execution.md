# 2026-11-03 10:21 UTC — Plan coordination meta-plan: backlog execution

> 🤖 **Picked up by agent** — `531392d34340` at `2026-11-03T10:42:45Z`

## TL;DR

**Why:** The 18 backlog plans overlap on the same files and contracts, several are already done or obsolete, and most are blocked on user decisions, so they can't safely be picked up in isolation while the cloud deploy plan is active.
**End result:** The backlog runs in a safe order: obsolete plans deleted, the two money plans merged, autonomous work done in two waves, and decision-gated work following once the user answers Q-A..Q-H, with no conflicting edits or docker clashes.

## ▶ Next executable step (resume here)

U1-U4 are done and on main (frontend-react is on Vite 7, Vitest 3 and React 19; run `npm ci` there after pulling). U4 item 3 was verified on 2026-11-02: compile-all 7/7, TS legacy sample 26/26, multitier java/dotnet/ts targeted and sample 11/11, monolith java sample 11/11.

Next (2026-11-03): **Step M2**. M1 and M3 are done. M2's proof is blocked until `gh-optivem-multitier-java.yaml` validates again: the `inner-loop:` key from `cf2869ad` needs a gh-optivem release that supports it, or the key has to be removed. Q-A..Q-H were answered on 2026-11-03 (see "User decisions (2026-11-03)"), and the whole meta (M1–M5) runs autonomously.

Follow-ups found during the run (not fixed) are in `plans/backlog/p2-product-correctness/20261103-1336-meta-run-followups.md`. The removed inner-loop keys get re-added via `plans/backlog/p1-ci-pipeline/20261103-1335-readd-inner-loop-pilot-config.md`.

Scope decisions for the run: the `20261030-1822-template-cloud-deploy-all-variants` plan is another agent's, so stay out of its files; push each verified unit, unless a cloud run is in progress; U5-U12 stay blocked on Q-A..Q-H (confirmed 2026-10-31). Commit with `--paths`, and don't use `git pull --autostash` while another agent shares the checkout. Commit plan edits before running `gh optivem commit`: an uncommitted plan file blocks its `pull --rebase`.

Read-only analysis of `plans/backlog/p1..p4`. No existing plan was edited. Repo state was spot-checked (grep/ls) to find stale premises.

## Steps (execute via this meta-plan)

`/execute-plan` on this file runs these steps in order. Each step executes its child plan through a subagent that follows the `/execute-plan` rules for that child: its own gates, its own pre-approvals, item deletion and the child's ▶ block. Delete an M-step here once its child scope is verified done; if the child plan is fully done, the child plan file is deleted too. Then refresh the ▶ block above to name the next M-step. Child plans stay the source of truth for details, so don't copy their items here.

- [x] Step M1 — **P1 Playwright upgrade:** done 2026-11-03 (`5c02fc56`, plus `16012a4e`, which fixes stale cross-language docker images and moves `actAs` out of the operation port). Proven by `monolith-typescript-acceptance-stage` run 41638128462 and `monolith-dotnet-acceptance-stage` run 41638134931, both green on 26.04 with no real chromium-support errors. `meta-prerelease-stage.yml` run 41637810423 is still red, but not because of Playwright: `cf2869ad` added `inner-loop:` to `gh-optivem-multitier-java.yaml`, and gh-optivem v1.6.105 rejects it (`field inner-loop not found in type projectconfig.TierSpec`).
- [ ] Step M2 — **P2 single-pipeline, Step 2 only:** execute Step 2 of `plans/20261102-1855-single-pipeline-deploy-target.md` (multitier-java acceptance tracks). Carry the `@1.64.0` legacy Playwright pin from M1 into `_acceptance-stage-docker-compose.yml`. Gate: Docker latest and legacy green with no tag. The one manual cloud run creates `multitier-java-v<x.y.z>-rc.N`; that RC tag and a few cents of GCP are expected. Step 3 builds on its result; per the 2026-11-03 decisions, continue to M3/M4 without a review stop.
- [x] Step M3 — **P3 GCP-script sync:** agent work landed in `33c832c8` (2026-11-03). The child plan stays because 4 manual checks remain for the user (devcontainer rebuild, Windows `git push`, `teardown-gcp.sh` dry run after `gcloud auth login`, optional `setup-gcp.sh` re-run); see its ▶ block. Local gcloud credentials had expired, so the teardown dry run couldn't be automated.
- [ ] Step M4 — **P2 single-pipeline, Steps 3–9:** resume that plan after M2 and M3 land. Steps 4 and later delete tags, releases, GHCR images and environments; that is pre-approved with guards (see "User decisions (2026-11-03)").
- [ ] Step M5 — **Backlog U5–U12:** Q-A..Q-H were answered on 2026-11-03 (see "User decisions (2026-11-03)"). U5 `cross-language-parity-fixes` is **done** (`c16a196e`..`fa80388c`, 2026-11-03; the plan is deleted). Run the rest in this order: U6 (`/create-plan` the merged `money-strings-end-to-end` from #3 + #5, execute it, then delete #3 and #5) → U7 (#7 steps 1.4/1.5) → U9 (#6 remainder) → U10 (#1 then #2, both shop-only) → U12 (p3, narrow). U8 is **deferred**, and U11 is **manual for the user**. Before each child plan runs, write the relevant decisions below into it as resolved, so its own pre-flight gate passes.

## User decisions (2026-11-03): run the whole meta autonomously

The user asked for every question up front so that M1–M5 can run without stopping. These answers override the "Blocked on user" recommendations wherever they differ.

- **Scope:** M1 → M2 → M3 → M4 → M5, all autonomous.
- **M2's** manual cloud run, its RC tag and the few cents of GCP it costs are expected; no review stop is needed before M4 (the user pre-approved M4).
- **M4 (P2 Steps 3–9) destructive actions are pre-approved, with guards.** That covers deleting the `multitier-java-cloud-v1.0.235` tag and release, local `*-cloud-v*-rc.*` tags, cloud-version GHCR images and the GitHub environments left unused (e.g. `multitier-java-qa-cloud`), plus manual cloud QA/prod runs and the billing kill-switch. Guards: delete only after that flavor's cutover is green in CI; log every deleted item in the final report; if GHCR pulls fail, revert the switch commit and stop (P2's own rule).
- **M3 Step 5 manual checks:** automate `bash -n` and `./teardown-gcp.sh < /dev/null` (never confirm). Rebuilding the devcontainer and `git push` from a Windows host go to the user as end-of-run verification; they don't block M3.
- **No changes to `optivem/actions` in this meta.** Anything that needs that repo goes into a separate plan that says so: `plans/backlog/p1-ci-pipeline/20261103-1042-optivem-actions-retry-release.md` (the `v1` re-point for `cd0432c`). That plan is outside M5.
- **Q-A:** pursue narrowly, accepting every sub-recommendation (P3a: A; P3b: reject 3, drop 4, then delete #10 and #13; P3c: A, then delete #16; P3d: #12 Steps 1–2 only, after U6). Delete #15 now, and #9 and #11 after a `/review-plan` existence check.
- **Q-B:** accept all four (delete the numeric DSL overloads; every decimal field, rates included, becomes a string; one coordinated change set; `--sample` system tests pre-approved).
- **Q-C:** accept all three (explicit rounding mirroring `OrderPricing`; conditional UPDATE, verify-only after U1; the 500 detail text is "An unexpected error occurred. Please try again later.").
- **Q-D:** accept all five (idempotency per user with a 24 h TTL; the same key with a different body returns 422; optional `page`/`size`, default 50, cap 100, no UI paging; search indexing as is, documented; integration tests for races).
- **Q-E:** **defer U8** (tax-country); leave its plan in the backlog.
- **Q-F:** accept all five (actuator/swagger stay authenticated; no auth on the simulators; non-breaking `npm audit fix` only; close the .NET flake bullet; defer the route table and JWKS retry).
- **Q-G:** #1 uses **shop-local `.github/actions/`** per-concern actions (decisions written into that plan). #2 uses a job-summary warning, applies the same policy to acceptance stages and stays shop only (written into that plan; its Steps 1 and 3 moved to the optivem/actions plan).
- **Q-H:** #17 SonarLint stays a **manual task for the user**; keep the plan.

## Active top-level plans (added 2026-11-03)

These plans live in `plans/`, not `plans/backlog/`, and take priority over every backlog unit below.

| ID | Plan | State | Files it owns |
|---|---|---|---|
| P1 | `20261103-1006-upgrade-playwright-ubuntu-26` | Not started; all decisions resolved; Step 6 (push + dispatch `meta-prerelease-stage.yml`) pre-approved | `system-test/typescript/{package.json,package-lock.json}`, `system-test/dotnet/{Common,Driver.Adapter,SystemTests}/*.csproj`, `system-test/java/build.gradle`, the `npx playwright@` pins in `{monolith,multitier}-java-acceptance-stage-legacy.yml` (`:218`, `:224`), plus any pins found by its Step 4 sweep |
| P2 | `20261102-1855-single-pipeline-deploy-target` | Paused before Step 2 (naming confirmed 2026-11-03; no marker) | `*-acceptance-stage*.yml`, `*-{qa,prod}-stage*.yml`, `_acceptance-stage*.yml`, `_prerelease-pipeline.yml`, `_deploy-stage-cloud.yml`, `terraform/`, `setup-gcp.sh` |
| P3 | `20261102-2020-sync-gcp-cloud-scripts-from-event-hub` | Not started | `setup-gcp.sh`, `teardown-gcp.sh`, `terraform/**`, `deploy-config.yml` |

**Order: P1 first, then P2 Step 2.**
- **P1 → P2 (hard).** `meta-prerelease-stage.yml` has been red since 2026-11-02 10:33, because .NET and TypeScript can't install chromium on Ubuntu 26.04. P2 Step 2 can't be proven until the Docker acceptance gates are green.
- **P1 / P2 file collision.** P1 Step 3 edits `multitier-java-acceptance-stage-legacy.yml:224`, and P2 Step 2 `git mv`s that file to `multitier-java-acceptance-stage-docker-compose-legacy.yml` and rewrites it as a thin caller. Landing P1 first means P2 carries the `@1.64.0` pin into the new reusable `_acceptance-stage-docker-compose.yml` and doesn't lose it. If P2 goes first instead, P1 must re-target its pin edits to the new files. The same applies later to `monolith-java-acceptance-stage-legacy.yml:218` during P2 Step 5.
- **P2 / P3 (soft).** Both touch `setup-gcp.sh` and `terraform/` (P2 Step 6 adds the kill-switch and GHCR images; P3 merges event-hub fixes). Serialise P3 before P2 Step 6, so the kill-switch is built on the merged scripts. They are independent of P1.
- **P1 vs backlog.** P1 changes only dependency versions in `system-test/*`, not driver code, so C12 (U4 driver edits) is unaffected. It holds the docker lock for its Step 5 `--sample` gates, so the shared-resource rule applies.

## Plans analysed

18 in-scope plans (all read in full) + `plans/backlog/README.md`. Referenced only: the active cloud plan `plans/20261029-1316-deploy-shop-cloud-run.md` (marked "Picked up by agent"; Step 7 = user provisioning, Step 8 = first cloud run). `after-cloud-deploy/*` and `parked/*` were not read and are excluded.

Cloud-plan footprint (treated as owned): `*-cloud.yml` workflows, `terraform/`, `setup-gcp.sh`/`teardown-gcp.sh`, `docker/keycloak/Dockerfile.cloud` + `docker/keycloak/shop-realm.json` (realm import with URL substitution), cloud migration/stub Dockerfiles, `system-test/typescript/config/test-config-<env>-<mode>.json`, Neon/Secret Manager config.

## Per-plan status snapshot

Done/total = checkbox items. "Premise" = repo check on 2026-10-29.

| # | Plan (slug) | Folder | Done/total | Autonomy | Premise check |
|---|---|---|---|---|---|
| 1 | composite-action-for-stage-checks | p1 | 0/5 | needs-decision (OQ-A/B/C) | Valid: `_prerelease-pipeline.yml` `local` job still has inline lint steps. Parent plan `20260624-1049` no longer exists (assume landed) |
| 2 | sonar-outage-survives-commit-stage | p1 | 0/4 | needs-decision (policy 1-3) | Valid; most work is in sibling `optivem/actions` (exists at `../actions`). No Sonar step in any `*-cloud.yml` |
| 3 | string-only-money-surface | p2 | 0/5 items | needs-decision ("is breaking change acceptable") | Valid: `double` overloads and `Converter.fromDouble` still present in both Java testkits |
| 4 | cross-language-parity-fixes | p2 | 0/3 + 3 OQ | needs-decision (3 OQs, "resolve with /refine-plan") | Valid: `Internal server error:` still in 6 handlers; `between 22:00 and 23:00` still in all 7 backends; no `@Transactional`/atomic redeem in monolith/backend-java |
| 5 | money-wire-format-strings | p2 | 0/8 + 3 OQ | needs-decision | Valid: `ThenOrder` still has `double` |
| 6 | keycloak-auth-followups | p2 | 0/10 bullets | mixed (see units) | `SecurityConfigTest` exists in `monolith/java` and `backend-java` |
| 7 | production-readiness-chunk1-data-correctness | p2 | 0/6 steps (~25 sub-items) | mixed: 1.1/1.2/1.3/1.6 ready, 1.4/1.5 need decisions | Valid: no `@Version`, no idempotency table, migrations are only 3 files |
| 8 | tax-country-from-customer-location | p2 | 0/7 | needs-decision ("not yet approved to build") | Valid |
| 9 | component-pact-layer-other-multitier-backends | p3 | 0/6 | OBSOLETE (likely) | `backend-dotnet/Tests/Contract/BackendPactVerificationTest.cs`, `Tests/Component/`, `backend-typescript/test/pact/backend.pact.spec.ts`, `test/component/` exist |
| 10 | component-stub-contract-beyond | p3 | 0/2 open items (3, 4) | needs-decision ("discuss before executing") | Item 4 largely overtaken: Erp/Tax/Clock `*ParityContractTest` pairs now exist |
| 11 | orderhistory-systemtest-dsl-parity | p3 | 0/n | OBSOLETE (likely) | system-test Java has `ThenOrderHistoryImpl`, `WhenBrowseOrderHistoryImpl`, `BrowseOrderHistory{Positive,Negative}Test`; .NET has `ThenSuccessOrderHistory`; TS only partly verified |
| 12 | backend-testkit-alignment-cross-language-mirror | p3 | 0/4 | needs-decision (gated; Step 3 on string-only) | Java side shipped; `ErpDriver.returnsProduct` present |
| 13 | component-stub-contract-cross-language-mirror | p3 | 0/4 | needs-decision (payload defined by #10) | Depends on #10 outcome and #9 |
| 14 | legacy-twin-stub-fidelity-gap | p3 | 0 (options A/B/C) | needs-decision ("discuss before executing") | Valid: "byte-identical" prose still present in legacy `BackendPactVerificationTest` |
| 15 | tax-real-parity-contract-test | p3 | 0/5 work items | OBSOLETE (done) | `TaxRealParityContractTest`, `TaxStubParityContractTest`, `ClockRealParityContractTest`, shared `ExternalSystemReal.java` and `externalSimulatorImage` Gradle task all exist; `test-taxonomy.md` lists `{clock,erp,tax}` parity |
| 16 | integration-dsl-facade-symmetry | p3 | 0 | needs-decision (OQ1 "no recommendation") | Valid: `GatewayDsl` does not exist |
| 17 | sonarlint-backend-init-failure | p4 | 0/6 | needs-user (local IDE, not code) | n/a, machine-local |
| 18 | frontend-major-upgrades | p4 | 0/1 | autonomous-ready after `/refine-plan` (no user decision) | Valid: React ^18.2, Vite ^5.4, Vitest ^2.1.9 |

## Dependency graph

Edges only from explicit plan language, plus a few conservative sequencing edges (listed below the graph). `A --> B` = A lands before B.

```mermaid
flowchart LR
  PRA["#7 steps 1.1/1.2/1.3/1.6"] -->|coupon race becomes verify-only| PAR["#4 parity-fixes"]
  PAR -->|contract regen first| WIRE["#5 money-wire-format"]
  STR["#3 string-only-money"] -->|sequence before| WIRE
  STR -->|resolve before Step 3| MIR["#12 testkit mirror"]
  BEY["#10 stub-contract-beyond"] -->|payload defines| MIR2["#13 stub-contract mirror"]
  PACT["#9 component-pact-other"] -->|prerequisite| MIR2
  PRA --> PRB["#7 steps 1.4/1.5"]
  COMP["#1 composite-action"] -->|same commit-stage files| SON["#2 sonar-outage"]
  CLOUD[("active cloud plan")] -.->|blocks| TAX["#8 tax-country"]
```

- Declared hard edges: #3 -> #12 Step 3 (#12: "Resolve that plan before running Step 3"); #3 before #5 (#3 banner "sequence before the mirror"; #5 OQ2 couples them); #10 -> #13 ("payload defined by"); #9 -> #13 ("prerequisite for this one").
- Conservative edges added (not explicit; avoid same-file edits): #7(1.2) -> #4 coupon race; #4 -> #5 for `contracts/frontend-backend.json`; #1 -> #2 for commit-stage workflows.
- Cycles: none. Note #3 Item 4 edits the text of plan #12 (a plan-to-plan write; handled by merging, CS1).

## Conflicts

| ID | Class | Files | Plans / items | Resolution |
|---|---|---|---|---|
| C1 | hard (duplicate scope) | coupon read-then-write in `monolith/java`, `monolith/dotnet`, `backend-java`, `backend-dotnet` (`CouponService`/`OrderService`) | #7 step 1.2 vs #4 "Coupon usage race" | Run #7 first; #4's race item becomes verify-only (#4 already says "if any backend turns out already fixed, the item shrinks") |
| C2 | soft (same files, different regions) | `OrderService` (all 7 backends), `GlobalExceptionHandler` | #7 1.1/1.3, #4 blackout text + 500 detail, #8 item 3 | Serialise: #7A -> #4 -> #8 |
| C3 | hard | `contracts/frontend-backend.json` | #4 blackout (line ~888), #5 bodies+matchers, #8 item 4, #7 1.3.e/1.5 | Serialise: #7 -> #4 -> money -> #8. Pact provider verification runs in 3 backends, so each contract change lands with its backend edit in one commit |
| C4 | hard (merge-worthy) | `system-test/java` money assertions + verification layer; `ThenOrder`/`ThenOrderImpl` etc. | #3 vs #5 | Merge into one plan (CS1) |
| C5 | dependency | #12 Step 3 numeric-vs-String surface | #3 -> #12 | #12 Steps 3-4 stay blocked until #3 lands |
| C6 | claimed collision NOT confirmed | backend-java legacy twins (`component/legacy`, `integration/legacy`, `contract/internal/frontend/legacy`) vs String-only money | #14 vs #3 | An earlier meta-plan claimed a collision. Verified: legacy twins hand-write raw WireMock JSON and contain no numeric DSL money calls (the only numeric hit is the non-legacy `ErpStubConsumabilityContractTest`). No real collision |
| C7 | soft | `testSupport` DSL (`UseCaseDsl`, `ThenErp`, `ErpStubDriver`), `BaseGatewayIntegrationTest`, `docs/atdd/test-taxonomy.md` | #10 item 3, #16, #14, #12, #3 | All p3 plans blocked; if pursued, serialise: #14 -> #10 -> #16 -> #12 -> #13. The earlier-claimed `component-tests.yaml`/`build.gradle` collision is gone: its source, #15, is already done |
| C8 | soft vs active cloud plan | `docker/keycloak/shop-realm.json` (cloud Keycloak imports it) | #8 item 1 (realm users/attributes) | Do not schedule #8 until the cloud plan finishes |
| C9 | coordination vs active cloud plan | `system/db/migrations` (Flyway runs against Neon via the cloud migration image) | #7 1.3.a (new `version` column), #7 1.4 (`idempotency_keys`) | Additive expand-only migrations are safe; tell the cloud plan owner. Do not push migration commits while Step 8 is being debugged without user OK |
| C10 | coordination vs active cloud plan | frontend-react / backend-java images consumed by cloud deploy; `system-test/typescript/config` | #18, #7, keycloak-B (U4) | Cloud Step 8 may fix failures in `system-test/typescript/config`; keep U4's TS driver edits out of `config/` |
| C11 | soft | `*-commit-stage.yml` | #1 (compile/lint/unit/component), #2 (Sonar steps) | Serialise #1 -> #2; no merge needed (#1 scopes Sonar out) |
| C12 | soft | `system-test/*` drivers (`MyShopUiDriver`, `MyShopApiClient`) | U4 (keycloak), #3, #11 | U4 before money; #11 is obsolete |
| C13 | soft | `frontend-react` | #18, #5 (types), #8 (order form), #7 1.5.c (paging) | #18 first; the others are blocked anyway |

Shared-resource rule (not a file collision): `gh optivem system start` binds fixed docker ports and reuses cached images (stale images break UI tests). Only one session at a time may run it.

## Consolidation findings (decided)

| ID | Finding | Decision | Rec. |
|---|---|---|---|
| CS1 | #3 and #5 are one theme, overlap on system-test money assertions, and #3 Item 4 edits another plan's text | **Merge into a fresh plan `money-strings-end-to-end`** via `/create-plan` (Phase 1 = #3 Items 1-5 in Java; Phase 2 = #5 wire format: all backends, contract, 3 suites, frontend). Delete #3 and #5 once it lands. Alternative: keep both, re-order. Recommended: merge, because both plans' open questions (breaking change, rates, one-commit-vs-staged) are answered together and the merge removes the plan-to-plan edit | **recommended** |
| CS2 | #7 step 1.2 and #4 coupon-race item are the same fix in the same files | Re-order, no merge: #7A first, then #4 with the race item verify-only. Alternative: merge #4 + #7A (larger, and #4 still has 3 unresolved OQs). Recommended: re-order, because #7A has no open questions and #4 already tolerates "already fixed" | **recommended** |
| CS3 | #13 payload is defined by #10, which may ship nothing | Re-order: decide #10 first; if nothing ships, delete #13. Not merged | |
| CS4 | #9, #11, #15 are done or obsolete | **Delete** (below). Run `/review-plan` on #9 and #11 first because only existence checks were done | **recommended** |
| CS5 | #1 and #2 touch the same commit-stage workflows | Re-order #1 -> #2, no merge (disjoint steps) | |
| CS6 | #7 mixes ready items (1.1/1.2/1.3/1.6) and decision items (1.4/1.5) | Partial execution: `/execute-plan` scoped to 1.1, 1.2, 1.3, 1.6; leave 1.4/1.5 in the file. Alternative: split via `/create-plan`. Recommended: partial execution, because the plan itself says 1.1-1.3 need no API change and lists that order | **recommended** |

Deletion candidates (already done / obsolete):
- `plans/backlog/p3-test-parity/20260814-1610-tax-real-parity-contract-test.md` — confirmed done.
- `plans/backlog/p3-test-parity/20260616-0830-component-pact-layer-other-multitier-backends.md` — likely done, verify.
- `plans/backlog/p3-test-parity/20260717-1020-orderhistory-systemtest-dsl-parity.md` — Java and .NET done; verify TypeScript.

## Execution units

A = autonomous-ready, B = needs user decision (see Blocked on user).

| Unit | Plans / items | Type | Primary files | Autonomy |
|---|---|---|---|---|
| U1 | #7 steps 1.1, 1.2, 1.3, 1.6 | code, 3 languages, new Flyway migration | `OrderService`, `CouponService`, coupon/order repositories, `Order` entity, `GlobalExceptionHandler` (409) and equivalents, `system/db/migrations/V*__order_version.sql`, integration/concurrency tests in monoliths + multitier backends | A |
| U2 | #18 frontend upgrades (after `/refine-plan`) | dependency upgrade | `system/multitier/frontend-react/{package.json,package-lock.json,vite.config.*}` + test config | A (halt if refine finds a decision) |
| U3 | #6: `SecurityConfigTest` route enumeration; expired-token / wrong-audience cases | Java tests | `system/monolith/java/.../config/SecurityConfigTest.java`, `system/multitier/backend-java/.../config/SecurityConfigTest.java` | A |
| U5 | #4 parity-fixes (race verify-only, 500 detail, blackout text) | code + contract regen | 7 backends, `contracts/frontend-backend.json`, `CancelOrderNegativeIsolatedTest` x3, frontend interactions | B (Q-C) |
| U6 | CS1 merged `money-strings-end-to-end` | code, all layers | see CS1 | B (Q-B) |
| U7 | #7 steps 1.4, 1.5 | API change | idempotency table, pagination, frontend | B (Q-D) |
| U8 | #8 tax-country | API + test change | see plan | B (Q-E) |
| U9 | #6 remainder | mixed | .NET auth, actuator, npm audit | B (Q-F) |
| U10 | #1 composite action, #2 sonar outage | CI, cross-repo | `optivem/actions`, `*-commit-stage.yml`, `_prerelease-pipeline.yml` | B (Q-G) |
| U11 | #17 SonarLint | local IDE | none in repo | B (Q-H, manual) |
| U12 | p3 cluster: #14, #10, #16, #12, #13 | test architecture | see C7 | B (Q-A; README gate: "decide first whether to pursue at all") |

## Execution waves

Autonomous-ready units only. Rules for every batch: execute in-session via subagents (no `/clear` hand-offs); verify per the repo CLAUDE.md (`./compile-all.sh`, then `gh optivem system-test run --sample` per affected language); commit/push only through `/commit`; the cloud plan is active, so run the pre-execute checks first.

### Wave 1

| Batch | Mode | Units / plans | Files owned | Sessions | Pre-execute commands |
|---|---|---|---|---|---|
| 1A | serial; holds the docker lock for its system-test gate | U1: #7 steps 1.1, 1.2, 1.3, 1.6 | backend sources + tests in `system/monolith/*` and `system/multitier/backend-*`, `system/db/migrations` | 1 orchestrating session, subagents per language (Java -> .NET -> TS) | `git status`; confirm the cloud plan has not touched `system/db/migrations` |
| 1B | parallel with 1A for code and static checks; its `system start` gate waits until 1A releases the docker lock | U2: #18 frontend upgrades | `system/multitier/frontend-react/**` | 1 | `/refine-plan plans/backlog/p4-maintenance/20260917-1811-frontend-major-upgrades.md` (creates steps; no user decision expected) |

1A and 1B own disjoint files. If 1A step 1.3.e touches `contracts/frontend-backend.json` and 1B's Pact run regenerates it, check `git diff contracts/` before committing 1B.

### Wave 2 (after Wave 1 is committed)

| Batch | Mode | Units | Files owned | Sessions | Pre-execute |
|---|---|---|---|---|---|
| 2A | parallel-safe (disjoint files) | U3 SecurityConfigTest additions | the two `SecurityConfigTest.java` files | 1 | read the existing tests first (the plan says so) |
| 2B | parallel-safe with 2A; holds the docker lock | U4 system-test driver items | `system-test/{java,dotnet,typescript}` drivers and affected acceptance tests (not `system-test/typescript/config/`) | 1 orchestrating + 1 subagent per language | none |

No later wave of autonomous-ready units exists. Once the user answers, the follow-on order is: U5 (#4) -> U6 (money) -> U7 (#7B) -> U8 (#8, only after the cloud plan finishes); U10 (#1 then #2) and U12 are independent of that chain.

## Blocked on user

Answering "accept all recommendations" unblocks the whole section.

**Q-A. p3 gate (README: "decide first whether to pursue at all").** Pursue the test-parity track? Recommended: pursue narrowly. Delete the 3 done/obsolete plans now; most remaining p3 items are parity polish with no correctness payoff, so only the cheap ones should survive.
- Q-P3a (#14 legacy-twin): option A now (replace the "byte-identical" prose with an honest frozen-snapshot note), C as a separate later discussion, not B. **Recommended: A.**
- Q-P3b (#10): reject item 3 (ERP interaction verification; couples tests to implementation); drop item 4 (parity pairs now exist; no test can detect the difference). **Recommended: reject 3, drop 4**, then delete #10 and #13 (CS3).
- Q-P3c (#16 facade): option A (leave as-is; the win is about 3 lines of duplication). **Recommended: A**, delete #16.
- Q-P3d (#12 mirror): run Steps 1-2 (survey + driver-port vocabulary) only; Steps 3-4 wait for the money merge. **Recommended: yes**, sequenced after U6.

**Q-B. Money (CS1 merged plan).**
1. Accept the breaking change of deleting numeric DSL overloads? **Recommended: yes**, the DSL shape is the lesson.
2. Do rates become strings too? **Recommended: yes, all decimal fields**, one rule.
3. One commit vs staged per language? **Recommended: one coordinated change set (all backends + consumers) built in one session and committed together**, because the Pact contract is shared and a transition window adds permanent complexity.
4. Pre-approve running system tests (the plans say "ask, never self-initiate")? **Recommended: yes, `--sample` only**, per the repo CLAUDE.md gate.

**Q-C. #4 parity-fixes.**
1. Explicit rounding in the four backends? **Recommended: yes**, mirror `OrderPricing`; the rounding rule should be visible in code.
2. Coupon race shape (after U1 lands)? **Recommended: (a) conditional UPDATE**, same as #7 1.2.a; item becomes verify-only.
3. 500 `detail` text? **Recommended: adopt `"An unexpected error occurred. Please try again later."` verbatim in the other six.**

**Q-D. #7 steps 1.4 / 1.5.**
1. Idempotency scope/TTL: **per authenticated user, 24 h.**
2. Same key, different body: **422.**
3. Pagination: **optional `page`/`size`, default 50, cap 100; UI paging controls out of scope.**
4. Search indexing: **option (3), accept as is, document the trade-off.**
5. Race tests: **integration tests for races, acceptance scenarios for single-call behaviour.**

**Q-E. #8 tax-country** (also gated on the cloud plan finishing, C8).
1. Where does country live? **Keycloak user attribute exposed as a `country` claim.**
2. Customer has no country: **reject with "Customer country is not set".**
3. Keep `country` on `Order`: **yes.**
4. First pass: **monolith java + system-test java, then port.**
5. Timing vs the paid article: **defer until the article decision; confirm the change is wanted at all** (product call, no content recommendation).

**Q-F. #6 remainder (U9).**
1. Actuator/swagger on backend-java public? **No, leave authenticated.**
2. Simulators need auth? **No.**
3. `npm audit` warnings: **`npm audit fix` non-breaking only; ignore the rest.**
4. .NET intermittent UI flake: **treat as mitigated, close the bullet.**
5. .NET central route table / JWKS retry: **defer; not needed for a teaching template.**

**Q-G. CI (#1, #2).**
1. #1: per-concern actions vs one parameterised (OQ-A)? **Per-concern.** Commands owned by the action (OQ-B)? **Yes.** Pinning (OQ-C)? **Match the existing `optivem/actions` pin convention in shop.** Approve a cross-repo change to `optivem/actions`? **Yes.**
2. #2: outage policy? **(b) non-blocking on transient 5xx exhaustion only; quality-gate failures still fail.** Skipped-analysis notice: **job-summary warning.** Acceptance stages: **same policy as commit stages.** Step 1 (re-point `optivem/actions` `v1` to `cd0432c` via `update-v1.yml`) is independent; **recommended now**, before the policy is decided.

**Q-H. #17 SonarLint.** Machine-local; no agent can verify. **Recommended: do it by hand (steps 1-6 in order) or delete the plan.**

## Pre-execute checks

Run before any wave:

1. `grep -l "PICKUP\|in-flight\|claimed by\|Picked up by agent" plans/*.md plans/backlog/*/*.md` — on 2026-10-29 this matched the cloud plan (real "Picked up by agent" marker) and `tax-real-parity-contract-test.md` (false positive: the word appears in prose). Any new hit means another agent is in flight; stop and ask.
2. `git status` — at analysis time `.github/workflows/multitier-java-acceptance-stage-cloud.yml`, `plans/20261029-1316-deploy-shop-cloud-run.md` and `terraform/README.md` were modified by the cloud plan. Do not touch or stage those.
3. The cloud plans `20261029-1316-deploy-shop-cloud-run` and `20261030-1822-template-cloud-deploy-all-variants` are gone. From 2026-11-03, "the cloud plan" means P2 (`20261102-1855-single-pipeline-deploy-target`); C8/C9/C10 now apply to it. Don't edit its files, and get the user's OK before pushing while one of its cloud runs is in progress. Run P1 (Playwright) before any wave (see "Active top-level plans").
4. `git config core.hooksPath .githooks` is set (repo CLAUDE.md).
5. No docker stack running (`gh optivem system stop`) before the first system-test gate; rebuild images with `--no-cache` if UI tests time out on `aria-label="SKU"`.

## Suggested run order

1. User (a few minutes): answer Q-A..Q-H; delete the three obsolete plans (run `/review-plan` on #9 and #11 first if unsure; #15 is confirmed done). Do Q-G.2 Step 1 whenever convenient.
2. Pre-steps: `/refine-plan` on the frontend-upgrade plan. Merged-plan creation via `/create-plan` for CS1 (`money-strings-end-to-end`) only after Q-B is answered.
3. Wave 1: one session runs `/execute-plan` on `plans/backlog/p2-product-correctness/20261009-1545-production-readiness-chunk1-data-correctness.md` scoped to steps 1.1, 1.2, 1.3, 1.6, with subagents per language; a second session runs `/execute-plan` on the refined frontend-upgrade plan. Commit each via `/commit`.
4. Wave 2: `/execute-plan` on the keycloak plan scoped to U3 items, and in parallel a second session on U4 items.
5. After answers arrive: U5 (`cross-language-parity-fixes`), then U6 (merged money plan), then U7, U10 (#1 then #2), U12 in the order above; U8 only after the cloud plan finishes.

## Out of scope

- `plans/backlog/after-cloud-deploy/*` (pinned-sha gate, QA smoke tests, test-order alignment, production-readiness-gaps parent) and `plans/backlog/parked/*` — not read; blocked on or separate from the cloud plan.
- Editing any existing plan, running `/execute-plan`, committing or pushing.
- Content of the `optivem/actions` repo beyond confirming the sibling checkout exists.
