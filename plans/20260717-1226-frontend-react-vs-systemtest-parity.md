# frontend-react component tests — parity with system-test coverage

**Status:** ✅ Done (2026-07-17). Enumeration done; parity depth = render-and-wire; settled against the
code → **only G1 + G2** were genuine gaps (G3–G6 dropped with recorded evidence). G1 + G2 implemented —
see the commit. Kept as the decision record so the render-and-wire scope and the dropped items aren't
relitigated.

## Scope

Reconcile the **frontend-react component-test suite** (`system/multitier/frontend-react/src/test`) with
the **system-test suite** (`system-test/**`, Java canonical), closing every capability/coverage
difference between them — starting with `orderHistory` but covering **all** use cases where the two
suites differ.

**Reconciliation direction:** the system-test suite is the **fixed reference**; all changes land in
**frontend-react**. We do **not** modify the system-test layer, and we leave **backend-dotnet** and
**backend-typescript** as-is (only frontend-react is touched).

- **In scope:** frontend-react component-test DSL, drivers (UI + gateway), backend-stub DSL, pact
  interactions, and the `latest/component` (and where relevant `latest/integration`) test files.
- **Out of scope:** the system-test additions (tracked, deferred, in
  `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md`); backend-dotnet / backend-typescript.

## Current state (verified 2026-07-17)

frontend-react **already** has a full browse-order-history path in its component-test layer, so
`orderHistory` is **not** a gap on the frontend-react side:
- DSL: `frontend.browseOrderHistory().execute().showsOrder('ORD-1')` (`src/test/support/frontend-dsl.ts`)
- UI driver + gateway driver both implement `browseOrderHistory()` / `showsOrder()`
- backend-stub DSL `returnsOrderHistory()`; pact `browseOrderHistoryInteraction()`
- tests: `src/test/latest/component/browse-order-history.component.test.tsx` (+ legacy twin)

The real question is the **broader** parity: what else do the two suites cover differently. That is
being enumerated now.

## Coverage enumeration (verified 2026-07-17)

System-test .NET + TypeScript mirror Java at the file/method level, so all differences below are
A(frontend-react) vs B(system-test), language-independent.

### Use-case coverage matrix

| Use case | frontend-react component | system-test latest |
|---|---|---|
| PlaceOrder — positive (confirmation) | ✓ `place-order-positive.component.test.tsx` (+ integration) | ✓ `PlaceOrderPositiveTest` (15), `…Isolated` (3), `e2e` (1) |
| PlaceOrder — client-side validation (no backend call) | ✓ `place-order-negative` describe#1 | ✗ (B has no client tier) |
| PlaceOrder — backend field rejections (coupon/sku/country) | ✓ `place-order-negative` describe#2 | ✓ `PlaceOrderNegativeTest` |
| PlaceOrder — outright reject (blackout) | ✓ `place-order-negative` describe#3 | ✓ `PlaceOrderNegativeIsolatedTest` |
| PlaceOrder — pricing math (base/subtotal/tax/discount/total) | ✗ | ✓ `PlaceOrderPositiveTest` (10+) |
| PlaceOrder — coupon usage-count increment | ✗ | ✓ `PlaceOrderPositiveTest` |
| PlaceOrder — expired / usage-limit coupon | ✗ | ✓ `PlaceOrderNegative(Isolated)Test` |
| PlaceOrder — promotion pricing / placement timestamp | ✗ | ✓ `PlaceOrderPositiveIsolatedTest` |
| CancelOrder — positive | ✓ `cancel-order.component.test.tsx` (`wasCancelled`) | ✓ `CancelOrderPositive(Isolated)Test` (asserts status CANCELLED) |
| CancelOrder — blackout reject | ✓ `cancel-order` (`wasRejected`) | ✓ `CancelOrderNegativeIsolatedTest` |
| CancelOrder — non-existent | ✗ | ✓ `CancelOrderNegativeTest` |
| CancelOrder — already-cancelled | ✗ (button hidden) | ✓ `CancelOrderNegativeTest` |
| ViewOrder — positive | ✓ `view-order` (`showsOrderDetails('ORD-1','$22.00')` — total only) | ✓ `ViewOrderPositiveTest` (`shouldSucceed` only) |
| ViewOrder — not found | ✓ `view-order` (`showsNotFound`) | ✓ `ViewOrderNegativeTest` |
| ViewOrder — status-gated Cancel/Deliver actions | ✓ `view-order` describe#2 | ✗ |
| BrowseOrderHistory | ✓ `browse-order-history.component.test.tsx` (+ integration) | ✗ (not in latest at all) |
| BrowseCoupons | ✓ `browse-coupons.component.test.tsx` (`showsCoupon('SAVE10')`) | ✓ `BrowseCouponsPositiveTest` (`shouldSucceed` only) |
| PublishCoupon — positive | ✓ `publish-coupon.component.test.tsx` (`succeeded`) | ✓ `PublishCouponPositiveTest` (4, w/ field round-trip) |
| PublishCoupon — negative (range/limit/duplicate/blank) | ✗ (deliberate) | ✓ `PublishCouponNegativeTest` (5) |

### 3(a) — frontend has, system-test lacks → **no action** (we don't touch system-test)
BrowseOrderHistory in latest; ViewOrder status-gated UI actions; rendered order-detail total;
client-side (no-backend-call) validation path; specific rendered coupon code on browse. These are
frontend-only capabilities or system-test's own gap (its deferred plan).

### 3(b)/3(c) — system-test has, frontend-react lacks → **categorized** by whether the frontend has a real surface

**Category 1 — genuine frontend RENDERING-parity gaps** (frontend displays the field but the test
doesn't assert it; stub returns the value, test asserts it renders on the right label — *not* a
computation test). `OrderDetailView` renders the full `ViewOrderDetailsResponse` breakdown
(`unitPrice, basePrice, discountRate, discountAmount, subtotalPrice, taxRate, taxAmount, totalPrice,
status`); `OrderHistoryTable` renders `orderNumber, totalPrice, status`.
- G1. **ViewOrder** — assert the full detail breakdown renders (currently only total). Needs richer
  `showsOrderDetails(...)` / new assertion verbs on the Frontend DSL.
- G2. **BrowseOrderHistory** — assert the row renders `totalPrice` + `status` (currently only
  `showsOrder(orderNumber)`).
- G3. **Displayed order status** after place/cancel (PLACED / CANCELLED) where the UI shows it.

**Category 2 — genuine frontend BEHAVIOR/wiring gaps** (frontend owns the rule or must surface the error):
- G4. **PlaceOrder — negative quantity** distinct from zero (frontend `order-validation.ts` owns this;
  A currently tests only zero via `withQuantity(0)`). Add if the rule exists client-side.
- G5. **CancelOrder — non-existent order** → stub 404, assert the UI surfaces the error (wiring).
- G6. **PublishCoupon — blank code** → if validated client-side, assert `hasFieldError`; otherwise
  treat as backend-rejection wiring (one representative case).

**Category 3 — backend-owned; NOT frontend parity gaps (skip — would be tautological vs a stub, or no
frontend surface)**
- Pricing/tax/discount **computation** (base = unit×qty, subtotal = base − discount, tax by country,
  total = subtotal + tax), coupon **usage-count increment**, **promotion** pricing, **placement
  timestamp**: the frontend neither computes these nor (for promotion/timestamp) renders them; a
  stub-based assertion just echoes the canned value. Stay in system-test / backend-component.
- CancelOrder **blackout boundary rows** (time-dependent — frontend has no clock arrange).
- PublishCoupon **discount-range / duplicate / usage-limit** negatives (backend rules; duplicate is
  backend-only). Frontend file header already documents these as deliberate omissions.
- Expired / usage-limit-**exceeded** coupon on PlaceOrder: backend rule; at most one generic
  "backend-rejects → UI shows error" wiring case, not the rule table.

### Structural differences (context — not work items)
- **Arrange side:** A = Backend Stub DSL (canned Pact-backed responses) — no clock / product-price /
  promotion / country-tax verb; B = domain-state Given DSL that builds real state, which is *why* B can
  test pricing/tax/promotion/time and A structurally cannot.
- **Channel model:** B runs one scenario through UI + API via `@Channel`; A splits it into two harnesses
  (`componentHarness` → rendered React, `integrationHarness` → gateway) sharing the same Frontend DSL.
- **Order-number resolution:** A always explicit (`'ORD-1'`); B supports no-arg resolution + explicit.
- **Assertion semantics:** A is UI-anchored (`hasConfirmation`, `showsOrder`, `hasFieldError(field,msg)`);
  B is domain-anchored (`order().hasTotalPrice(...)`, `coupon().hasUsedCount(1)`).

## Resolved decisions

- **Parity depth: render-and-wire parity (not a full 1:1 mirror).** Close **G1–G6 only** — assert what
  the frontend RENDERS (order-detail breakdown, history row total+status), the rules it OWNS client-side
  (negative quantity), and the backend errors it must SURFACE (cancel non-existent). **Category 3 is
  explicitly OUT OF SCOPE:** backend-owned pricing/tax/discount computation, coupon usage-count,
  promotion pricing, placement timestamp, blackout boundary rows, and the PublishCoupon
  discount-range/duplicate/usage-limit rule table stay in the backend / system-test layers — a
  stub-based frontend assertion of those just echoes the canned value (tautology) and duplicates backend
  coverage. This respects the level boundary the frontend test files already document.

## Work items — ✅ both done (G1, G2)

Implemented as: Frontend DSL assertion additions (`OrderDetailExpectation`, `OrderHistoryRowExpectation`,
widened `showsOrderDetails` / `showsOrder`) → UI + gateway driver impls → the two `latest/component`
tests. `npx tsc --noEmit` clean.

- **G1 — ✅ ViewOrder detail breakdown renders.** Extended `showsOrderDetails(...)` (UI-only method) to assert
  the full `OrderDetailView` breakdown renders against its aria-labels (`Display Unit Price / Base Price /
  Discount Rate / Discount Amount / Subtotal Price / Tax Rate / Tax Amount / Total Price / Status / SKU /
  Country / Quantity / Applied Coupon`), not just order number + total. Expected values are the fixed
  stub values in `viewOrderDetailsInteraction` (unit $10.00, base $20.00, discount 0.00%/$0.00, subtotal
  $20.00, tax 10.00%/$2.00, total $22.00, status PLACED, sku BOOK-123, country US, qty 2, coupon None).
- **G2 — ✅ BrowseOrderHistory row renders `totalPrice` + `status`.** Extended `showsOrder(orderNumber, …)`
  (shared UI+gateway method, so the expectation is **semantic** — number total + status string; each
  driver formats/compares its own way) to assert the row also shows total 22 (`$22.00` in UI) and status
  `PLACED`. Values from `browseOrderHistoryInteraction`.

### Dropped — evidence-based (verified 2026-07-17), recorded here so the decision isn't relitigated
- **G3 (displayed status after place/cancel)** — DROP: status is only rendered on the details view (G1)
  and the history row (G2); the place/cancel confirmations have no status field. Subsumed.
- **G4 (negative quantity)** — DROP: `validateOrderForm` has a single `quantityNum <= 0 → "Quantity must
  be positive"` branch, so negative qty hits the *identical branch and message* as the existing zero-qty
  test. Redundant.
- **G5 (cancel non-existent)** — DROP: cancel is reachable only from a *loaded* details screen; a
  non-existent order → "Order not found" → no Cancel button, so a user cannot provoke it. Same documented
  level boundary as already-cancelled (`cancel-order.component.test.tsx` header).
- **G6 (publish blank code)** — DROP: `CouponForm` has no client-side validation and publish-coupon
  negatives are a documented deliberate omission; would be generic backend-wiring with no frontend surface.

## Resolved (encoding decisions)
- **Q1 (G1 assertion shape):** extend the existing `showsOrderDetails(...)` to take an expected-fields
  object rather than adding discrete `showsTax`/`showsDiscount` verbs — one UI-only method, matches the
  existing `'$22.00'` string style. **Q2 (G4/G6 rule location):** moot — both items dropped.

## Related

- `plans/20260717-1020-orderhistory-systemtest-dsl-parity.md` — the deferred system-test track.
- `plans/deferred/20260717-1015-component-stub-contract-beyond.md` — deferred PASS-2 follow-up to the
  external-stub contract work (PASS 1 "mirror" completed; separate track).
