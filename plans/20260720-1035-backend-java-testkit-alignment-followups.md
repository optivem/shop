# 2026-07-20 10:35:10 UTC — backend-java testkit ↔ system-test alignment: follow-ups

**Status:** 🟢 Refined 2026-07-20 — all six items decided (see `## Resolved decisions`). Raised while restructuring
`backend-java` test support to mirror `system-test/java` (commits `03798adf`, `6397c3b1`). The
restructure itself is **done and merged**; this plan is only the residue it surfaced.

**Related:**
- `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md` — owns the system-test half of Item 3
  (`WhenBrowseOrderHistory` / `ThenOrderHistory`). DECIDED + DEFERRED. **Do not re-decide it here.**
- frontend-react vs system-test parity — COMPLETED 2026-07-17 (render-and-wire depth; plan file deleted
  per the plan-processing rule).

## TL;DR

**Why:** Restructuring `backend-java`'s test support to mirror `system-test/java` surfaced six
residual divergences. One was investigated and rejected; one turned out to be a real correctness
question hiding behind a cosmetic one; the rest needed a decision rather than a refactor. Without
writing them down they get re-discovered and re-argued.

**End result:** Every divergence is now decided. Exactly **one** is executable in this plan (the Item 1
rename); everything else is either closed with its reasoning recorded, or routed to a file that owns
it. Nothing is left as an open question.

## Target state

**Item 1 is done** (2026-07-20): the ten stub-programming methods on `ErpDriver` / `TaxDriver` /
`ClockDriver` were renamed to match their DSL callers (`stubProduct` → `returnsProduct`,
`stubTaxError` → `failsForCountry`, …). A backend use case no longer translates a name between the
DSL verb and the port method it calls — the two read identically, the way `goToErp()` already does.
Java only; suites green (component 54 / integration 26 / contract 30, 0 failures).

**What is decided but deliberately not executed here:**

| Item | Decision | Lands in |
|---|---|---|
| 1 (mirror) | `returns*` rename ported to .NET/TS | `plans/deferred/20260720-1055-…-cross-language-mirror.md` Step 2 |
| 3 | Order cancellation **is** in scope for the backend component layer | Its own plan/item (not yet written) |
| 4 | Verify the guard before routing a fix | Throwaway two-scenario test in `system-test/java` |
| 5 | Additive convergence on `String`-canonical money assertions | Own plan (Java) + deferred mirror Step 3 |

**What is closed, with reasoning recorded so it is not re-raised:**

- **Item 2** — production types in the DSL port are *fine* here: `backend-java/testSupport` shares a
  build unit with production and asserting against the real contract is the point. `system-test`'s
  copies are a workaround for being a separate Gradle project, not a design principle to imitate.
- **Item 6** — commit `6397c3b1`'s message says `checkReachable()`; the real methods are `goToErp()` /
  `goToTax()` / `goToClock()`. Left unamended; this plan is the correction of record.

**What is explicitly unchanged:** no production code, no test *behaviour*, no assertions deleted, no
history rewritten, and no `.NET` / `TypeScript` file touched by this plan. The Item 5 work, whenever it
runs, is purely additive — existing call sites keep compiling.

**The one reversal worth flagging:** Item 5 was written as "likely skip — cost far above benefit." A
survey during refinement showed the divergence is narrower than assumed (four step types already agree)
*and* that `backend-java`'s `String`-canonical model is the sounder one, because `system-test` compares
money as raw `double`. It is now a scheduled additive fix with a correctness rationale, not a
cosmetic-parity nice-to-have.

## ▶ Next executable step (resume here)

**Item 1 landed 2026-07-20; Item 4 is the only remaining executable unit — and it is a verification,
not a fix.** In `system-test/java`, write a throwaway test that runs two scenarios in one test method
and observe whether `ScenarioDslImpl`'s `executed` guard throws. If it throws, the guard is reachable:
delete the throwaway, close Item 4, nothing else to do. Only if it does **not** throw does this become
a real (three-language, `system-test`-owned) fix, and ownership is decided at that point. Requires a
local test run — ask the user before starting it.

## Items

### Item 2 — Production types in the DSL port — **investigated, rejected, do not re-open**

- [x] Investigated 2026-07-20. **Conclusion: not a problem. No action.**

Observation that triggered it: `dsl/port/then/steps/ThenOrder.java:3` imports the production enum
`backend.core.entities.OrderStatus`; `driver/port/MyShopDriver` imports `backend.core.dtos.*` and
returns Spring `ResponseEntity`. `system-test` instead keeps its own `testkit/domainvaluetypes/OrderStatus`
and 27 independent port DTOs.

Why it is nonetheless fine here:
- `system-test` copies those types because it is a **separate Gradle project that structurally cannot
  import backend production code**. The copies are a workaround, not a design principle.
- `backend-java/testSupport` is **in the same build unit** as production and already depends on
  production types deliberately and pervasively (`SutErpReader` returns `ProductDetailsResponse`,
  `BackendDriver` takes `PlaceOrderRequest`). Asserting against the real contract is the point.
- The one genuine argument for copies — drift detection — is already covered elsewhere: stub drivers
  hand-write JSON as strings so stub↔production drift fails the stub-contract tests, and
  frontend↔backend drift fails Pact.

Copying types here would add maintenance and *remove* the "assert against the real thing" property in
order to fix a problem this layer does not have.

### Item 3 — Step-tree feature gaps — **needs a product decision, partly owned elsewhere**

- [x] Decided 2026-07-20: **in scope** for the backend component layer, but scheduled as its own
      plan/item — see Resolved decisions. Not executed here.

Gaps run both directions:
- **backend-java has no `WhenCancelOrder`** — system-test has both the port and the impl. This half is
  unowned and is the only genuinely new question in this item.
- **system-test has no `WhenBrowseOrderHistory` / `ThenOrderHistory`** — backend has both. This half is
  **already owned** by `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md` (decided, deferred).
  Do not re-decide it here; if it comes up, defer to that plan.

These are missing *features*, not missing structure — no amount of refactoring produces them.

### Item 4 — Is system-test's one-scenario guard inert? — **verify before filing**

- [ ] Write a throwaway test that runs two scenarios in one system-test method and confirm whether it
      throws. Only file/fix if it does not.

Suspicion (unverified): in `system-test/java`'s `ScenarioDslImpl`, `markAsExecuted()` is public but
`given()` / `when()` return `new GivenImpl(app)` / `new WhenImpl(app)` without passing `this`, so
nothing can flip `executed` through the normal DSL path. `backend-java` passes `this`
(`new GivenImpl(app, this)`) and its guard is reachable.

Caveats: this came from a static reading, not an observed failure. It concerns **system-test, not
backend-java**, so any fix lands in that layer and inherits its cross-language mirror obligation
(.NET + TypeScript).

### Item 5 — `double` vs `String` assertion vocabulary — **surveyed 2026-07-20, converge additively**

- [x] Surveyed. **Decision: additive convergence on `String`-canonical, scoped as its own plan.**

The original framing ("`system-test` prefers `double`, `backend-java` prefers `String`, fixing it is a
broad signature sweep") was too coarse. The actual state:

**`ThenOrder` — money amounts**

| Method | `system-test/java` | `backend-java` |
|---|---|---|
| `hasUnitPrice` | `double` only | `String` only |
| `hasTotalPrice` | `double` + `String` | `String` only |
| `hasBasePrice` | `double` + `String` | `String` only |
| `hasSubtotalPrice` | `double` + `String` | `String` only |
| `hasDiscountAmount` | `double` + `String` | `String` only |
| `hasTaxAmount` | `String` | `String` ✅ |

**Rates**

| Method | `system-test/java` | `backend-java` |
|---|---|---|
| `ThenOrder.hasTaxRate` | `double` + `String` | `String` + `double` ✅ |
| `ThenOrder.hasDiscountRate` | `double` only | `String` + `double` |
| `ThenCountry.hasTaxRate` | `double` | `double` ✅ |
| `ThenCoupon.hasDiscountRate` | `double` | `String` ❌ |
| `ThenProduct.hasPrice` | `double` | `double` ✅ |

Two corrections this forces:

1. **It is not "String vs double" — it is "which overloads exist."** `system-test` offers *both* on
   most money amounts; `backend-java` offers only `String`. Four step types already agree exactly.
2. **`backend-java`'s model is deliberate and sounder.** Its `double` overloads do not assert on
   doubles — they convert and delegate
   (`ThenOrderImpl.java:71`: `hasDiscountRate(BigDecimal.valueOf(expectedDiscountRate).toPlainString())`).
   `String` is canonical (exact decimal comparison), `double` is sugar. That sidesteps float equality
   on money. `system-test` passing `double` straight through to `orderVerification.totalPrice(double)`
   is the weaker end — so there is a genuine correctness argument here, and it points at `system-test`
   adopting `backend`'s model rather than the reverse.

Because the fix is **additive**, it is not the signature sweep originally feared: add delegating
one-liner overloads, delete nothing, so no existing call site changes.

### Item 6 — Stale commit message on `6397c3b1`

- [x] Decided 2026-07-20: **leave it**; this plan carries the correction. No history rewrite.

The message describes the concurrently-landed liveness work as adding `checkReachable()` to the three
external driver ports. The actual method is `goToErp()` / `goToTax()` / `goToClock()` — it had been
renamed before the commit was made and the message was not re-checked. Everything else in that message
is accurate. `6397c3b1` is already pushed, so correcting it rewrites published history for a one-word
error; recommendation is to leave it and let this plan carry the correction.

## Resolved decisions

- **Item 1 — done in Java 2026-07-20; .NET/TypeScript mirroring deferred.** The rename shipped in
  `backend-java` alone; it was not blocked on surveying the other two harnesses. The mirror obligation
  is not dropped — it lives in
  `plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md` (Step 2), which
  owns the survey.

- **Item 3 — order cancellation is in scope for the backend component layer, scheduled separately**
  (2026-07-20). `WhenCancelOrder` + `ThenOrder` status assertions belong at the component layer: it is
  a real backend state transition, assertable in-process and cheaply, and `ThenOrder` already carries
  the `OrderStatus` vocabulary. But it is a feature addition, not a refactor, so it does **not** ride
  along with the Item 1 rename — it gets its own plan/item. The system-test-side
  `WhenBrowseOrderHistory` / `ThenOrderHistory` half remains owned by
  `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md`.

- **Item 4 — verify before routing** (2026-07-20). Ownership is not decided yet, deliberately: the
  suspicion is a static reading, not an observed failure. Step one is the throwaway two-scenario test
  in `system-test/java` (Java only, no mirror obligation, minutes of work). If the guard throws, Item 4
  closes with no fix and the ownership question never arises. Only if it is genuinely inert does it
  become a three-language `system-test` item, and ownership is decided then.

- **Item 5 — additive convergence on `String`-canonical, scoped as its own plan** (2026-07-20).
  Reverses the plan's original "likely skip" after an actual survey (table in Item 5). Target: every
  money/rate assertion accepts both forms, with `String` canonical and `double` a delegating
  convenience that converts via `BigDecimal.toPlainString()`. Concretely — `backend-java` gains
  `double` sugar on the five `ThenOrder` amount methods; `system-test` gains `String` on
  `hasUnitPrice` / `hasDiscountRate` and routes its `double` money comparisons through exact decimal
  comparison; `ThenCoupon.hasDiscountRate` is aligned. **Nothing is deleted**, so no existing call site
  changes. Java lands first as its own plan; the .NET/TypeScript rollout is deferred to
  `plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md` (Step 3). Not
  executed here.

- **Item 6 — leave `6397c3b1`'s message as-is** (2026-07-20). Not worth rewriting published history
  for a one-word error. The correction of record: the commit message says the liveness work added
  `checkReachable()` to the three external driver ports; the actual methods are `goToErp()` /
  `goToTax()` / `goToClock()`. Everything else in that message is accurate.

- **Cross-cutting — split out to `plans/deferred/`, survey first** (2026-07-20). The .NET/TypeScript
  harness drift is now
  **`plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md`**. Deferred
  because it is blocked on the Java shape settling, and its **first deliverable is a survey** of both
  harnesses against the restructured Java one — they have not been looked at once, so committing to
  full convergence now would be committing blind. That file is also the home for the two pieces of
  scope deferred above: Item 1's `returns*` rename mirror (its Step 2), and Item 5's three-language
  additive overload rollout (its Step 3).
