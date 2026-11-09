# Storefront UX

The shared UX spec for all four MyShop UIs:

| UI | Project | Rendering |
|---|---|---|
| React SPA (reference) | `system/multitier/frontend-react` | client-side |
| Next.js monolith | `system/monolith/typescript` | server and client |
| Thymeleaf monolith | `system/monolith/java` | server-side |
| Razor Pages monolith | `system/monolith/dotnet` | server-side |

All four implement the same routes, page anatomy and selector contract. Where server-side and client-side rendering have to differ (the cart store, how a redirect happens), this doc says so.

Implementation plan: [`plans/20261107-1657-storefront-implementation.md`](../../../plans/20261107-1657-storefront-implementation.md).

Behaviour, business rules and acceptance criteria live in the use cases ([index](../../shared/specs/use-cases/README.md)); where this doc and a use case disagree, the use case wins. Per page:

| Page / area | Use cases |
|---|---|
| Catalog `/` (§4.1) | [UC-006 Browse Catalog](../../shared/specs/use-cases/UC-006-browse-catalog/use-case-narrative.md) |
| Product detail (§4.2) | [UC-007 View Product](../../shared/specs/use-cases/UC-007-view-product/use-case-narrative.md) |
| Cart (§4.3, §6.3) | [UC-008 Manage Cart](../../shared/specs/use-cases/UC-008-manage-cart/use-case-narrative.md) |
| Checkout and confirmation (§4.4, §4.5, §6.5) | [UC-009 Check Out](../../shared/specs/use-cases/UC-009-check-out/use-case-narrative.md), which places each line through [UC-001 Place Order](../../shared/specs/use-cases/UC-001-place-order/use-case-narrative.md) |
| Quick order `/new-order` (§4.6) | [UC-001 Place Order](../../shared/specs/use-cases/UC-001-place-order/use-case-narrative.md) (extension 1a) |
| My account › Profile & address (§8) | [UC-010 Manage Profile](../../shared/specs/use-cases/UC-010-manage-profile/use-case-narrative.md) |
| My orders, order details (§4.6) | [UC-002 View Order](../../shared/specs/use-cases/UC-002-view-order/use-case-narrative.md), [UC-003 Cancel Order](../../shared/specs/use-cases/UC-003-cancel-order/use-case-narrative.md) |
| Admin › All orders (§8) | [UC-011 View All Orders](../../shared/specs/use-cases/UC-011-view-all-orders/use-case-narrative.md), [UC-012 Deliver Order](../../shared/specs/use-cases/UC-012-deliver-order/use-case-narrative.md) |
| Admin › Coupons (§4.6) | [UC-004 Publish Coupon](../../shared/specs/use-cases/UC-004-publish-coupon/use-case-narrative.md), [UC-005 Browse Coupons](../../shared/specs/use-cases/UC-005-browse-coupons/use-case-narrative.md) |

## 1. Goals and non-goals

**Goals:** the shop should look and behave like an ordinary small web shop. An anonymous visitor can browse, search, filter, view a product, keep a cart, and sign in only at checkout. Customers have a "My orders" area and admins have an admin area.

**Non-goals (for now):**

- **Multi-line orders in the domain.** An order is still one SKU and a quantity (`PlaceOrderRequest`). Checkout places one order per cart line (see §6.5).
- Payments, wishlists, reviews and ratings, recommendations, accounts beyond Keycloak, and real product photos.

## 2. Sitemap and routes

| Route | Page | Auth | Notes |
|---|---|---|---|
| `/` | Catalog (home) | anonymous | Replaces today's "feature cards" home page |
| `/?q=&category=&brand=&sort=` | Catalog, filtered | anonymous | Query string only, so it can be bookmarked and server-rendered |
| `/products/{sku}` | Product detail | anonymous | 404 page when the ERP doesn't know the SKU |
| `/cart` | Cart | anonymous | |
| `/checkout` | Checkout | **customer** | Anonymous visitors are sent to Keycloak, then back to `/checkout` |
| `/checkout/confirmation` | Order confirmation | **customer** | Shows the order numbers just created |
| `/new-order` | Quick order (by SKU) | **customer** | Today's New Order form, unchanged. **Test contract.** |
| `/order-history` | My orders | **logged in** | Unchanged contract, restyled |
| `/order-details/{orderNumber}` (SPA), `/order-details?orderNumber=` (server-rendered) | Order details | **logged in** | Unchanged contract, restyled |
| `/admin-coupons` | Admin › Coupons | **admin** | Unchanged contract, restyled |
| `/sign-in` | Starts login | anonymous | Redirects to Keycloak and returns to `returnUrl` (default `/`) |

Auth rules:

- The protected routes behave **exactly as today**. An anonymous request goes straight to the Keycloak login form (the test `assertLoginRequiredFor` relies on this). A logged-in user without the needed role gets the existing refusal: a 403 from the server-rendered UIs, or the "Not authorized" `h1` in the SPA.
- The public routes (`/`, `/products/*`, `/cart`) never redirect to Keycloak. When a session already exists, they show the signed-in header.
- SPA: `AuthGate` stops wrapping the whole app in `RequireAuth`. It loads any existing session silently, and protected routes wrap themselves in `RequireAuth`.
- Backends: `GET /api/products` and `GET /api/products/{sku}` are `permitAll`. Everything else stays as it is.

## 3. Global chrome

### Header (every page)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [■] MyShop    [ Search products…                   ] [Search]   🛒 Cart (3)  │
│                                                         Sign in  |  user ▾   │
├──────────────────────────────────────────────────────────────────────────────┤
│ All products · Laptops · …            Quick order · My orders · Admin        │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Logo / "MyShop"** links to `/`.
- **Search** is a `form` with `method=get action=/`, `aria-label="Search Products"` on the input and `aria-label="Search"` on the button.
- **Cart** is a link to `/cart`, `aria-label="Cart"`. The badge `aria-label="Cart Item Count"` shows the total quantity and is hidden when that is 0.
- **Anonymous visitors:** a "Sign in" link, `a[href^='/sign-in']`, `aria-label="Sign In"`.
- **Signed in:** the existing `UserMenu` (username plus Sign out).
- **Second row, left:** category links derived from the catalog.
- **Second row, right**, shown only when signed in, as plain visible links (not hidden in a dropdown):
  - "Quick order", `a[href='/new-order']`, only when the user can place orders.
  - "My orders", `a[href='/order-history']`.
  - "Admin", `a[href='/admin-coupons']`, only for admins.

  These are today's home-page links. They keep the `HomePage` selectors working and they appear on **every** page, not only `/`.

### Footer

Three columns of static text: Shop (All products, categories), Help (Shipping, Returns, Contact, all `#` placeholders) and "© MyShop · demo store for the Optivem ATDD course".

### Notifications

The existing notification component and markup stay as they are: `[role='alert'].notification.success|error`, `.error-message`, `.field-error`, `data-notification-id`. "Added to cart" uses the same component with type `success`. It does **not** use the text "Success! Order has been created…", which the driver parses.

## 4. Pages

### 4.1 Catalog (`/`)

```
┌ header ─────────────────────────────────────────────────────────────┐
├─────────────────────────────────────────────────────────────────────┤
│  HERO: "New season laptops"  ·  "Free shipping over $1,000"  [Shop] │
├───────────────┬─────────────────────────────────────────────────────┤
│ Category      │  3 products                     Sort: [Featured ▾]  │
│ (•) All       │ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│ ( ) Laptops   │ │  [ HP ]  │ │ [ DELL ] │ │ [LENOVO] │              │
│               │ │ HP Laptop│ │ Dell XPS │ │ ThinkPad │              │
│ Brand         │ │ 15       │ │ 13       │ │ T14      │              │
│ [x] HP        │ │ HP       │ │ Dell     │ │ Lenovo   │              │
│ [x] Dell      │ │ $699.99  │ │$1,299.99 │ │ $999.99  │              │
│ [x] Lenovo    │ │[Add cart]│ │[Add cart]│ │[Add cart]│              │
│               │ └──────────┘ └──────────┘ └──────────┘              │
└───────────────┴─────────────────────────────────────────────────────┘
```

- Grid: 1 column under 576px, 2 from 576px, 3 from 992px, 4 from 1200px. On small screens the filters collapse into a "Filters" toggle above the grid.
- **Product card:** `article.product-card` with `aria-label="Product {SKU}"`.
  - The image placeholder (§5) and the title both link to `/products/{sku}`.
  - Brand in muted text.
  - Price with `aria-label="Display Price"`.
  - Button with `aria-label="Add {SKU} to Cart"`. It adds quantity 1 and shows the "Added to cart" toast.
- **Filters:** `category` (radio), `brand` (checkboxes), `q` (search over title, brand and description, case-insensitive) and `sort` ∈ `featured` (ERP order), `price-asc`, `price-desc`, `name`. Filtering happens **in the shop UI or BFF over the full list**, because the catalog is small and the ERP has no query API.
- **Empty result:** "No products match your search." plus a "Clear filters" link to `/`.
- **ERP unavailable:** an `alert-danger` "Products are temporarily unavailable." The header still renders, so "My orders" keeps working.

### 4.2 Product detail (`/products/{sku}`)

```
Home › Laptops › HP Laptop 15
┌────────────────────┐   HP Laptop 15
│                    │   by HP · Laptops · SKU HP-15
│       [ HP ]       │   $699.99
│                    │   Qty [ - ][ 1 ][ + ]   [ Add to cart ]
└────────────────────┘   ───────────────────────────────────
                         15.6-inch laptop with Intel Core i5 processor
                         ✓ Ships in 1–2 business days   ✓ 30-day returns
```

- Breadcrumbs reuse the existing `Breadcrumb` component.
- Quantity input: `aria-label="Product Quantity"`, min 1, integer.
- Button: `aria-label="Add to Cart"`.
- An unknown SKU gives a 404 page: `h1` "Product not found" with a link back to `/`.

### 4.3 Cart (`/cart`)

```
Shopping cart (2 items)
┌──────────────────────────────────────────────────────────────┬──────────────────┐
│ [HP]  HP Laptop 15        $699.99   [-][ 2 ][+]   $1,399.98 ✕│ Order summary    │
│ [DL]  Dell XPS 13       $1,299.99   [-][ 1 ][+]   $1,299.99 ✕│ Subtotal $2,699.97│
│                                                              │ Tax & discounts   │
│ ← Continue shopping                                          │ at checkout       │
│                                                              │ [Proceed to       │
│                                                              │  checkout]        │
└──────────────────────────────────────────────────────────────┴──────────────────┘
```

- Each line is a `tr` (or `li`) with `aria-label="Cart Line {SKU}"`.
  - Quantity input: `aria-label="Quantity for {SKU}"`.
  - Remove button: `aria-label="Remove {SKU}"`.
  - Line total: `aria-label="Line Total {SKU}"`.
- Subtotal: `aria-label="Cart Subtotal"`.
- Proceed button: `aria-label="Proceed to Checkout"`.
- **Empty cart:** "Your cart is empty." plus a "Start shopping" link to `/`.
- Unit prices in the cart are **display-only and re-read from the catalog** whenever the cart renders. The backend prices the order, so a stale cart can't change what gets charged.

### 4.4 Checkout (`/checkout`, customer)

```
Checkout
┌ 1. Shipping address ─────────────────────┐  ┌ Order review ───────────────┐
│ (•) Ana Novak, Main St 1, 10115 Berlin,  │  │ HP Laptop 15 × 2  $1,399.98 │
│     DE                                   │  │ Dell XPS 13 × 1   $1,299.99 │
│ ( ) Ship to a different address          │  │ Subtotal          $2,699.97 │
├ 2. Coupon (optional) ────────────────────┤  │ Est. tax (DE)               │
│ Code [          ]                        │  │ Discount and tax are worked │
├──────────────────────────────────────────┤  │ out per order when placed   │
│ [ Place order ]                          │  │                             │
└──────────────────────────────────────────┘  └─────────────────────────────┘
```

- **Shipping address**, which replaces a free country field:
  - The saved profile address is preselected.
  - "Ship to a different address" opens a form: `Shipping First Name`, `Shipping Last Name`, `Shipping Street`, `Shipping Postal Code`, `Shipping City`, and `Shipping Country` (a select of US, DE and GB).
  - The **sales tax country is the shipping address's country** (§8), and the review shows the estimated tax for it.
- Coupon: `aria-label="Checkout Coupon Code"`. This label is deliberately **different** from the Quick order label (`Coupon Code`), so the two pages never collide in a selector.
- Button: `aria-label="Place Checkout Order"`.
- On submit, place one order per cart line (§6.5).
  - **All succeed:** clear the cart and go to confirmation.
  - **Some fail** ([UC-009](../../shared/specs/use-cases/UC-009-check-out/use-case-narrative.md) 8a): the other lines are still placed. Remove the placed lines from the cart, keep the rejected ones, and go to confirmation, which shows a result per line: the order number for each placed line, and the SKU and the backend's message for each rejected line.
  - **All fail** (UC-009 8b): no order is placed. Stay on checkout with the cart unchanged and show an error notification listing each SKU with the backend's message.
- An empty cart redirects to `/cart`.
- **Empty profile (first order):** checkout never blocks.
  - The address form opens directly, with names prefilled from the sign-in account where known.
  - A **"Save as my default shipping address"** checkbox (`aria-label="Save as Default Address"`) is ticked by default. Placing the order with it ticked writes the address to the profile.
  - This is the common pattern: collect the address in the flow and offer to remember it, rather than sending the customer to an account page first.

### 4.5 Order confirmation (`/checkout/confirmation`)

"Thank you for your order!", then a list of the order numbers just created (plus, after a partial failure, each rejected SKU with its reason), each linking to its order-details page (`aria-label="Confirmed Order {orderNumber}"`). Below that: "View my orders" and "Continue shopping".

### 4.6 Existing pages (restyled only)

Quick order (`/new-order`), My orders (`/order-history`), Order details and Admin › Coupons render inside the new chrome with a page title and breadcrumbs. **Every selector in §7 keeps working, and their markup inside the main content area doesn't change.** A visual refresh only touches the wrapper (card or panel styling).

## 5. Visual language

- **Base:** Bootstrap 5, already in all four UIs. No new CSS framework.
- **Tokens** (CSS custom properties on `:root`):
  - `--shop-brand` `#1f6feb` and `--shop-brand-contrast` `#ffffff`
  - `--shop-accent` `#f59e0b` (sale and highlight)
  - `--shop-surface` `#ffffff`, `--shop-muted-surface` `#f6f8fa`
  - `--shop-text` `#1f2328`, `--shop-muted-text` `#59636e`
  - `--shop-radius` `0.5rem`
- **Type:** the system UI font stack. Prices use tabular numerals.
- **Product image placeholder:** an inline SVG or CSS tile. The brand name sits in large letters on a tile coloured by a deterministic hash of the brand. Aspect ratio 4:3 on cards, 1:1 on the detail page. No external or hotlinked images; ERP has no image field.
- **Money:** `$` with thousands separators and 2 decimals, the same formatting the order pages use today. **One currency for every shipping country** (decided 2026-11-07); no € or £ display.
- **Accessibility:**
  - every interactive element has an accessible name (the `aria-label`s above)
  - the focus ring is visible
  - colour contrast is at least 4.5:1
  - the cart badge count is announced through `aria-live="polite"`

## 6. Data and behaviour

### 6.1 ERP catalog contract

- `GET /erp/api/products` returns `[{ id, title, description, price, category, brand }]`. The simulator (json-server) already serves this. The WireMock stubs need a new `erp-products-list.json` mapping.
- `GET /erp/api/products/{sku}` returns the same shape. The stubs need `title`, `description`, `category` and `brand` added to each product mapping.
- **Catalog seed** (10 products in 3 categories), in both the simulator and the stubs. The first three rows exist today, and their SKUs and prices must not change, because tests depend on them.

  | SKU | Title | Brand | Category | Price |
  |---|---|---|---|---|
  | `HP-15` | HP Laptop 15 | HP | Laptops | 699.99 |
  | `DELL-XPS` | Dell XPS 13 | Dell | Laptops | 1299.99 |
  | `LENOVO-T14` | Lenovo ThinkPad T14 | Lenovo | Laptops | 999.99 |
  | `ASUS-ZENBOOK-14` | ASUS Zenbook 14 | ASUS | Laptops | 899.99 |
  | `DELL-U2723QE` | Dell UltraSharp 27 4K | Dell | Monitors | 579.99 |
  | `LG-27UL500` | LG 27-inch 4K UHD Monitor | LG | Monitors | 299.99 |
  | `SAMSUNG-S24` | Samsung 24-inch FHD Monitor | Samsung | Monitors | 149.99 |
  | `LOGI-MX3S` | Logitech MX Master 3S | Logitech | Accessories | 99.99 |
  | `LOGI-MXKEYS` | Logitech MX Keys | Logitech | Accessories | 109.99 |
  | `ANKER-HUB-7` | Anker 7-in-1 USB-C Hub | Anker | Accessories | 39.99 |
- The ERP contract tests cover both endpoints in all three languages.

### 6.2 Shop catalog API (all six backends, anonymous)

- `GET /api/products` returns `200` with `[{ sku, title, description, brand, category, price }]`, or `503` with a problem-details body when the ERP is down.
- `GET /api/products/{sku}` returns `200` with the same shape, `404` for an unknown SKU, and `503` when the ERP is down.

The server-rendered monoliths call their own service layer directly; the HTTP API exists for the SPA and for API-channel tests.

### 6.3 Cart store

- **SPA:** `CartContext` persisted to `localStorage["myshop.cart"]` as `[{ sku, quantity }]`. Every read and write is wrapped in try/catch, and the cart falls back to memory when storage is unavailable.
- **Server-rendered:** the cart lives in the HTTP session (Spring `HttpSession`, ASP.NET `ISession`, or a Next.js cookie). Mutations are plain `POST /cart/add`, `/cart/update` and `/cart/remove` with a redirect back (PRG pattern), so the page works without JavaScript.
- Adding a SKU that's already in the cart increases its quantity. Quantity is capped at 99 per line.

### 6.4 Sign-in round trip

`/sign-in?returnUrl=…` (or an SPA `signinRedirect({ state: returnUrl })`) goes to Keycloak and comes back to `returnUrl`. A protected route reached anonymously redirects to the login form directly, as today. The cart survives login: the SPA keeps it in `localStorage`, and the server-rendered UIs must keep the session cart when the session ID rotates after login.

### 6.5 Checkout semantics

The domain supports one SKU per order, so checkout calls the existing place-order use case **once per cart line**, in cart order, with the same shipping address and coupon. Each line gets its own order number. A coupon with a usage limit may therefore apply to only some lines, and the confirmation shows what actually happened.

Multi-line orders are a separate follow-up plan (it changes UC-001, the schema and every test).

## 7. Selector contract (must not break)

The system-test UI drivers use these selectors in Java, .NET and TypeScript (`system-test/*/…/driver/adapter/ui/client/**`). Every UI must keep them working.

**Navigation and auth**

- `a[href='/new-order']`, `a[href='/order-history']` and `a[href='/admin-coupons']` are **visible** after login on `/`. `a[href='/order-history']` is also the "home ready" selector.
- `#root` (SPA detection).
- Keycloak: `#username`, `#password`, `#kc-login`.
- `h1:has-text('Not authorized')` (SPA refusal). The server-rendered UIs return a 403 status.
- Anonymous requests to `/new-order`, `/order-details?…`, `/order-history` and `/admin-coupons` land on the Keycloak form.

**Notifications**

- `[role='alert']`, `[role='alert'].notification.success`, `[role='alert'].notification.error`, `.notification.error .error-message`, `.notification.error .field-error`, `[data-notification-id]`.
- `.alert-danger`.
- The success text `Success! Order has been created with Order Number {n}`.

**Quick order (`/new-order`)**

- `[aria-label="SKU"]`, `[aria-label="Quantity"]`, `[aria-label="Country"]`, `[aria-label="Coupon Code"]`, `[aria-label="Place Order"]`.

**My orders**

- `[aria-label='Order Number']`, `[aria-label='Refresh Order List']`.
- `table tbody tr`, with the order number in `td:first-child`.
- A `View Details` link text in each row.

**Order details**

- `[aria-label='Display …']` for Order Number, Order Timestamp, SKU, Quantity, Unit Price, Base Price, Discount Rate, Discount Amount, Subtotal Price, Tax Rate, Tax Amount, Total Price, Country, Status and Applied Coupon.
- `[aria-label='Cancel Order']`, `[aria-label='Deliver Order']`.
- `p:has-text('Order not found')`.

**Admin › Coupons**

- `[aria-label="Discount Rate"]`, `[aria-label="Valid From"]`, `[aria-label="Valid To"]`, `[aria-label="Usage Limit"]`, `[aria-label="Create Coupon"]`, `[aria-label="Coupons Table"]`, `[aria-label='Refresh Coupon List']`.
- `table.table tbody tr td:nth-child(1..6)` = code, discount, valid from, valid to, usage limit, used count.

### 7.1 The one driver change this requires

Today `openHomePage()` navigates to `/` and waits for **either** the Keycloak form **or** `a[href='/order-history']`, because the whole app currently forces login. With an anonymous storefront neither appears for a fresh session: no redirect happens, and the "My orders" link only shows after login. So `openHomePage()` in all three languages changes as follows:

1. Navigate to `/`.
2. If a login is configured and `[aria-label='Sign In']` is visible, click it.
3. `ensureLoggedIn(HOME_READY_SELECTOR)` as before.

The change lives in `MyShopUiClient` (Java), its .NET and TS equivalents, and `KeycloakUiLogin`. It ships **in the same commit** as the first UI that drops the global login.

## 8. Customer profile and tax country

Decided 2026-11-07. This needs a domain change; see Step 2 of the [implementation plan](../../../plans/20261107-1657-storefront-implementation.md).

- **Profile fields:** first name, last name, email (read-only, from Keycloak), and a shipping address of street, postal code, city and country.
- **Where it's edited:** `/account` ("My account › Profile & address"). The selectors are `First Name`, `Last Name`, `Street`, `Postal Code`, `City`, `Profile Country` and `Save Profile`.
- **Profile API:** `GET`/`PUT /api/account/profile` (keyed by the Keycloak subject; email from the token).
- **Tax:** the tax rate comes from the **shipping address country** (today's tax stubs: US 0%, DE 19%, GB 20%). Checkout has no separate country field. Order details show the shipping address alongside `Display Country`.
- **Quick order (`/new-order`)** keeps its `Country` field, prefilled from the profile's country (UC-001 extension 1a, UC-010 BR-5). Place Order also takes a full shipping address (`shippingAddress` on `POST /api/orders`, used by checkout); Quick order sends only the country, so the test contract stays stable.
- **Admin orders:** for an admin, `/order-history` lists **every customer's orders**. It has a Customer column (which exists today) and a **customer filter** (`aria-label="Customer Filter"`, "All customers" by default, backed by `GET /api/orders?customer=`). It sits under Admin › All orders in the admin sidebar. The route and the existing selectors are unchanged.

Visual design (clickable prototype of every page): <https://claude.ai/artifact/GFfHJ51yWJLVvY8JR4YVmj>. It's private until shared.

## 9. New selectors (for future system tests)

- **Catalog:** `[aria-label='Search Products']`, `[aria-label='Search']`, `article.product-card[aria-label='Product {SKU}']`, `[aria-label='Display Price']`, `[aria-label='Add {SKU} to Cart']`.
- **Product detail:** `[aria-label='Product Quantity']`, `[aria-label='Add to Cart']`.
- **Header:** `[aria-label='Cart']`, `[aria-label='Cart Item Count']`, `[aria-label='Sign In']`.
- **Cart:** `[aria-label='Cart Line {SKU}']`, `[aria-label='Quantity for {SKU}']`, `[aria-label='Remove {SKU}']`, `[aria-label='Line Total {SKU}']`, `[aria-label='Cart Subtotal']`, `[aria-label='Proceed to Checkout']`.
- **Checkout:** `[aria-label='Checkout Country']`, `[aria-label='Checkout Coupon Code']`, `[aria-label='Place Checkout Order']`.
- **Confirmation:** `[aria-label='Confirmed Order {orderNumber}']`.
