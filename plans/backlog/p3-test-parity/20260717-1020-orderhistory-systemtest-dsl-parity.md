# orderHistory in the **system-test** DSL — parity (DECIDED; DEFERRED)

## TL;DR

**Why:** The component scenario DSL has a browse-order-history path (`when().browseOrderHistory()` / `then().orderHistory().containsOrder()`) that the system-test DSL completely lacks, and `GET /api/orders` is only exercised by a single *legacy* UI e2e test — the system-test API driver has no list method at all, so the API channel cannot even reach the endpoint.

**End result:** The system-test layer gains a matching `browseOrderHistory` / `orderHistory().containsOrder()` scenario step wired multi-channel (UI + API via `ChannelMode.DYNAMIC`), a `MyShopApiDriver` list method for `GET /api/orders`, a place-order when-step that reads the SUT-generated order number back off the response instead of echoing `DEFAULT-ORDER`, and a new `latest` test browsing order history across both channels — in Java, .NET, and TypeScript.

**Scope:** the **system-test** layer only (Java canonical; .NET + TypeScript mirror). This is the
"bring system-test up to the component layer" track. The parallel **frontend-react** parity work is a
separate plan — see Related.

**Status:** Decisions made (Q1–Q4 resolved 2026-07-17), but **DEFERRED — not being worked now.** The
frontend-react parity track that preceded this one completed on 2026-07-17 (render-and-wire parity: only
the ViewOrder detail-breakdown and order-history row assertions landed; its plan file was deleted per the
plan-processing rule). Per that decision we touched frontend-react only and left the system-test layer
(and backend-dotnet / backend-typescript components) as-is. Keep this plan as the record of the agreed
system-test end-state for when it is picked up later.
**Related:**
- `plans/20260717-1015-component-stub-contract-mirror.md` (external-stub contract work — separate;
  `orderHistory` is deliberately out of that scope because it involves no external stub).

## Target state

When the follow-up actionable plan is implemented, the **system-test** layer will have a
browse-order-history path that mirrors the component layer, and the `GET /api/orders` coverage gap
will be closed — in all three languages (Java, .NET, TypeScript):

- **New scenario-DSL step**, same surface as component: `when().browseOrderHistory()` and
  `then().orderHistory().containsOrder()` / `containsOrder(orderNumber)`. Implemented over
  system-test's existing `ExecutionResultContext` + `ChannelMode.DYNAMIC`, so **one step runs across
  both the UI and API channels** (not the component's single-channel wiring).
- **New API-driver list method** on `MyShopApiDriver` for `GET /api/orders` — it has none today, so
  the API channel currently cannot reach the list endpoint at all.
- **Place-order when-step fixed** to read the **SUT-generated** order number back off the response
  instead of echoing the `DEFAULT-ORDER` constant, so no-arg `containsOrder()` asserts what the SUT
  actually produced. Explicit `containsOrder(orderNumber)` remains available as a fallback.
- **New `latest` system-test** that browses order history across API + UI channels — closing the gap
  where the list endpoint is exercised only by the *legacy* UI e2e test.

What a developer observes afterward: a system-test can write
`scenario.when().placeOrder().then().shouldSucceed().and().orderHistory().containsOrder();` — reading
identically to the component test — and it runs on both channels.

**Explicitly unchanged:** the component DSL (already honest and correct); the external-stub contract
work (clock/product/country) in the sibling plans; the existing clock/product/country/order/coupon
steps; and the legacy UI order-history test, which stays.

**Adjacent note (not in this plan's scope):** the *component* browse-history DSL is itself uneven
across languages — full DSL in **Java** only, a raw-`supertest` equivalent in **TypeScript**, and
**nothing** in **.NET**. Bringing the component layer to parity is a separate concern; this plan is
about the **system-test** layer matching component.

## What this is about

`then().orderHistory().containsOrder()` exists in the **component** scenario DSL but has **no
equivalent in system-test's** scenario DSL. This surfaced repeatedly while aligning the two layers;
it is a genuine parity question that deserves its own decision rather than being folded into the
contract-test passes.

## The facts (verified 2026-07-17)

- **Component** has a full browse-history path in the scenario DSL:
  - `when().browseOrderHistory()` (`WhenBrowseOrderHistoryImpl`)
  - `then().orderHistory().containsOrder()` / `containsOrder(orderNumber)` (`ThenOrderHistoryImpl`)
  - reads the SUT's own **`GET /api/orders`** via `BrowseOrderHistory` → `BackendDriver`.
  - exercised by `OrderHistoryComponentTest.browseReturnsPlacedOrders()`.
- **System-test** has **none** of that in its scenario DSL. `ThenStep`/`ThenStage` there expose
  `order`, `coupon`, `clock`, `product`, `country` — **no `orderHistory`**, and there is no
  `when().browseOrderHistory()`.
- System-test exercises order history **only through the UI** — `OrderHistoryPage` /
  `MyShopUiDriver`, and only in a **legacy** UI test (`legacy/mod04/e2e/PlaceOrderPositiveUiTest`).
  There is **no `latest` acceptance/scenario test** for `GET /api/orders` at the system-test level.

## Why it is not a correctness problem

`orderHistory()` reads the SUT's own list endpoint — same category as `order()` and `coupon()`. It is
already honest (no stub involved, so no tautology). So this is purely about **parity and coverage
symmetry between the two test layers**, not about fixing a broken step. Whatever we decide, the
component step as written is fine.

## Resolved decisions

- **Q1 — Direction of reconciliation: (a) bring system-test up to component.**
  Add a browse-order-history scenario step to the system-test DSL
  (`when().browseOrderHistory()` + `then().orderHistory().containsOrder()`), mirroring the component
  surface. A symmetric scenario step reads consistently across the two layers and is exactly what the
  new `latest` API+UI test (Q2) needs. This supersedes any "accept the asymmetry" option.

- **Q2 — `GET /api/orders` is under-covered at the system-test level: confirmed real gap; close it.**
  Verified 2026-07-17: the list endpoint is exercised only by the *legacy UI* e2e test
  (`legacy/mod04/e2e/PlaceOrderPositiveUiTest`, all 3 languages) via `OrderHistoryPage` /
  `MyShopUiDriver`. No `latest` acceptance/scenario/e2e test drives it, and the system-test API driver
  (`MyShopApiDriver`) has **no list method** — the API channel cannot even call `GET /api/orders` today.
  Decision: the follow-up actionable plan adds a `latest` system-test that browses order history across
  **API + UI channels**, which requires adding a list method to `MyShopApiDriver`. Closing this gap
  naturally introduces the `orderHistory` scenario step and satisfies Q1(a) at the same time.

- **Q3 — DSL shape: mirror the component surface, wire it multi-channel.**
  Keep the component method names (`when().browseOrderHistory()` + `then().orderHistory().containsOrder()`)
  so the parity between layers is obvious at a glance, but implement them over system-test's existing
  `ExecutionResultContext` + `ChannelMode.DYNAMIC` plumbing so a single step runs across both the UI and
  API channels — rather than copying the component's single-channel (API-only) wiring.

- **Q4 — `containsOrder()` no-arg: support it, but capture the SUT-generated number first.**
  Support the no-arg `containsOrder()` via system-test's existing `ExecutionResultContext`, but first fix
  the system-test place-order when-step to read the **SUT-generated** order number back off the response —
  today it stores the client-supplied `DEFAULT-ORDER` constant (`ScenarioDefaults.DEFAULT_ORDER_NUMBER`),
  so resolving no-arg against it would assert against a value the test itself chose, not what the SUT
  produced. Reading it back off the response matches what the component step already does and keeps the
  assertion honest. The explicit `containsOrder(orderNumber)` form stays available as a fallback.

## Explicitly NOT in scope here

- The external-stub contract work (clock/product/country) — that is the other two plans. `orderHistory`
  has no external stub and no contract-test angle.

## Next step

Q1–Q4 are resolved (see **Resolved decisions** and **Target state** above). Convert the decisions into
a fresh actionable plan — new scenario-DSL step, `MyShopApiDriver` list method, place-order when-step
read-back fix, and a `latest` API+UI browse-order-history test, across all three languages — then delete
this discussion file.
