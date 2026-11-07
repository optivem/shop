# 2026-11-07 17:05 UTC — Plan coordination meta-plan: storefront specs + build

**Plans analysed:** 2 in-scope, 1 referenced-only

- In scope: `20261107-1638-storefront-use-cases-rules-acceptance.md` (call it **SPEC**), `20261107-1657-storefront-implementation.md` (call it **BUILD**).
- Referenced-only (not in coordination scope): `20261106-1533-use-cases-match-substack-articles.md` (**UC1**). Both in-scope plans name it as the owner of UC-001 and the spec gate.

## Per-plan status snapshot

| Plan | Status | Items done / total | Touched files (primary) | Notes |
|---|---|---|---|---|
| SPEC `20261107-1638` | In progress; waiting for the user to review the UC-006 template | 4 / 11 (Steps 0–3 done; Step 10 is a standing rule) | `docs/shared/specs/use-cases/UC-002…UC-012/*` (new folders), `UC-001-place-order/{use-case,business-rules,acceptance-criteria}.md` (Step 8), `docs/shared/specs/use-cases/README.md` (Status column), `docs/design/shared/storefront-ux.md` + `plans/20261107-1657-…md` (Step 9 cross-references) | Docs only. Step 4 reads backend code and tests without editing them. |
| BUILD `20261107-1657` | Not started | 0 / 8 | Step 1: `external-systems/simulators/mock-server.js`, `external-systems/stubs/mappings/erp-products-*.json`, ERP contract tests ×3, six backends (products endpoints, ERP DTOs). Step 2: DB migration, profile API, `PlaceOrderRequest`/`OrderService` ×6 backends, place-order acceptance tests ×3. Steps 3–6: the four UIs plus `openHomePage()` in the three UI drivers. Step 8: acceptance tests for `@UC-006…011` | Gates: user signs off the prototype, and the use cases for each step are written. Its decision 2 says Step 2 doesn't wait for UC1. |
| UC1 `20261106-1533` (ref-only) | Tracks A and B done; C (gh-optivem) and D (re-enable the gate) remain | — | D: `spec.yaml`, `.github/workflows/_acceptance-stage-docker-compose.yml` | D4 expects "all 14 criteria (UC-001.1–14)" with a 90% threshold. |

## Dependency graph

```
SPEC Step 3 (UC-006, done) ──► [GATE G0: user reviews UC-006 + signs off the prototype]
                                   │
               ┌───────────────────┼─────────────────────────┐
               ▼                   ▼                         ▼
     SPEC Steps 4,5          SPEC Steps 6,7          BUILD Step 1 (catalog data path)
     (UC-002–005,012;        (UC-009,010,011)        needs UC-006 ("at least UC-006 for Step 1")
      UC-007,008)                  │
                                   ▼
                    SPEC Step 8 (amend UC-001) ══ BUILD Step 2 (profile + shipping address)
                                   │            (atomic unit U3, see consolidation #1)
                                   ▼
                    SPEC Step 9 (cross-reference BUILD steps ↔ @UC tags)
                                   │
                                   ▼
          BUILD Step 3 (React; needs UC-006–011) ──► Steps 4, 5, 6 (ports) ──► Step 7 (verify) ──► Step 8 (UI acceptance tests)

UC1 Track C (gh-optivem C1 + C1b, release) ──► UC1 Track D (re-enable the gate)   ⚠ see Conflict #1
```

Edges come from the plans' own wording:
- BUILD's "use cases for the step being built are written", and its Step 3 "UC-006 to UC-011".
- SPEC Step 8 / BUILD Step 2: "coordinate with the UC-001 plan".
- SPEC Step 9: "each of its steps lists the `@UC-NNN.M` scenarios".
- BUILD decision 1: "No code before the design is signed off".

## Conflicts

### 1. Spec gate threshold vs. the new criteria: dependency inversion (SPEC ↔ UC1 Track D)
- SPEC adds Gherkin criteria for UC-002 to UC-012 under `specDir` (UC-006 already exists, with 17 scenarios). BUILD writes their tests only in Steps 2 and 8, and SPEC Step 4 documents existing behaviour that has no `@UC-00N` tags at all.
- UC1 D1–D4 turn the gate back on with `threshold: 90` across `specDir` and expect exactly 14 criteria (UC-001 only).
- **Why it's a conflict:** once gh-optivem C1 and C1b ship, the gate sees about 100 or more criteria with only UC-001's covered. It fails, or D4's "14 criteria" check is wrong. UC1 assumes UC-001 is the only use case ("Out of scope: … The shop has only UC-001"). That stopped being true at commit `16144f83`/`18d34c87`.
- **Resolution (decided):** `status: planned` in the criteria front matter, skipped by gh-optivem (C1c). See "Decided" below. UC1 Track D waits for C1c.

### 2. UC-001 scenario meaning changes under existing tagged tests: coordination conflict (SPEC Step 8 ↔ BUILD Step 2 ↔ UC1's tagged tests)
- SPEC Step 8 replaces extension 4b ("country is missing", `@UC-001.10 @narrative-4b`) with an address-country extension and updates BR-3.
- `@UC-001.10` is the tag on `PlaceOrderNegativeTest` in Java, .NET and TypeScript (`system-test/{java,dotnet,typescript}/…/PlaceOrderNegativeTest*`). Those tests still assert today's free `country` field until BUILD Step 2 changes `PlaceOrderRequest`.
- **Why it's a conflict:** if the spec is amended first, the tagged tests prove a criterion that no longer exists. If the code changes first, the code has no spec. BUILD decision 3 also keeps Quick order's Country field "until the domain change lands".
- **Resolution:** consolidation #1. Both run as one atomic session, so spec, code and tags move together.

### 3. `docs/design/shared/storefront-ux.md`: soft conflict (SPEC Step 9 ↔ BUILD gate G0)
- SPEC Step 9 adds spec links to it. BUILD's gate folds the prototype feedback into it.
- **Resolution:** gate G0 lands first (wave 1), and Step 9 comes later (wave 3), so they never run in parallel.

### 4. `plans/20261107-1657-storefront-implementation.md`: soft conflict (SPEC Step 9 ↔ any BUILD session)
- SPEC Step 9 writes `@UC-NNN.M` lists into BUILD's steps. Every `/execute-plan` BUILD session also edits that file, deleting finished items and refreshing its Next block.
- **Resolution:** don't run Step 9 while a BUILD session is open. It goes in unit U3 (below), which is the one BUILD session at that point.

## Consolidation findings (decided)

### 1. SPEC Steps 8 + 9 and BUILD Step 2 — atomic single session (unit U3)
- They share `UC-001-place-order/*` (the spec), the `@UC-001.10` tests (Conflict #2) and the BUILD plan file (Conflict #4). Each depends on the other: BUILD Step 2 implements the amended UC-001, and SPEC Step 8 is only true once that code exists.
- **Resolution (recommended):** keep both plans as written. One executor runs SPEC Step 8, then BUILD Step 2 (tests updated to the amended `@UC-001.*` tags), then SPEC Step 9. All of it is pushed only once `compile-all.sh` and the sample system tests are green. **Why:** spec, code and tags change together, and nothing red is ever on main. Neither plan needs rewriting.
- **Alternative considered:** merge into a fresh plan. Rejected because SPEC is docs and BUILD is code, both are already being executed, and an atomic session gets the same safety without re-planning.

### 2. SPEC Steps 4–7: no change (one unit, internal parallelism)
- They write disjoint folders (`UC-002…005,012`, `UC-007,008`, `UC-009,010`, `UC-011`). The only shared file is `README.md`'s Status column, so update it once at the end of the unit, not in each subagent.

## Execution units (post-consolidation)

| Unit | Plans / items | Type | Touched files (primary) |
|---|---|---|---|
| G0 | SPEC (UC-006 review) + BUILD (prototype sign-off) | user gate | `UC-006-browse-catalog/*`, `storefront-ux.md`, the prototype artifact |
| U1 | SPEC Steps 4, 5, 6, 7 | standalone (4 parallel subagents inside one session) | `docs/shared/specs/use-cases/UC-00{2,3,4,5,7,8,9}-*/`, `UC-01{0,1,2}-*/`, `use-cases/README.md` (Status, once) |
| U2 | BUILD Step 1 | standalone | `external-systems/**` ERP simulator, stubs, contract tests; six backends' product endpoints and DTOs |
| U3 | SPEC Steps 8, 9 + BUILD Step 2 | atomic single session | `UC-001-place-order/*`; profile and migration; `PlaceOrderRequest`/`OrderService` ×6; place-order acceptance tests ×3; `storefront-ux.md`; BUILD plan file |
| U4 | BUILD Steps 3 → 4 → 5 → 6 → 7 → 8 | standalone, sequential | four UIs, three UI drivers, then acceptance tests for UC-006…011 |

## Decided (was Needs-decision #1, resolved 2026-11-07 by the user)

### 1. Spec gate vs. written-but-unbuilt use cases → `status: planned`
- Every `acceptance-criteria.md` has `status: planned | implemented` in its front matter. UC-001 = `implemented`, UC-006 = `planned` (both set 2026-11-07). SPEC writes `planned` for every new use case. BUILD flips a use case to `implemented` in the commit where its acceptance tests pass (BUILD decision 5).
- gh-optivem request: Step 2c of `plans/move-to-gh-optivem/20261107-1300-spec-parse-gherkin-criteria.md`, listed as C1c in UC1 Track C. UC1 D4 is annotated: it needs C1c, and the UC-001 count may change after U3.

## Execution waves

### Wave 1 — now (user only, no agent sessions)

- **G0:**
  - Review `docs/shared/specs/use-cases/UC-006-browse-catalog/`: tag convention, shared Background, assumptions. The list is in SPEC's ▶ Next block.
  - Sign off the prototype (<https://claude.ai/artifact/GFfHJ51yWJLVvY8JR4YVmj>) and fold any feedback into `storefront-ux.md`.

### Wave 2 — after G0 (2 parallel sessions)

**Batch A (parallel-safe, 2 agent sessions):**
- **U1:** `/execute-plan plans/20261107-1638-storefront-use-cases-rules-acceptance.md` (Steps 4–7). Docs under `docs/shared/specs/use-cases/` only. Fans out to 4 subagents.
- **U2:** `/execute-plan plans/20261107-1657-storefront-implementation.md` (Step 1 only; stop before Step 2). Touches `external-systems/**` and the six backends' product endpoints.
- Files are disjoint (docs vs. code, and each session edits only its own plan file).

### Wave 3 — after U1 and U2 land (1 session)

**Batch A (serial, 1 agent session):**
- **U3:** one session that runs SPEC Step 8, then BUILD Step 2, then SPEC Step 9 (consolidation #1). Don't run it at the same time as UC1 Track D or any other BUILD session.
- Before you start: check that UC1 has no in-flight shop work. Its remaining work is C (gh-optivem) and D (`spec.yaml`/workflow), which share no files with U3, but UC1 owns UC-001's tags.

### Wave 4 — after U3 (1 session, possibly split across several)

- **U4:** `/execute-plan plans/20261107-1657-storefront-implementation.md` (Steps 3–8, in order). BUILD itself gates on React sign-off after Step 3. The ports in Steps 4–6 touch disjoint monoliths, but BUILD orders them, so keep them serial unless BUILD is changed.

### Independent track — UC1 Track D (ref-only)

- Can land any time after gh-optivem C1, C1b and C1c are released. Never alongside U3. It touches `spec.yaml` and the workflow, which no in-scope unit edits.

## Pre-execute checks (apply before any wave starts)

- `git fetch && git status -sb`. Origin is the source of truth (two machines).
- `grep -ln "Picked up by agent" plans/*.md`. Today only `20261104-1148-meta-backlog-execution.md` has a marker.
- `git stash list`. Two leftover `autostash` entries from the SPEC Step 1–3 run. `stash@{0}` was already checked as redundant and `stash@{1}` wasn't; look at both before dropping them.

## Out of scope of this meta-plan

- Whether either plan's content is correct (use the plan's own refine cycle).
- The other active plans (`20261103-1049`, `20261104-1148`). `20261104-1148` is the backlog meta-plan; this one only covers the storefront pair.
- Actual execution (use `/execute-plan` against each plan in turn).
