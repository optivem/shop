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

**Gates first:**
1. The user signs off on the visual prototype. Fold any feedback into the prototype and into `storefront-ux.md`.
2. The use cases for the step being built are written (plan `20261107-1638`, at least UC-006 for Step 1).

Then **Step 1 (catalog data path)**:
1. Enrich the three `external-systems/stubs/mappings/erp-products-*.json` with `title`/`description`/`category`/`brand`, copying the values from `external-systems/simulators/mock-server.js`.
2. Add `erp-products-list.json` for `GET /erp/api/products`.
3. Add ERP contract tests for the list endpoint in java, dotnet and typescript.
4. Add an anonymous `GET /api/products` and `GET /api/products/{sku}` to the six backends (spec §6.2).

Gate: `./compile-all.sh` green.

Before you start, check that `plans/20261106-1533-use-cases-match-substack-articles.md` (touching UC-001 and the acceptance tests) isn't editing the same ERP contract-test files. If it is, coordinate first.

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

- [ ] **Step 1: Catalog data path** (UC-006, UC-007).
  - ERP data: grow the catalog to the 10 products in spec §6.1, in both the simulator (`mock-server.js`) and the stubs (one mapping per product plus a list mapping).
  - ERP stubs: add a `GET /erp/api/products` list mapping and add `title`/`description`/`category`/`brand` to the product mappings. Check that the simulator's json-server list route already serves this.
  - ERP contract tests: cover the list endpoint and the new fields in all three languages.
  - Backends: add anonymous `GET /api/products` and `GET /api/products/{sku}` in all six backends (multitier java/dotnet/typescript, monolith java/dotnet/typescript) and extend the `ErpProductDetailsResponse` DTOs. Don't touch `backend-clean-java` unless the spec says so.
  - Gate: `./compile-all.sh` is green.
- [ ] **Step 2: Profile, shipping address and admin order filter** (UC-010, UC-011, amended UC-001). The UC-001 plan is in flight and touches the same Place Order code and tests. Before starting, check its status: either wait for its current track to commit, or coordinate so the two don't both edit `PlaceOrderRequest`, `OrderService` and the place-order acceptance tests at the same time.
  - Profile storage: a DB migration plus `GET`/`PUT /api/account/profile`, keyed by the Keycloak subject.
  - An `orders.shipping_address` column. `PlaceOrderRequest` takes the address instead of a free `country`.
  - `GET /api/orders?customer=` for the admin filter.
  - Acceptance tests for these use cases' scenarios in all three languages.
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
- [ ] **Step 4: Port to the Next.js monolith** (`system/monolith/typescript`). Same routes and same markup contract.
- [ ] **Step 5: Port to the Thymeleaf monolith** (`system/monolith/java`). Server-side cart in the session, header partial in `layout.html`.
- [ ] **Step 6: Port to the Razor monolith** (`system/monolith/dotnet`). Server-side cart in the session, header in `_Layout.cshtml`.
- [ ] **Step 7: Verify.** `./compile-all.sh`, then `system-test run --sample` for java, dotnet and typescript (monolith and multitier). Then `./test-all.sh` before closing.
- [ ] **Step 8: Acceptance tests for the UI journeys.** Turn the remaining `@UC-006` to `@UC-011` scenarios into acceptance tests with UI and API channels in all three languages, after the UI exists so they don't collide with the UC-001 spec-gate work.

- [ ] **Step 9: Tag the existing use cases' tests** (UC-002 View Order, UC-003 Cancel Order, UC-004 Publish Coupon, UC-005 Browse Coupons, UC-012 Deliver Order). Their behaviour is already built but no test carries `@UC-NNN.M` tags. Tag the existing acceptance tests that cover each scenario in all three languages (same mechanism as UC-001: `@Tag` / `[Trait("tag", …)]` / `criteria(…)`), add tests for the scenarios with none, then flip each use case to `status: implemented` (decision 5). Independent of Steps 1–8; touches only `system-test/**` acceptance tests for orders and coupons, so it can run whenever no other step edits those tests. Added 2026-11-07 by `plans/20261107-1638-…`.
