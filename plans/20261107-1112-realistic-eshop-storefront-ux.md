# 2026-11-07 11:12:18 UTC — Realistic e-shop storefront UX (browse, product page, cart, checkout)

## TL;DR

**Why:** Today the shop reads as an admin tool rather than a shop. The home page is a set of feature cards (New Order, Order History, Coupons), and to order something you type a SKU into a form. You can't browse products, there's no product page and there's no cart, so the demo doesn't look like an e-commerce site to people watching the course.
**End result:** An anonymous visitor lands on a storefront with a product grid. They can search, filter and sort it, open a product page, add items to a cart, and check out (they sign in at checkout). Customers get a "My orders" area and admins get a separate admin area. All four UIs (React, Next.js monolith, Thymeleaf monolith, Razor monolith) behave the same, and every existing system test stays green.

## Outcomes

- **Anonymous storefront.** `/` shows a header (logo, search box, cart icon with an item count, Sign in or account menu), a hero banner and a product grid. Each card shows an image placeholder, title, brand, price and an "Add to cart" button. Category and brand filters and price sorting work. No login is needed to browse.
- **Product detail page.** `/products/{sku}` shows the title, brand, category, description, price, a quantity stepper, "Add to cart" and breadcrumbs (Home › Category › Product).
- **Cart.** `/cart` lists the lines with quantity edit, remove and line totals, plus a subtotal and "Proceed to checkout". The cart survives a page reload (browser storage for the SPA, cookie or session for the server-rendered UIs). The header count updates straight away, and a toast confirms each "Added to cart".
- **Checkout.** `/checkout` requires login: an anonymous visitor goes to Keycloak and comes back to checkout. It collects the country and an optional coupon, shows an order review with price, discount and tax, and places the order(s). Then it shows an order confirmation page with the order number(s) and a link to "My orders".
- **Account and admin are separated from shopping.** "My orders" (the current order history and order details) sits under the account menu. Coupon management sits under an "Admin" link that only admins see.
- **Real catalog data.** The shop backends expose an anonymous `GET /api/products` and `GET /api/products/{sku}`, fed by the ERP (title, description, category, brand, price). The ERP simulator already has these fields. The WireMock stubs gain a list mapping and the enriched fields.
- **The test contract is kept.** Every selector the system-test UI drivers use still works (spec §7). The existing E2E suites pass in all three languages after **one small driver change**: `openHomePage()` clicks "Sign In" when a login is configured (spec §7.1). That change is unavoidable. Today the whole app forces login, so the driver waits for either the Keycloak form or `a[href='/order-history']`. A public storefront shows neither to a fresh session.
- **One documented UX spec.** [`docs/design/shared/storefront-ux.md`](../docs/design/shared/storefront-ux.md) lists the sitemap, the routes, a wireframe per page, the visual tokens and the selector contract. It's linked from the README, and all four UIs are built against it.

## ▶ Next executable step (resume here)

**Waiting for the user's sign-off on the visual design** at <https://claude.ai/artifact/GFfHJ51yWJLVvY8JR4YVmj>. The user said "UX design first, don't touch any code". Fold any design feedback into the canvas and into `docs/design/shared/storefront-ux.md`. Only after sign-off, start **Step 2 (catalog data path)**:
1. Enrich the three `external-systems/stubs/mappings/erp-products-*.json` with `title`/`description`/`category`/`brand`, copying the values from `external-systems/simulators/mock-server.js`.
2. Add `erp-products-list.json` for `GET /erp/api/products`.
3. Add ERP contract tests for the list endpoint in java, dotnet and typescript.
4. Add an anonymous `GET /api/products` and `GET /api/products/{sku}` to the six backends (spec §6.2).

Gate: `./compile-all.sh` green.

Before you start, check that `plans/20261106-1533-use-cases-match-substack-articles.md` (active, touching UC-001 and the acceptance tests) isn't editing the same ERP contract-test files. If it is, coordinate first.

## Decisions (2026-11-07, with the user)

1. **Checkout = one order per cart line.** The confirmation page lists every order number. Multi-line orders are a separate follow-up plan.
2. **Visual design first, no code.** Before any code step, produce a clickable visual design (prototype) of every page in spec §4, and get it signed off. Only then start Step 2.
3. **Customer profile.** A customer has a first name, last name and shipping address (street, city, postal code, country). It's edited on a new "My account" page (`/account`) and prefilled at checkout, where the customer can also use a different address for this order.
4. **Tax uses the customer's country.** The tax rate comes from the country of the shipping address, so checkout has no separate country field. This is a **domain change**:
   - `PlaceOrder` takes the shipping address (or reads the profile) instead of a free `country` field.
   - Orders store the shipping address.
   - UC-001 and the tests change with it.
   - The Quick order page (`/new-order`, test contract) keeps its Country field until that domain change lands, so tests stay green in between.

   Sequence it after the in-flight UC-001 plan (`20261106-1533-use-cases-match-substack-articles.md`), as its own steps (see Step 2b).

## Steps

- [x] **Step 1b: Visual design prototype.** Published at <https://claude.ai/artifact/GFfHJ51yWJLVvY8JR4YVmj> (12 artboards: 10 pages plus the shared header and footer). **Waiting for the user's sign-off.** A clickable mock-up of Catalog, Product detail, Cart, Checkout, Confirmation and the restyled My orders, Order details, Quick order and Admin pages, anonymous and signed in. It goes to the user for sign-off, and approved changes flow back into the spec.

- [x] **Step 1: UX spec.** Written to `docs/design/shared/storefront-ux.md` (it fits the existing `docs/design/shared/` layout better than a new `docs/ux/`) and linked from the README.
  - Finding: today `AuthGate` wraps the whole SPA in `RequireAuth`, and every backend has `anyRequest().authenticated()`. Anonymous browsing therefore also needs the SPA's auth to be per-route, `permitAll` on the catalog endpoints, and the driver change in spec §7.1.
- [ ] **Step 2: Catalog data path.**
  - ERP stubs: add a `GET /erp/api/products` list mapping and add `title`/`description`/`category`/`brand` to the three product mappings. Check that the simulator's json-server list route already serves this.
  - ERP contract tests: cover the list endpoint and the new fields in all three languages.
  - Backends: add anonymous `GET /api/products` and `GET /api/products/{sku}` in all six backends (multitier java/dotnet/typescript, monolith java/dotnet/typescript) and extend the `ErpProductDetailsResponse` DTOs. Don't touch `backend-clean-java` unless the spec says so.
  - Gate: `./compile-all.sh` is green.
- [ ] **Step 2b: Customer profile and shipping address (domain change, after the UC-001 plan lands).**
  - New use cases: Manage Profile, and Place Order shipping to an address.
  - Profile storage (DB migration plus `GET`/`PUT /api/account/profile`, keyed by the Keycloak subject) and an `orders.shipping_address` column.
  - Place Order takes the tax country from the shipping address.
  - Acceptance tests in all three languages, and update the spec doc to match.
- [ ] **Step 3: React reference implementation** (`system/multitier/frontend-react`).
  - Make auth per-route: `AuthGate` loads the session silently, and `RequireAuth` wraps only `/checkout*`, `/new-order`, `/order-*` and `/admin-*` (spec §2).
  - In the same commit, change `openHomePage()` to click "Sign In" in the Java, .NET and TS UI drivers (spec §7.1).
  - New `StoreLayout` (header with search, cart badge and account menu; footer).
  - Pages: `Catalog` (the new `/`), `ProductDetail`, `Cart`, `Checkout`, `OrderConfirmation`.
  - `CartContext` backed by `localStorage` (wrapped in try/catch).
  - Move the admin and order pages under the account and admin navigation, keeping their routes and `aria-label`s.
  - Keep the `/new-order` page working (tests use it) and reachable from `/`.
  - Add unit and interaction tests under `src/test`.
  - Gate: `npx tsc --noEmit`, the unit tests, and the multitier sample system tests.
- [ ] **Step 4: Port to the Next.js monolith** (`system/monolith/typescript`). Same routes and same markup contract.
- [ ] **Step 5: Port to the Thymeleaf monolith** (`system/monolith/java`). Server-side cart in the session, header partial in `layout.html`.
- [ ] **Step 6: Port to the Razor monolith** (`system/monolith/dotnet`). Server-side cart in the session, header in `_Layout.cshtml`.
- [ ] **Step 7: Verify.** `./compile-all.sh`, then `system-test run --sample` for java, dotnet and typescript (monolith and multitier). Then `./test-all.sh` before closing.
- [ ] **Step 8 (optional, depends on the open question): system tests for the new journeys.** Add a "Browse catalog / add to cart" use case doc and acceptance scenarios with UI and API channels in all three languages.

## Open questions

- **What does checkout do with a multi-line cart?** The domain only supports **one SKU per order** today (`PlaceOrderRequest`: sku, quantity, country, couponCode), and UC-001 is being reworked by the in-flight plan `20261106-1533-use-cases-match-substack-articles.md`.
  - *Recommendation:* for this plan, checkout places **one order per cart line**, and the confirmation page lists every order number. Multi-line orders (an order aggregate with lines) become their own follow-up plan once the UC-001 work has landed, because that changes the domain, the use case, the DB schema and every test.
  - *Alternative:* a single-item "Buy now" checkout only, with no multi-line cart.
- **Do all four UIs move together, or React first?** *Recommendation:* React first (Step 3), get a sign-off on how it looks, then port. The ports are mostly mechanical once the spec and the React version agree.
- **Do the new journeys get their own system tests?** (Step 8) *Recommendation:* yes, as a separate use case (UC-002 Browse & Cart), but after the UI exists, so it doesn't collide with the UC-001 spec-gate work.
- **Product images:** generated placeholders (a brand initial on a brand-coloured tile) vs. real images. *Recommendation:* placeholders. They avoid licensing questions and hotlinking, and ERP has no image field.
