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
- **The test contract is kept.** Every selector the system-test UI drivers use still works: the `aria-label`s on New Order, Order History, Order Details and Coupons, plus the `a[href='/new-order']`, `a[href='/order-history']` and `a[href='/admin-coupons']` links reachable from `/`. The existing E2E suites pass in all three languages without driver changes.
- **One documented UX spec.** `docs/ux/storefront.md` lists the sitemap, the routes, a wireframe per page, the visual tokens and the selector contract. All four UIs are built against it.

## ▶ Next executable step (resume here)

**Step 1 — write the UX spec `docs/ux/storefront.md`** (documentation only, no code). It contains:
1. The sitemap and route table for both shopper and admin flows, with the auth requirement on each route.
2. An ASCII wireframe per page: Home/catalog, Product detail, Cart, Checkout, Order confirmation, plus the restyled My orders and Admin pages.
3. The header and footer anatomy and the visual tokens: a Bootstrap 5 base (already used everywhere), a brand colour, card style, and an image placeholder generated from brand and title, with no external images.
4. The **selector contract**: the full list of selectors from `system-test/java/**/driver/adapter/ui/client/**` (plus the .NET and TS equivalents) that must survive. New elements use `aria-label`s in the same style.
5. The catalog API shape (`GET /api/products`, `GET /api/products/{sku}`) and the ERP list contract.

Link it from the README. Stop here and let the user review the spec before Step 2: it's the shared reference for four UIs, so changing it later costs four times as much.

## Steps

- [ ] **Step 1: UX spec.** Write `docs/ux/storefront.md` as described above and link it from the README.
- [ ] **Step 2: Catalog data path.**
  - ERP stubs: add a `GET /erp/api/products` list mapping and add `title`/`description`/`category`/`brand` to the three product mappings. Check that the simulator's json-server list route already serves this.
  - ERP contract tests: cover the list endpoint and the new fields in all three languages.
  - Backends: add anonymous `GET /api/products` and `GET /api/products/{sku}` in all six backends (multitier java/dotnet/typescript, monolith java/dotnet/typescript) and extend the `ErpProductDetailsResponse` DTOs. Don't touch `backend-clean-java` unless the spec says so.
  - Gate: `./compile-all.sh` is green.
- [ ] **Step 3: React reference implementation** (`system/multitier/frontend-react`).
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
