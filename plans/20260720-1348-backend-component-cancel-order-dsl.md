# 2026-07-20 13:48 UTC — backend component DSL: add order cancellation

**Status:** 🟡 Drafted 2026-07-20 — grounded in a survey of both layers; two open questions to resolve
before execution (see `## Open questions`).

**Origin:** Item 3 of `plans/20260720-1035-backend-java-testkit-alignment-followups.md`, which decided
order cancellation **is** in scope for the backend component layer but scheduled it as its own plan
because it is a feature addition, not a refactor. That parent plan is now closed.

**Related:**
- `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md` — the mirror-image gap
  (`WhenBrowseOrderHistory` / `ThenOrderHistory` missing in *system-test*). DECIDED + DEFERRED.
  **Do not fold it into this plan.**
- `plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md` — owns the eventual
  .NET / TypeScript rollout. This plan is **Java-first and Java-only**.

## TL;DR

**Why:** The backend production code has supported order cancellation the whole time — controller,
service, business rules, and an `OrderStatus.CANCELLED` value. Only the *component-test DSL* cannot
reach it, so every cancellation scenario (including the Dec-31 blackout rule) is testable solely at the
slow system-test layer. `ThenOrder.hasStatus(OrderStatus)` already exists on the component side, so the
assertion vocabulary is sitting there unused.

**End result:** `scenario.when().cancelOrder().withOrderNumber(...).then()` works in `backend-java`
component tests, and the four cancellation scenarios currently covered only by system tests have fast
in-process equivalents.

## Target state

This is a **test-DSL gap, not a production gap** — the survey confirmed production is complete:

| Layer | Location | State |
|---|---|---|
| Controller | `OrderController.java:47` — `POST /api/orders/{orderNumber}/cancel` | ✅ exists |
| Service | `OrderService.java:137` — `cancelOrder(String)` | ✅ exists |
| Business rules | Dec-31 22:00–22:30 blackout (`:147`), already-cancelled rejection (`:159`) | ✅ exists |
| Enum | `OrderStatus` — `PLACED, CANCELLED, DELIVERED` | ✅ exists |
| Component assertion | `ThenOrder.hasStatus(OrderStatus)` (`ThenOrder.java:34`) | ✅ exists |
| Component action | `WhenCancelOrder` | ❌ **absent — this plan** |

**What is explicitly unchanged:** no production code, no system-test code, no `.NET` / `TypeScript`
file. Purely additive to the component DSL — nothing existing changes signature or behaviour.

## ▶ Next executable step (resume here)

**Resolve the two open questions below, then execute Item 1** (the driver-port + adapter pair), which
is the bottom of the stack and unblocks everything above it. Items 1→4 are strictly ordered; Item 5
(the tests) is the payoff and the only place the work is actually verified.

Compile-only verification is fine for Items 1–4 (`./gradlew build` in
`system/multitier/backend-java`). Item 5 needs a component-test run — `./gradlew componentTest` — which
is in-process and needs no docker stack, so it is cheap. **Ask before running it.**

## Open questions

1. **Literal order number, or alias-based like system-test?** The system-test `WhenCancelOrderImpl`
   resolves an alias through the result context (`context.getResultValue(...)`), because a system test
   rarely knows the order number it just created. The component layer's existing `WhenViewOrderImpl`
   takes a **literal** order number (`WhenViewOrderImpl.java:30`).
   **Recommendation: follow the component layer's literal convention.** Consistency inside a layer
   beats consistency across layers, and the component DSL's `given().order()` already lets a test fix
   the order number up front. Cross-layer symmetry is not a goal the parent plan ever set.

2. **Does `cancelOrder()` call `ensureDefaults()` in `WhenImpl`?** `OrderService.cancelOrder` reads the
   clock (`:143-146`) to evaluate the blackout window, and the component DSL's clock is a WireMock stub
   that 404s until someone programs it.
   **Recommendation: yes — call `ensureDefaults()`, exactly as `viewOrder()` does.** Otherwise a
   scenario that does not mention the clock gets an unexplained 404 from the SUT rather than "now".

## Items

### Item 1 — Driver port + adapter

- [ ] Add `cancelOrder(String orderNumber)` to `driver/port/MyShopDriver.java` (currently 7 methods, no
      cancel).
- [ ] Implement it in `driver/adapter/api/BackendDriver.java` — `POST /api/orders/{orderNumber}/cancel`.

**Return type matters.** Use `ResponseEntity<String>`, **not** `ResponseEntity<Void>`. The blackout and
already-cancelled rules raise `ValidationException`, which surfaces as a ProblemDetail body; a `Void`
return throws that body away and makes the negative scenarios unassertable.

### Item 2 — Use case layer

- [ ] Add `core/usecase/usecases/CancelOrder.java`, modelled on the sibling `ViewOrder.java`:
      `BaseMyShopUseCase<Void, VoidVerification>`, builder `orderNumber(String)`, `execute()` calling
      `driver.cancelOrder(...)` and building a `UseCaseResult<>` with the success/rejection statuses.
- [ ] Add a `cancelOrder()` factory to `core/usecase/MyShopDsl.java` (currently 6 factories).

`VoidVerification` already exists at `core/shared/VoidVerification.java` — reuse it, do not add a new
verification type. **Confirm the controller's actual success status** (`OrderController.java:49-50`)
rather than assuming `200`; the rejection statuses are most likely `BAD_REQUEST` (blackout,
already-cancelled) and `NOT_FOUND` (unknown order).

### Item 3 — DSL port + step impl

- [ ] Add `dsl/port/when/steps/WhenCancelOrder.java` — extends `WhenStep`, one builder method
      `withOrderNumber(String)`.
- [ ] Add `dsl/core/scenario/when/steps/WhenCancelOrderImpl.java` — extends
      `BaseWhenStep<Void, VoidVerification>`, defaulting the order number to the scenario default.

Note `BaseWhenStep` now takes `(app, scenario)` and calls `scenario.markAsExecuted()` — the constructor
must forward both.

### Item 4 — Register the step

- [ ] Add `cancelOrder()` to `dsl/port/when/WhenStage.java` (currently 5 actions).
- [ ] Add the override to `dsl/core/scenario/when/WhenImpl.java`, calling `ensureDefaults()` per Open
      question 2, and passing `scenario` into the new step like its siblings.

### Item 5 — Component tests

- [ ] Add `CancelOrderPositiveTest` / `CancelOrderNegativeTest` under `backend-java`'s `componentTest`
      source set (`latest` package — currently only Coupon, OrderHistory, PlaceOrder{Positive,Negative},
      contract and smoke tests live there).

Mirror the four existing system-test scenarios, which are the specification:

| System test | Scenario |
|---|---|
| `CancelOrderPositiveTest:12` | `shouldHaveCancelledStatusWhenCancelled` |
| `CancelOrderNegativeTest:16,26` | non-existent order (parameterized); already-cancelled order |
| `CancelOrderPositiveIsolatedTest:17` | cancellable **outside** the Dec-31 22:00–22:30 blackout |
| `CancelOrderNegativeIsolatedTest:18` | rejected **inside** the blackout |

The blackout pair is the interesting one and the strongest argument for this plan: it is pure
time-dependent business logic, and the component DSL drives it in-process via the existing
`given().clock()` step instead of standing up a stack.

## Non-goals

- **No `deliver` step.** `POST /api/orders/{orderNumber}/deliver` has the same gap, but the parent plan
  scoped cancellation only. Note it, do not build it.
- **No .NET / TypeScript mirror** — deferred to
  `plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md`.
- **No change to `system-test`.** The four tests above stay exactly as they are; this adds a fast layer
  beneath them, it does not replace them.
