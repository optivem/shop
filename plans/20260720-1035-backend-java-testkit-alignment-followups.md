# 2026-07-20 10:35:10 UTC — backend-java testkit ↔ system-test alignment: follow-ups

**Status:** 🟡 For discussion (2026-07-20). Nothing here is agreed. Raised while restructuring
`backend-java` test support to mirror `system-test/java` (commits `03798adf`, `6397c3b1`). The
restructure itself is **done and merged**; this plan is only the residue it surfaced.

**Related:**
- `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md` — owns the system-test half of Item 3
  (`WhenBrowseOrderHistory` / `ThenOrderHistory`). DECIDED + DEFERRED. **Do not re-decide it here.**
- `plans/20260717-1226-frontend-react-vs-systemtest-parity.md` — the currently active parity track.

## TL;DR

**Why:** Restructuring `backend-java`'s test support to mirror `system-test/java` surfaced five
residual divergences. One is a defect introduced by the restructure itself; one was investigated and
rejected; the rest need a decision rather than a refactor. Without writing them down they get
re-discovered and re-argued.

**End result:** Each divergence is either fixed, explicitly rejected with its reasoning recorded, or
handed to the plan/repo that owns it — and no one re-opens the rejected one.

## Outcomes

- The `testkit` driver ports speak one vocabulary end to end: the port method a use case calls has
  the same name as the DSL method that calls it.
- The "production types leak into the test port" question is settled **in writing** as a non-issue,
  with the reasoning, so it is not raised a fourth time.
- The backend-side `WhenCancelOrder` gap is either scheduled or explicitly declined — and the
  system-test-side order-history gap stays owned by the plan that already owns it.
- Whether `system-test`'s one-scenario-per-test guard actually fires is known, not suspected.
- Commit `6397c3b1`'s inaccurate message is either corrected or knowingly left.

## ▶ Next executable step (resume here)

**Discussion, not edits.** No item below is agreed yet — this plan should be walked with the user
(`/refine-plan`) before any code is touched. If the user has already green-lit Item 1 in conversation,
that one *is* mechanically executable: rename the 10 `stub*` methods on the three ports in
`system/multitier/backend-java/src/testSupport/java/com/mycompany/myshop/backend/testkit/driver/port/external/`
to match their DSL callers (table in Item 1), update the three adapters under `driver/adapter/external/`
and the ~10 use-case call sites under `dsl/core/usecase/external/*/usecases/`, then verify with
`./gradlew compileTestSupportJava componentTest integrationTest contractTest checkstyleAll` from
`system/multitier/backend-java`. Gate: all three suites green (27 / 54 / 30 at time of writing).

## Items

### Item 1 — Port and DSL disagree on vocabulary — **recommended fix**

- [ ] Rename the stub-programming methods on `ErpDriver`, `TaxDriver`, `ClockDriver` to match their
      DSL callers.

Introduced by the restructure: the port methods were lifted verbatim off the old concrete
`ErpStubDriver` when the interface was extracted, so every use case now translates a name:

| DSL method | Port method today | Port method proposed |
|---|---|---|
| `returnsProduct()` | `stubProduct` | `returnsProduct` |
| `returnsNoProduct()` | `stubProductMissing` | `returnsNoProduct` |
| `returnsPromotion()` | `stubPromotion` | `returnsPromotion` |
| `failsForProduct()` | `stubProductError` | `failsForProduct` |
| `failsForPromotion()` | `stubPromotionError` | `failsForPromotion` |
| `returnsTaxRate()` | `stubTax` | `returnsTaxRate` |
| `returnsNoTaxRate()` | `stubTaxMissing` | `returnsNoTaxRate` |
| `failsForCountry()` | `stubTaxError` | `failsForCountry` |
| `returnsTime()` | `stubTime` | `returnsTime` |
| `failsForTime()` | `stubTimeError` | `failsForTime` |

Supporting evidence:
- `goToErp()` already sits in the same port and *does* match its DSL method — the port is half
  consistent already, which is what makes the other five read as accidental.
- `system-test`'s `ErpDriver` uses `returnsProduct(...)` on a port that a **real** driver implements
  (`ErpRealDriver`), so `returns*` is not stub-specific language.
- `stub*` carries no information the adapter name (`ErpStubDriver`) does not already carry.

Also worth settling while here: backend currently has **three** words for "absent" — `ReturnsNoX`
(use case), `stubXMissing` (port), `doesNotExist()` (given-step). The rename collapses two of them.

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

- [ ] Decide whether the backend component layer should cover order cancellation
      (`WhenCancelOrder` + `ThenOrder` status assertions).

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

### Item 5 — `double` vs `String` assertion vocabulary — **likely skip**

- [ ] Confirm "skip" and close, or scope it.

`system-test` prefers `hasUnitPrice(double)`; `backend-java` prefers `String`, keeping `double`
overloads only for `hasDiscountRate` / `hasTaxRate`. Systematic rather than a one-off, so a fix is a
broad signature sweep across both codebases and all three languages. Cost looks far above the benefit;
the concrete symptom is only that assertions do not copy-paste between the two layers.

### Item 6 — Stale commit message on `6397c3b1`

- [ ] Decide: leave the inaccuracy noted here, or amend + force-push.

The message describes the concurrently-landed liveness work as adding `checkReachable()` to the three
external driver ports. The actual method is `goToErp()` / `goToTax()` / `goToClock()` — it had been
renamed before the commit was made and the message was not re-checked. Everything else in that message
is accurate. `6397c3b1` is already pushed, so correcting it rewrites published history for a one-word
error; recommendation is to leave it and let this plan carry the correction.

## Open questions

- **Item 1:** rename the ports to match the DSL (proposed), or the reverse — rename the DSL down to
  `stub*`? The table above assumes the former, on the grounds that `system-test` proves `returns*`
  works on a port with a real implementation.
- **Item 1 scope:** does this rename need mirroring in `backend-dotnet` / `backend-typescript`, or are
  their harnesses structurally different enough that it does not apply? *Not yet surveyed — the whole
  restructure was Java-only, and .NET/TypeScript have not been looked at once.*
- **Item 3:** is order cancellation in scope for the backend component layer at all, or deliberately a
  system-test-only concern?
- **Item 4:** who owns a system-test fix, given the .NET + TypeScript mirror obligation?
- **Cross-cutting:** the .NET and TypeScript harnesses are now further out of sync with Java than
  before the restructure. Is closing that a plan of its own?
