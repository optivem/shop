# 2026-07-22 12:21 UTC — Plan coordination meta-plan: test-DSL work

**Plans analysed:** 3 in-scope, 2 referenced-only (deferred)

**Headline:** there is **no parallelism available**. Only one of the three in-flight plans is
executable; the other two are *discussion files* that each require a conversion or approval step
before they are plans at all. The real coordination finding is a **sequencing** one, not a batching
one — see Conflicts §1.

## Per-plan status snapshot

| Plan | Status | Executable now? | Touched files (primary) |
|---|---|---|---|
| `20260717-1015-component-stub-contract-beyond` | Active, **"not yet approved to build — discuss before executing any item"** | ❌ needs per-item build decision | `backend-java` componentTest, new `ThenErp` port/impl, `ErpStubDriver`, `external-systems/stubs/mappings/*`, `mock-server.js` |
| `20260717-1020-orderhistory-systemtest-dsl-parity` | **DECIDED; DEFERRED** — "not being worked now" | ❌ next step is *"convert the decisions into a fresh actionable plan, then delete this discussion file"* | system-test DSL (`ThenStep`/`ThenStage`, new `ThenOrderHistory`, `WhenBrowseOrderHistory`), `MyShopApiDriver`, place-order when-step, new `latest` test — Java + .NET + TS |
| `20260722-1216-string-only-money-surface` | 🟡 Drafted | ⚠️ yes, **after** its open question is answered | `system-test/java/src/test` (~60 call sites), `backend-java` componentTest (4 sites), `ThenOrder`/`ThenProduct`/`ThenCountry`/`ThenCoupon` + impls **in both layers**, both verification layers, both `Converter`s |

**Referenced-only (deferred, not in coordination scope):**
`deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror` — hard dependency on the
string-only plan (its Step 3 payload changes depending on the outcome).
`deferred/20260720-1118-component-stub-contract-cross-language-mirror` — payload is defined by whatever
ships from `20260717-1015`.

## Dependency graph

```
20260722-1216-string-only-money-surface
        │
        ├──► deferred/20260720-1055 (mirror)   [explicit: "resolve before Step 3"]
        │
        └──► (soft, see Conflicts §1) 20260717-1020  and  20260717-1015

20260717-1015-component-stub-contract-beyond ──► deferred/20260720-1118 (mirror)
```

No cycles.

## Conflicts

### 1. New test call sites vs. the string-only migration — coordination conflict

- `20260722-1216` rewrites every numeric money/rate call site in both Java test trees and then
  **deletes the numeric overloads**.
- `20260717-1015` item 3's own sketch writes `.given().product().withSku("ABC").withUnitPrice(20.00)`
  — a numeric literal that would not compile after the string-only plan lands.
- `20260717-1020` adds a new `latest` system-test plus a rewritten place-order when-step; any money
  assertions it introduces would be written in whichever idiom is current when it is authored.

**Why it matters:** these plans do not collide on *files* so much as on *idiom*. Whichever lands
first sets the form the others must be written in. If either discussion plan is actioned first, its
new call sites become extra migration work for the string-only plan — and worse, they will look
correct at review time.

**Resolution (recommended): run `20260722-1216` first.** It is the only executable plan anyway, so
this costs nothing. The other two then get authored natively in the surviving idiom.
*Alternative considered:* let them run in either order and absorb the churn — rejected, because the
churn is silent (compiles fine until the overload deletion, then fails in bulk).

### 2. `20260717-1020` vs `20260722-1216` — soft conflict on the system-test DSL

Both touch the system-test scenario DSL, but in disjoint regions: `1020` adds `orderHistory()` to
`ThenStep`/`ThenStage` and adds new step classes; `1216` removes overloads from `ThenOrder` /
`ThenProduct` / `ThenCountry` / `ThenCoupon`. Safe sequentially with a rebase; **unsafe in parallel
agent sessions**, because the second session would plan against a stale view of the port interfaces.

Moot in practice — `1020` is not executable (see below).

## Consolidation findings (decided)

**None.** No two plans are mechanically intertwined, no mutual waiting, no plan asks for atomic
co-execution with another. The three are genuinely separate concerns that happen to share a layer.

## Execution units (post-consolidation)

| Unit | Plan | Type | Blocking pre-step |
|---|---|---|---|
| U1 | `20260722-1216-string-only-money-surface` | standalone | Answer its **Open question** — is the breaking change acceptable? |
| U2 | `20260717-1020-orderhistory-...` | standalone | **Convert to an actionable plan.** Today it is a decisions record; its own "Next step" says write a fresh plan and delete this file. |
| U3 | `20260717-1015-component-stub-contract-beyond` | standalone | **Per-item build approval.** Item 3 (ERP interaction verification) and item 4 (stub-vs-real field divergence) each need a yes/no; item 4's own text says decide on a weakened basis "or drop the item". |

## Needs-decision

### 1. Is `20260717-1015` item 4 still wanted at all?
The plan itself records that the original justification collapsed — all three SUTs bind only
`{id, price}`, so no test can detect the stub/simulator divergence. What remains is a fidelity and
teaching-value argument, which the plan calls "a much weaker case". **Question:** pursue on that
basis, or drop the item? This changes whether U3 exists at all.

### 2. Should `20260717-1020` be converted now or stay parked?
It is marked DEFERRED with a completed sibling track. Converting it is real work (three languages).
**Question:** is order-history parity wanted this cycle, or does it stay a decisions record?

## Execution waves

### Wave 1 — can start now

**Batch A (1 session):**
- `20260722-1216-string-only-money-surface`, all items — owns `system-test/java/src/test`,
  `backend-java/src/componentTest`, both DSL port/impl sets, both verification layers, both
  `Converter`s.
- **Gate:** answer the plan's open question first (breaking change acceptable?).
- **Note:** Items 1–2 are behaviour-affecting (a mis-scaled literal is a real assertion change), so a
  system-test run is required before commit — ask before running.

There is no Batch B. Nothing else is runnable.

### Wave 2 — after Wave 1 lands

Whichever of U2 / U3 is unblocked by the Needs-decision answers above. They touch disjoint trees
(`1020` = system-test DSL + drivers; `1015` = backend-java componentTest + external-system stubs), so
**if both are unblocked they are genuinely parallel-safe** — two sessions.

### Wave 3

`deferred/20260720-1055` Step 3 (.NET/TS mirror), which is gated on Wave 1's outcome. Its Step 1 is a
survey, not a port — keep it that way.

## Pre-execute checks

- `grep -l "PICKUP\|in-flight\|claimed by" plans/*.md plans/deferred/*.md`
- `git status` — confirm clean before starting Wave 1; it touches ~60 call sites.
- Confirm the Needs-decision answers above.

## Out of scope of this meta-plan

- Plan content correctness — this audits coordination only.
- Actual execution — `/execute-plan` per plan.
