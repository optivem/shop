# 2026-11-07 16:57:45 UTC — Storefront implementation (catalog, profile, cart, checkout in all four UIs)

## TL;DR

**Why:** The storefront is designed but none of it is built. What it should look like is in [`docs/design/shared/storefront-ux.md`](../docs/design/shared/storefront-ux.md) and the prototype at <https://claude.ai/artifact/GFfHJ51yWJLVvY8JR4YVmj>. What it should do (use cases, business rules, Gherkin) is being written by `plans/20261107-1638-storefront-use-cases-rules-acceptance.md`. The shop still reads as an admin tool: you order by typing a SKU into a form.
**End result:** UC-006 to UC-011 and the amended UC-001 work in all four UIs (React, Next.js monolith, Thymeleaf monolith, Razor monolith) and the six backends, as the UX spec lays them out. Their acceptance scenarios pass, and every existing system test stays green.

This plan holds **only how to build it**. Look and layout live in the UX spec. Behaviour and rules live in the use cases. If a step here disagrees with either, the spec or use case wins, and this plan gets fixed.

## Outcomes

- The catalog data path is real: 10 ERP products (spec §6.1) in the simulator and the WireMock stubs, and an anonymous `GET /api/products` and `GET /api/products/{sku}` in all six backends (spec §6.2).
- Customer profile storage and the shipping address on orders exist, and Place Order reads the tax country from the address (UC-010, amended UC-001).
- All four UIs implement the routes, pages and selector contract in spec §2–§4 and §7, with per-route auth so anonymous visitors can browse.
- The existing E2E suites pass in all three languages after the one driver change in spec §7.1.

## ▶ Next executable step (resume here)

Step 1 (catalog data path) is done: commits `a71fb8ae`, `cee2babc`, `b3846cdc` (2026-11-07). `GET /api/products` returns a bare array of `{sku,title,description,brand,category,price}` (price is a 2-decimal string); `GET /api/products/{sku}` returns 404 for an unknown SKU and 503 when the ERP is down.

Step 9 (tag the existing UC-002–005, UC-012 tests) is done: commit `ac04be12` (2026-11-09). Only UC-004 flipped to `implemented`; the gaps keeping UC-002, UC-003, UC-005 and UC-012 at `planned` are listed under Step 10.

Next, per the meta-plan `plans/20261107-1705-meta-storefront-specs-build.md`: **Step 2** as part of the atomic unit U3 (SPEC Step 8 → BUILD Step 2 → SPEC Step 9). New tests go in the **latest** system tests only. Change legacy only as much as it needs to stay green.

Gate: `./compile-all.sh`, plus the sample system tests in all three languages, before pushing anything.

## Background

- The UX spec and prototype came from an earlier design plan (commits `6c82597a`, `5ac720c0`, `79030091`).
- Finding from the spec work: `AuthGate` wraps the whole SPA in `RequireAuth`, and every backend has `anyRequest().authenticated()`. Anonymous browsing therefore needs per-route auth in the SPA, `permitAll` on the catalog endpoints, and the driver change in spec §7.1.

## Decisions (2026-11-07, with the user)

1. **No code before the design is signed off.**
2. **The domain change goes before the UI.** Profile and shipping address (Step 2) land right after the catalog (Step 1) and before any UI step, so checkout is built once against real addresses. The user decided not to wait for the UC-001 plan.
3. **Tests stay green throughout.** The Quick order page (`/new-order`, test contract) keeps its Country field until the domain change lands. The existing three SKUs and prices stay unchanged.
4. **React first, then port.** React is the reference implementation (Step 3). Get a sign-off on how it looks, then port to the three monoliths.
5. **Flip the spec status when tests land.** When a step makes a use case's acceptance scenarios pass in all three languages, change `status: planned` to `status: implemented` in that use case's `acceptance-criteria.md` in the same commit, so the spec gate starts enforcing it (decided 2026-11-07, `plans/20261107-1705-meta-storefront-specs-build.md`).

## Steps

- [ ] **Step 2: Profile, shipping address and admin order filter** (UC-010, UC-011, amended UC-001). The UC-001 plan is in flight and touches the same Place Order code and tests. Before starting, check its status: either wait for its current track to commit, or coordinate so the two don't both edit `PlaceOrderRequest`, `OrderService` and the place-order acceptance tests at the same time.
  - Profile storage: a DB migration plus `GET`/`PUT /api/account/profile`, keyed by the Keycloak subject.
  - An `orders.shipping_address` column. `PlaceOrderRequest` takes the address instead of a free `country`.
  - `GET /api/orders?customer=` for the admin filter.
  - Acceptance tests for these use cases' scenarios in all three languages.
  - Scenarios (API channel): `@UC-001.4`, `@UC-001.10`, `@UC-001.15`, `@UC-001.16` (retagged Quick-order country tests); `@UC-010.1`, `@UC-010.2`, `@UC-010.3` (unsupported country), `@UC-010.5` (privacy), `@UC-010.8`; `@UC-011.1` (admin sees every customer's orders), `@UC-011.3` (customer filter), `@UC-011.4`, `@UC-011.5`, `@UC-011.8` (no match). The UI-only rest of UC-010 and UC-011 moves to Step 8.
- [ ] **Step 3: React reference implementation** (`system/multitier/frontend-react`; UC-006 to UC-011).
  - Make auth per-route: `AuthGate` loads the session silently, and `RequireAuth` wraps only the signed-in routes in spec §2.
  - In the same commit, change `openHomePage()` to click "Sign In" in the Java, .NET and TS UI drivers (spec §7.1).
  - New `StoreLayout` (spec §3).
  - Pages from spec §4: `Catalog` (the new `/`), `ProductDetail`, `Cart`, `Checkout`, `OrderConfirmation`, `Account`.
  - `CartContext` backed by `localStorage`, wrapped in try/catch (spec §6.3).
  - Move the admin and order pages under the account and admin navigation, keeping their routes and `aria-label`s.
  - Keep the `/new-order` page working (tests use it) and reachable from `/`.
  - Add unit and interaction tests under `src/test`.
  - Gate: `npx tsc --noEmit`, the unit tests, and the multitier sample system tests.
  - Scenarios it must make passable through the UI (their tests land in Step 8): all of `@UC-006.*`, `@UC-007.*`, `@UC-008.*`, `@UC-009.*`; `@UC-010.1` (email read-only), `@UC-010.3` (country choices), `@UC-010.4`, `@UC-010.5` (checkout preselect, Quick order prefill), `@UC-010.6`, `@UC-010.7`; `@UC-011.1` (link to details), `@UC-011.3` (filter options; needs a way to list the customers who have ordered, which Step 2 did not add), `@UC-011.6`, `@UC-011.7`, `@UC-011.8` (reset filters); `@UC-003.2`, `@UC-012.2`. Existing `@UC-001`–`@UC-005`, `@UC-012` UI tests stay green.
- [ ] **Step 4: Port to the Next.js monolith** (`system/monolith/typescript`). Same routes and same markup contract.
  - Scenarios: the same list as Step 3, in this UI.
- [ ] **Step 5: Port to the Thymeleaf monolith** (`system/monolith/java`). Server-side cart in the session, header partial in `layout.html`.
  - Scenarios: the same list as Step 3, in this UI.
- [ ] **Step 6: Port to the Razor monolith** (`system/monolith/dotnet`). Server-side cart in the session, header in `_Layout.cshtml`.
  - Scenarios: the same list as Step 3, in this UI.
- [ ] **Step 7: Verify.** `./compile-all.sh`, then `system-test run --sample` for java, dotnet and typescript (monolith and multitier). Then `./test-all.sh` before closing.
  - Scenarios: none new; every tagged `@UC-NNN.M` test stays green.
- [ ] **Step 8: Acceptance tests for the UI journeys.** Turn the remaining `@UC-006` to `@UC-011` scenarios into acceptance tests with UI and API channels in all three languages, after the UI exists so they don't collide with the UC-001 spec-gate work.
  - Scenarios: `@UC-006.1`–`.10`, `@UC-007.1`–`.7`, `@UC-008.1`–`.11`, `@UC-009.1`–`.13`; the UI-only UC-010 and UC-011 scenarios listed under Step 3; `@UC-011.2` ("-" customer; needs an order with no recorded owner); `@UC-003.2`, `@UC-012.2`. Then flip UC-006 to UC-011 to `implemented`.
- [ ] **Step 10: Close the coverage gaps left by Step 9** (found 2026-11-09, commit `ac04be12`), then flip UC-002, UC-003, UC-005 and UC-012 to `implemented`. Latest tests only, all three languages.
  - Scenarios: `@UC-002.1`, `@UC-002.2`, `@UC-002.3`, `@UC-002.4`, `@UC-002.9`, `@UC-005.1`, `@UC-005.3`, `@UC-005.4`, `@UC-005.7` (plus `@UC-003.2` and `@UC-012.2` via Step 8).
  - UC-003.2 and UC-012.2 (which of Cancel/Deliver are offered) are UI-only: cover them in Step 8 with the UI channel.
  - UC-002.1 ordering + summary fields, UC-002.2 partial/case-insensitive filter, UC-002.3 default page 1 / size 50, UC-002.4 "None" coupon example, UC-002.9 "No orders found" text: need DSL assertions (ordering, summary fields, page size) and a per-test isolated customer (only 2 shared test customers today).
  - UC-005.1 newest first, UC-005.3 page contents, UC-005.7 no coupons: need list assertions and an isolated coupon set (shared test DB); UC-005.4 is tagged on a 1-order test while the spec uses 2 orders.
  - Note: "no usage limit" is stored as int-max (2147483647), not null, in every backend; `hasUnlimitedUsage` accepts both.
