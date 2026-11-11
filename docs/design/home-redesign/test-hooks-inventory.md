# Test hooks inventory (Market redesign, Step 1)

Reference for the Market restyle (`plans/20261111-1814-market-ui-redesign-all-screens.md`). It lists every hook the UI system-test drivers rely on, per screen and shared component, and maps each one to the element that satisfies it in `system/multitier/frontend-react` and the three monolith UIs. A restyle may change classes, wrappers, spacing and icons freely **except** for what is listed here.

Snapshot taken 2026-11-11 from the working tree. Selectors marked **(uncommitted WIP)** come from the in-progress Java work (`CartPage.java`, `ProductPage.java`, additions to `CatalogPage.java` and `MyShopUiClient.java`) and do not yet exist in .NET or TypeScript.

## 1. Where the drivers live

| Language | Page objects | Shared Playwright wrapper | Client (routing, login, readiness) |
|---|---|---|---|
| Java | `system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/adapter/ui/client/pages/` — BasePage, CatalogPage, CouponManagementPage, HomePage, NewOrderPage, OrderDetailsPage, OrderHistoryPage, CartPage (WIP), ProductPage (WIP) | `.../driver/adapter/shared/client/playwright/PageClient.java`, `KeycloakUiLogin.java` | `.../driver/adapter/ui/client/MyShopUiClient.java` |
| .NET | `system-test/dotnet/Driver.Adapter/Ui/Client/Pages/` — BasePage, CatalogPage, CouponManagementPage, HomePage, NewOrderPage, OrderDetailsPage, OrderHistoryPage | `Driver.Adapter/Shared/Client/Playwright/PageClient.cs`, `KeycloakUiLogin.cs` | `Driver.Adapter/Ui/Client/MyShopUiClient.cs` |
| TypeScript | `system-test/typescript/src/testkit/driver/adapter/ui/client/pages/` — BasePage, CatalogPage, CouponManagementPage, HomePage, NewOrderPage, OrderDetailsPage, OrderHistoryPage | `.../shared/client/playwright/PageClient.ts`, `keycloak-ui-login.ts` | `.../ui/client/MyShopUiClient.ts` |
| Legacy (raw Playwright, no page objects) | `system-test/java/src/test/java/.../legacy/mod03/e2e/PlaceOrder{Positive,Negative}UiTest.java`, `system-test/dotnet/SystemTests/Legacy/Mod03/E2eTests/PlaceOrder{Positive,Negative}UiTest.cs`, `system-test/typescript/tests/legacy/mod03/e2e/place-order-{positive,negative}-ui-test.spec.ts` | — | — |

### How selectors are built

- **No role or label locators.** No driver uses `getByRole`, `getByLabel`, `getByText` or `getByTestId`, and no `data-testid` exists anywhere. Every selector is a raw CSS/Playwright string passed to `page.locator(selector)`.
- **PageClient does not transform names.** Java/.NET `PageClient` take the full selector string; page objects write `[aria-label='X']` themselves. TS page objects call `this.page.locator(...)` directly.
- **Waiting:** `PageClient.getLocator` waits for the selector to be **visible** (30 s) before acting, then reads/clicks. `isHidden` = "zero matches" (count, not visibility); `isVisible` = "at least one match".
- **Strict mode:** `fill`, `click`, `readTextContent`, `readInputValue`, `readAttribute` and `waitFor` run on a single Locator, so Playwright strict mode applies: **if a selector matches two elements (even one hidden), the step throws.** A responsive restyle that duplicates an element (e.g. a second search box in a mobile offcanvas, a second "Add to Cart" in a sticky bar, a second `a[href='/order-history']` in a mobile menu) breaks the tests even when the copy is `display:none`.
- **Text reads use `textContent`**, not `innerText`: CSS `text-transform`, `::before` content and hidden-by-CSS text do not matter, but any extra DOM text inside a hooked element (an icon font glyph, a "×" close button, a currency span, a count badge) becomes part of the value.
- **Hook vocabulary used:** `aria-label` (exact `=` and prefix `^=`), `role='alert'`, `data-notification-id`, `#root`, `#kc-login`/`#username`/`#password` (Keycloak), `href` exact values, tag names (`section`, `article`, `fieldset`, `select`, `input`, `button`, `table`, `tr`, `td`, `h1`, `p`, `a`, `nav`, `main`, `label`), classes (`.notification`, `.success`, `.error`, `.error-message`, `.field-error`, `.alert-danger`, `.form-check`, `.lead`, `.breadcrumb-item`, `table.table`), visible text (`:text-is`, `:has-text`, XPath `contains(text(), …)`), structural pseudo-classes (`:nth-child`, `:first-child`, `:has`, `:is`, `>`), and URL paths.

## 2. Where each UI loads Bootstrap and its CSS

| UI | Bootstrap | Where loaded | Own CSS |
|---|---|---|---|
| frontend-react | **5.3.2** CSS + bundle JS from jsDelivr CDN | `system/multitier/frontend-react/index.html` | `public/css/common.css` (184 lines), linked from `index.html`. No CSS imported from `src/`. |
| Java monolith (Thymeleaf) | **5.3.3** CSS + bundle JS from jsDelivr CDN | `system/monolith/java/src/main/resources/templates/layout.html` (lines 7, 158) | `src/main/resources/static/css/storefront.css` (136 lines), linked from `layout.html` |
| .NET monolith (Razor) | **5.1.0** vendored (not 5.3) | `system/monolith/dotnet/wwwroot/lib/bootstrap/dist/` via `Pages/Shared/_Layout.cshtml` (lines 16, 160) | `wwwroot/css/site.css` (14 lines) + `wwwroot/css/storefront.css` (136 lines), both linked from `_Layout.cshtml` |
| TypeScript monolith (Next.js) | **5.3.3** CSS + bundle JS from jsDelivr CDN (not an npm dependency) | `system/monolith/typescript/src/app/layout.tsx` (lines 49, 58) | `src/app/globals.css` (184 lines), imported in `layout.tsx` |

Note for Step 4/7: the .NET monolith is on Bootstrap **5.1.0**, which predates the 5.3 CSS-variable theming (`--bs-*` colour modes, `--bs-border-radius`, etc.) that the Market theme relies on. It needs a Bootstrap bump (or a CDN 5.3.x link like Java/TS) before the shared theme CSS will behave the same.

## 3. Shared chrome (every page)

| Hook (selector) | Used by | frontend-react | Monoliths |
|---|---|---|---|
| `#root` (SPA detection) | MyShopUiClient (all 3) `openDirectly`, `assertLoginRequiredFor` | `index.html` `<div id="root">` | must **not** exist in monolith pages |
| `a[href='/order-history']` (home ready + HomePage nav) | MyShopUiClient `HOME_READY_SELECTOR`, HomePage (all 3), legacy mod03 | `components/StoreHeader.tsx` nav link (signed-in) | Java `layout.html:77`, .NET `_Layout.cshtml:95`, TS `components/StoreHeader.tsx:111` |
| `a[href='/new-order']` | HomePage (all 3), legacy mod03 | `StoreHeader.tsx` nav link (can place order) | Java `layout.html:76`, .NET `_Layout.cshtml:93`, TS `StoreHeader.tsx:107` |
| `a[href='/admin-coupons']` | HomePage (all 3) | `StoreHeader.tsx` nav link (admin) | Java `layout.html:79`, .NET `_Layout.cshtml:98`, TS `StoreHeader.tsx:115` |
| `[aria-label='Sign In']` | KeycloakUiLogin (all 3) | `StoreHeader.tsx` Sign In link | Java `layout.html:66`, .NET `_Layout.cshtml:76`, TS `StoreHeader.tsx:87` |
| `input[aria-label='Search Products']` | CatalogPage (all 3) | `StoreHeader.tsx:51` | Java `layout.html:48`, .NET `_Layout.cshtml:57`, TS `StoreHeader.tsx:64` |
| `button[aria-label='Search']` | CatalogPage (all 3) | `StoreHeader.tsx:55` | Java `layout.html:49`, .NET `_Layout.cshtml:58`, TS `StoreHeader.tsx:68` |
| `nav[aria-label='Store navigation'] a:text-is('<Category>')` | CatalogPage `clickHeaderCategory` (all 3) | `StoreHeader.tsx:83-90` category links | Java `layout.html:70`, .NET `_Layout.cshtml:81`, TS `StoreHeader.tsx:94` |
| `[aria-label='Cart Item Count']` (integer text only) | ProductPage (uncommitted WIP) | `StoreHeader.tsx:64` badge, rendered only when count > 0 | Java `layout.html:56`, .NET `_Layout.cshtml:64`, TS `StoreHeader.tsx:77` |
| `[role='alert'][data-notification-id]` + `.notification.success` / `.notification.error` + children `.error-message`, `.field-error` | BasePage (all 3), legacy mod03 | `components/Notification.tsx` (rendered by `StoreLayout.tsx`) | Java `layout.html:87` + inline JS `innerHTML` strings in `new-order.html`, `order-details.html`, `admin-coupons.html`, `account.html`; .NET `_Layout.cshtml:110` + same inline JS in `NewOrder/OrderDetails/AdminCoupons/Account.cshtml`; TS `components/StoreLayout.tsx:39,51` + `new-order/page.tsx`, `order-details/page.tsx`, `admin-coupons/page.tsx` |
| `h1:has-text('Not authorized'):visible` | MyShopUiClient `openRestrictedPage` (all 3) | `auth/RequireAdmin.tsx:12`, `auth/RequireCanPlaceOrder.tsx:12` (`<h1 className="h4">`) | not used (monoliths answer 403) |
| `#kc-login`, `#username`, `#password` | KeycloakUiLogin, MyShopUiClient | Keycloak login theme (not storefront) | same |

Notification contract details: the element carrying `role="alert"` must itself carry `class="notification success|error"` and `data-notification-id` (a new id per message). The success text is the **whole `textContent`** of that element, so do not put text-bearing children (icon glyphs, a "×" close button with text, a "Success" heading) inside it. Error text comes from `.error-message`; each `.field-error` must read `field: message`.

The `/new-order` success text must keep the exact form `Success! Order has been created with Order Number <n>` (regex in NewOrderPage, all 3 + legacy).

## 4. Screens

### Catalog / home `/`

Page object: CatalogPage (Java, .NET, TS). frontend-react: `pages/Catalog.tsx`, `features/catalog/CatalogFilters.tsx`, `features/catalog/ProductCard.tsx`. Monoliths: Java `templates/catalog.html`, .NET `Pages/Index.cshtml`, TS `src/app/CatalogView.tsx` + `page.tsx`.

| Selector | Purpose | frontend-react element | Structure-dependent? |
|---|---|---|---|
| `section[aria-label='Catalog']` | scope for everything below | `Catalog.tsx:98` `<section className="col-lg-9">` | tag `section` |
| `… [aria-label='Product List']` | grid of cards | `Catalog.tsx:60` row div | must be an ancestor of the cards, inside the Catalog section |
| `… [aria-label='Product List'] article[aria-label^='Product ']` | each card; SKU = label minus `Product ` | `ProductCard.tsx:15` `<article>` | tag `article`; descendant chain |
| card `[aria-label='Product Title']` | title text; (WIP) `a[aria-label='Product Title']` clicked to open product | `ProductCard.tsx:21` `<Link>` | WIP Java requires tag `a` |
| card `[aria-label='Product Brand']` | brand text | `ProductCard.tsx:25` | — |
| card `[aria-label='Display Price']` | price text, compared as a string | `ProductCard.tsx:28` | text format |
| `… [aria-label='Product Count']` | first whitespace token parsed as int (`"12 products"`) | `Catalog.tsx:100` `<span>` | text format |
| `… select[aria-label='Sort']` | `selectOption({label})`, read `options[selectedIndex].text` | `Catalog.tsx:107-113` | **must stay a native `<select>`**; option labels are the contract |
| `fieldset[aria-label='Category Filter']` + ` label` | list of categories (all labels' text) | `CatalogFilters.tsx:19,34` | tag `fieldset`; every `label` inside is read as an option |
| `fieldset[aria-label='Brand Filter']` + ` label` | list of brands | `CatalogFilters.tsx:41,56` | same |
| `<fieldset> label:text-is('<value>')` | click a category / brand | `CatalogFilters.tsx` `form-check-label` | exact full text of the label |
| `<fieldset> .form-check:has(input:checked) label` | selected category / ticked brands | `CatalogFilters.tsx:24-37` `div.form-check > input + label` | **class `.form-check` + `:has` + input/label inside the same wrapper** |
| `… [aria-label='Clear Filters']` | no-match button; also "ready" marker | `Catalog.tsx:53` | — |
| `… p.lead` | no-match message text | `Catalog.tsx:52` | **tag `p` + class `.lead`, scoped to the Catalog section** |
| `… .alert-danger` | ERP-outage message; also "ready" marker | `Catalog.tsx:44` | **Bootstrap class** |
| READY: `section[aria-label='Catalog'] :is([aria-label='Product List'], [aria-label='Clear Filters'], .alert-danger)` | load complete | as above | `:is` over the three |

Notes: the hero `p.lead` ("Free shipping over $1,000", `Catalog.tsx:74`) is outside the Catalog section, so it is safe only while it stays outside. Do not add any other `p.lead` or `.alert-danger` inside the Catalog section, and do not add product counts or badges inside filter `<label>`s (that breaks `:text-is` and the option lists).

### Product detail `/products/{sku}` — (uncommitted WIP, Java only)

Page object: `ProductPage.java` (WIP). frontend-react: `pages/ProductDetail.tsx`, `components/Breadcrumb.tsx`. Monoliths: Java `templates/product.html` + `product-not-found.html`, .NET `Pages/Product.cshtml`, TS `src/app/products/[sku]/ProductDetailView.tsx` + `not-found.tsx`.

| Selector | Purpose | frontend-react element | Structure-dependent? |
|---|---|---|---|
| `h1[aria-label='Product Title']` | title; READY marker | `ProductDetail.tsx:107` `<h1 className="h2">` | **tag `h1`** |
| `[aria-label='Product Brand']`, `[aria-label='Product Category']`, `[aria-label='Product SKU']` | details | `ProductDetail.tsx:111-113` spans | — |
| `[aria-label='Display Price']` | price string | `ProductDetail.tsx:115` | text format |
| `[aria-label='Product Description']` | description | `ProductDetail.tsx:150` | — |
| `nav[aria-label='breadcrumb'] .breadcrumb-item` | breadcrumb trail texts | `components/Breadcrumb.tsx:18-23` | **Bootstrap class `.breadcrumb-item`**, tag `nav` |
| `input[aria-label='Product Quantity']` | fill / read value | `ProductDetail.tsx:137` | tag `input` |
| `button[aria-label='Increase Quantity']`, `button[aria-label='Decrease Quantity']` | stepper | `ProductDetail.tsx:129,141` | tag `button` |
| `button[aria-label='Add to Cart']` | add; then reads notification | `ProductDetail.tsx:145` | tag `button`; must be unique on the page |
| `[aria-label='Back to Catalog']` | not-found link; READY marker | `ProductDetail.tsx:55` | — |
| `:has(> [aria-label='Back to Catalog']) > h1` | not-found heading text ("Product not found") | `ProductDetail.tsx:52-58` `div > h1 + p + Link` | **h1 and the Back link must be direct children of the same parent; tag `h1`** |
| `main .alert-danger` | ERP-outage message; READY marker | `ProductDetail.tsx:69` inside `StoreLayout` `<main>` | **Bootstrap class + `<main>` ancestor** |
| URL `/products/{sku}` | page SKU read from path | route in `App.tsx:69` | URL path |

### Cart `/cart` — (uncommitted WIP, Java only)

Page object: `CartPage.java` (WIP). frontend-react: `pages/Cart.tsx`, `components/QuantityStepper.tsx`. Monoliths: Java `templates/cart.html`, .NET `Pages/Cart.cshtml`, TS `src/app/cart/page.tsx`.

| Selector | Purpose | frontend-react element | Structure-dependent? |
|---|---|---|---|
| `table[aria-label='Cart Lines']` | lines table; READY marker | `Cart.tsx:49` | **tag `table`** |
| `table[aria-label='Cart Lines'] tr[aria-label^='Cart Line ']` | one row per line; SKU = label minus `Cart Line ` | `Cart.tsx:67` `<tr>` | **tags `table` > … `tr`** — a card/div layout for cart lines breaks this |
| `input[aria-label='Quantity for <sku>']` | quantity value | `QuantityStepper.tsx:65` via `Cart.tsx:96` | tag `input` |
| `p.lead:has-text('Your cart is empty.')` | empty cart; READY marker | `Cart.tsx:30` | **tag `p` + class `.lead` + exact text** |
| URL `/cart` | open | `App.tsx:70` | — |

### Quick order / New order `/new-order`

Page object: NewOrderPage (all 3) + legacy mod03. frontend-react: `pages/QuickOrder.tsx` → `pages/NewOrder.tsx` → `features/orders/OrderForm.tsx` (`components/FormInput.tsx`, `SubmitButton.tsx`). Monoliths: Java `templates/new-order.html`, .NET `Pages/NewOrder.cshtml`, TS `src/app/new-order/page.tsx`.

| Selector | frontend-react element | Notes |
|---|---|---|
| `[aria-label="SKU"]` (READY marker) | `OrderForm.tsx:29` | — |
| `[aria-label="Quantity"]` | `OrderForm.tsx:40` | must stay a fillable text input (tests type `invalid-quantity`) |
| `[aria-label="Country"]` | `OrderForm.tsx:47` | fillable input |
| `[aria-label="Coupon Code"]` | `OrderForm.tsx:54` | — |
| `[aria-label="Place Order"]` | `OrderForm.tsx:61` (SubmitButton) | — |
| notification (see §3) | success text from `NewOrder.tsx:24` | exact sentence |
| URL `/new-order` | `App.tsx:74` | — |

### Order history `/order-history`

Page object: OrderHistoryPage (all 3) + legacy mod03. frontend-react: `pages/OrderHistory.tsx`, `features/orders/OrderHistoryTable.tsx`. Monoliths: Java `templates/order-history.html`, .NET `Pages/OrderHistory.cshtml`, TS `src/app/order-history/page.tsx`.

| Selector | Purpose | frontend-react element | Structure-dependent? |
|---|---|---|---|
| `[aria-label='Order Number']` | filter input | `OrderHistoryTable.tsx:161` | — |
| `[aria-label='Refresh Order List']` | refresh button | `OrderHistoryTable.tsx:172` | — |
| `table tbody tr:visible` | wait for rows | `OrderHistoryTable.tsx:183-211` | **tags `table/tbody/tr`, unscoped** (first table on the page) |
| `table tbody tr:visible:has(a) td:first-child` | order numbers | first column `Order Number` | **first column must be the order number; only data rows may contain an `<a>`** (empty row "No orders found" has none) |
| `//tr[contains(., '<orderNumber>')]` | find a row | any `<tr>` | **XPath, tag `tr`** |
| `<row>//a[contains(text(), 'View Details')]` | open details | `OrderHistoryTable.tsx:104-105` `<Link>` | **XPath on a direct text node of `<a>`**: wrapping the label in a `<span>` breaks it; an SVG icon next to the text is fine |

Column order (React): Order Number, [Customer — admin only], Order Date, SKU, Quantity, Country, Total Price, Coupon Code, Status, Actions. Only "first column = Order Number" is load-bearing.

### Order details `/order-details/{orderNumber}` (React) / `/order-details?orderNumber=` (monoliths)

Page object: OrderDetailsPage (all 3) + legacy mod03. frontend-react: `pages/OrderDetails.tsx`, `features/orders/OrderDetailView.tsx`, `components/DetailField.tsx`, `components/DataState.tsx`, `components/ErrorMessage.tsx`. Monoliths: Java `templates/order-details.html`, .NET `Pages/OrderDetails.cshtml`, TS `src/app/order-details/page.tsx`.

| Selector | Text contract | frontend-react element |
|---|---|---|
| `[aria-label='Display Order Number']` (`:visible` = READY) | order number | `OrderDetailView.tsx:16` via `DetailField.tsx:18` `<p>` |
| `[aria-label='Display Order Timestamp']` | parseable timestamp (`toLocaleString('en-US', UTC)`) | `OrderDetailView.tsx:17` |
| `[aria-label='Display Status']` | enum name exactly (`PLACED`, …) | `OrderDetailView.tsx:22` |
| `[aria-label='Display SKU']`, `'Display Country'`, `'Display Quantity'` | raw values; quantity parsed as int | `OrderDetailView.tsx:24,25,29` |
| `'Display Unit Price'`, `'Display Base Price'`, `'Display Discount Amount'`, `'Display Subtotal Price'`, `'Display Tax Amount'`, `'Display Total Price'` | `$` + decimal (only `$` stripped) | `OrderDetailView.tsx:30-41` |
| `'Display Discount Rate'`, `'Display Tax Rate'` | decimal + `%` | `OrderDetailView.tsx:32,35` |
| `'Display Applied Coupon'` | code or `None` | `OrderDetailView.tsx:43` |
| `[aria-label='Cancel Order']`, `[aria-label='Deliver Order']` | clicked; absence = hidden | `OrderDetails.tsx:68,78` (not rendered when not allowed — keep conditional rendering, not CSS hiding, since `isHidden` = zero matches) |
| `p:has-text('Order not found'):visible` | not-found | `DataState.tsx` empty branch `<p>{emptyMessage}</p>` | 
| `.alert-danger:visible, .notification.error:visible` | load error; text with `Error:` prefix stripped | `ErrorMessage.tsx:13` (`<strong>Error:</strong> …`) |

Structure-dependent: `p:has-text(...)` needs tag `p`; `.alert-danger` is a Bootstrap class and is **unscoped** — any other visible `.alert-danger` on this page is read as a load error.

### Coupon management `/admin-coupons`

Page object: CouponManagementPage (all 3). frontend-react: `pages/AdminCoupons.tsx`, `features/coupons/CouponForm.tsx`, `features/coupons/CouponTable.tsx`, `components/TableDataState.tsx`. Monoliths: Java `templates/admin-coupons.html`, .NET `Pages/AdminCoupons.cshtml`, TS `src/app/admin-coupons/page.tsx`.

| Selector | Purpose | frontend-react element | Structure-dependent? |
|---|---|---|---|
| `[aria-label="Coupon Code"]` (READY), `"Discount Rate"`, `"Valid From"`, `"Valid To"`, `"Usage Limit"` | form inputs (`Valid From/To` filled as `YYYY-MM-DDTHH:mm`, so must stay `datetime-local`) | `CouponForm.tsx:52-105` | input types |
| `[aria-label="Create Coupon"]` | submit | `CouponForm.tsx:116` | — |
| `[aria-label="Refresh Coupon List"]` | refresh (TS only) | `CouponTable.tsx:92` | — |
| `[aria-label='Coupons Table']` | wait for table | `CouponTable.tsx:99` `<table>` | .NET puts it on a wrapper `<div>` (`AdminCoupons.cshtml:65`) — fine as an ancestor |
| Java/.NET: `table.table tbody tr td:nth-child(1..6)` | Code, Discount Rate, Valid From, Valid To, Usage Limit, Used Count | `CouponTable.tsx:28-60` column order | **Bootstrap class `table.table` + `:nth-child` column order, unscoped** (any other `table.table` on the page would mix in) |
| TS: `[aria-label='Coupons Table'] tbody tr` → `td` by index 0..5, rows with < 6 cells skipped | same columns | same | **column order + cell count** |
| `table.table tbody tr td:has-text("No coupons found")` / TS `tbody tr td:has-text("No coupons found")` | empty state | `TableDataState.tsx` colspan row via `CouponTable.tsx:117` | tag `td`, exact text |

Cell text contract: discount `15%`-style (`%` stripped), dates parseable or `Immediate` / `Never`, usage limit integer or `Unlimited`, used count integer. The 7th column (Status) is not read, but must stay **after** column 6.

### Screens with no driver yet

Checkout (`pages/Checkout.tsx`), OrderConfirmation (`pages/OrderConfirmation.tsx`), Account (`pages/Account.tsx`), SignIn (`pages/SignIn.tsx`) have no page object today. They already carry aria-labels that the upcoming UC-007..011 acceptance tests are expected to use — keep them unchanged:

- Checkout: `Shipping Address` (section), `Use Saved Address`, `Saved Address`, `Ship to a Different Address`, `Save as Default Address`, address field labels from `features/checkout/address-labels.ts`, `Checkout Coupon Code`, `Place Checkout Order`, `Order Review`, `Review Line <sku>`, `Review Quantity <sku>`, `Review Line Total <sku>`, `Review Subtotal`, `Tax Country`, `Estimated Tax Rate`.
- OrderConfirmation: `Confirmed Orders`, `Confirmed Order <orderNumber>`, `Rejected Lines`, `Rejected Line <sku>`, `Rejection Reason <sku>`.
- Cart (not yet read by WIP CartPage): `Cart Line Count`, `Title <sku>`, `Unit Price <sku>`, `Line Total <sku>`, `Remove <sku>`, `Decrease/Increase` stepper labels, `Cart Subtotal`, `Proceed to Checkout`.
- Catalog card: `Add <sku> to Cart`. Header: `Cart`. Account: `First Name`, `Last Name`, `Email`, `Tax Country Hint`, `Save Profile`. Order history: `Customer Filter`. Order details: `Display Shipping Address`. `ProductImage`: `role="img"` + `aria-label="<brand> product image"`.

## 5. Shared component → hook map (frontend-react)

| Component | Hooks it must keep |
|---|---|
| `components/StoreHeader.tsx` | `role="search"` form; `Search Products` input; `Search` button; `Cart` link; `Cart Item Count` badge (integer text only, rendered only when > 0); `Sign In`; `nav[aria-label='Store navigation']` with category links whose text is exactly the category; plain `href`s `/new-order`, `/order-history`, `/admin-coupons` (each exactly once on the page) |
| `components/StoreLayout.tsx` / `Layout.tsx` | `<main>` wrapping page content (ProductPage `main .alert-danger`); renders Breadcrumb and Notification |
| `components/Notification.tsx` | `role="alert"` + `class="notification success|error"` + `data-notification-id` on the same element; `.error-message`, `.field-error` children; no extra text inside |
| `components/Breadcrumb.tsx` | `nav[aria-label="breadcrumb"]` + `.breadcrumb-item` per crumb |
| `components/ErrorMessage.tsx` | `.alert-danger` + `<strong>Error:</strong>` prefix |
| `components/DataState.tsx` | empty state as `<p>{emptyMessage}</p>` |
| `components/TableDataState.tsx` | empty state as a single `<td colSpan>` with the exact message |
| `components/FormInput.tsx`, `SubmitButton.tsx` | pass `aria-label` through to the `<input>` / `<button>` |
| `components/QuantityStepper.tsx` | `aria-label` on the `<input>` and on both buttons |
| `components/DetailField.tsx` | `aria-label` on the value element, value text only |
| `components/AccountNav.tsx` | duplicates `/order-history`, `/new-order`, `/admin-coupons` hrefs — only safe because it is not rendered on the home page; do not add it there |
| `components/StoreFooter.tsx`, `LoadingSpinner.tsx`, `ProductImage.tsx`, `SortableHeaderCell.tsx`, `UserMenu.tsx`, `ErrorBoundary.tsx` | no driver hooks (but footer must not add the three nav hrefs) |
| `auth/RequireAdmin.tsx`, `auth/RequireCanPlaceOrder.tsx` | `<h1>` with text `Not authorized` |
| `features/catalog/CatalogFilters.tsx` | `fieldset[aria-label]`, `div.form-check > input + label` per option, label text = option value only |
| `features/catalog/ProductCard.tsx` | `<article aria-label="Product <sku>">`, `a[aria-label="Product Title"]`, `Product Brand`, `Display Price` |
| `features/coupons/CouponTable.tsx` | `<table class="table" aria-label="Coupons Table">`, column order, single-cell empty row |
| `features/orders/OrderHistoryTable.tsx` | first column Order Number, `View Details` as a direct text node of the only link in each row |

## 6. Fragile (structure-dependent) selectors — checklist for restylers

| # | Selector | Drivers | Binds to (frontend-react) | What breaks it |
|---|---|---|---|---|
| 1 | `.form-check:has(input:checked) label` inside `fieldset[aria-label=…Filter]` | CatalogPage ×3 | `features/catalog/CatalogFilters.tsx` | renaming `.form-check`, moving the input out of the wrapper, replacing radios/checkboxes with buttons/chips |
| 2 | `fieldset[…] label` / `label:text-is('<value>')` | CatalogPage ×3 | `CatalogFilters.tsx` | counts or extra text in labels, extra `<label>` in the fieldset, replacing `fieldset` |
| 3 | `section[aria-label='Catalog'] p.lead` | CatalogPage ×3 | `pages/Catalog.tsx:52` | dropping `.lead`/`<p>`, adding another `p.lead` inside the section |
| 4 | `section[aria-label='Catalog'] .alert-danger` (+ READY `:is(...)`) | CatalogPage ×3 | `pages/Catalog.tsx:44` | dropping the Bootstrap alert class |
| 5 | `[aria-label='Product List'] article[aria-label^='Product ']` | CatalogPage ×3 | `pages/Catalog.tsx:60`, `features/catalog/ProductCard.tsx:15` | changing `<article>`, moving cards out of the Product List element |
| 6 | `select[aria-label='Sort']` | CatalogPage ×3 | `pages/Catalog.tsx:107` | replacing the native select with a custom dropdown |
| 7 | `nav[aria-label='Store navigation'] a:text-is('<Category>')` | CatalogPage ×3 | `components/StoreHeader.tsx:83-90` | extra text in links (counts), moving category links out of that nav, duplicating them in a hidden mobile menu |
| 8 | `table.table tbody tr td:nth-child(1..6)` (unscoped) | CouponManagementPage Java/.NET | `features/coupons/CouponTable.tsx` | reordering columns, dropping `.table`, a second `table.table` on the page, card layout |
| 9 | `[aria-label='Coupons Table'] tbody tr td` by index, `< 6 cells` = empty row | CouponManagementPage TS | `CouponTable.tsx`, `components/TableDataState.tsx` | same, plus splitting the empty row into several cells |
| 10 | `table tbody tr:visible:has(a) td:first-child` (unscoped) | OrderHistoryPage ×3 | `features/orders/OrderHistoryTable.tsx` | moving Order Number off column 1, linking the order number cell plus another cell, a link in the empty row, another table before it |
| 11 | XPath `//tr[contains(., …)]//a[contains(text(), 'View Details')]` | OrderHistoryPage ×3, legacy mod03 ×3 | `OrderHistoryTable.tsx:104` | wrapping "View Details" in a span, icon-only button, renaming the text |
| 12 | `p:has-text('Order not found')` | OrderDetailsPage ×3 | `components/DataState.tsx` | changing the empty state tag from `<p>` |
| 13 | `.alert-danger:visible, .notification.error:visible` (unscoped) | OrderDetailsPage ×3 | `components/ErrorMessage.tsx`, `components/Notification.tsx` | dropping `.alert-danger`/`Error:` prefix; any other visible `.alert-danger` on the page |
| 14 | `[role='alert'].notification.success/.error` + `.error-message` / `.field-error`, `data-notification-id` | BasePage ×3, legacy mod03 ×3 | `components/Notification.tsx` | renaming classes, splitting role and class onto different elements, adding text inside (icons with text, close "×") — Bootstrap's `.alert` can be **added**, not substituted |
| 15 | `:has(> [aria-label='Back to Catalog']) > h1` (WIP) | ProductPage Java | `pages/ProductDetail.tsx:52-58` | wrapping the h1 or the link in another element, changing `h1` |
| 16 | `main .alert-danger` (WIP) | ProductPage Java | `pages/ProductDetail.tsx:69` + `StoreLayout.tsx` `<main>` | dropping `.alert-danger` or `<main>` |
| 17 | `h1[aria-label='Product Title']` (WIP) | ProductPage Java | `pages/ProductDetail.tsx:107` | changing the tag |
| 18 | `nav[aria-label='breadcrumb'] .breadcrumb-item` (WIP) | ProductPage Java | `components/Breadcrumb.tsx` | replacing Bootstrap breadcrumb markup |
| 19 | `table[aria-label='Cart Lines'] tr[aria-label^='Cart Line ']` (WIP) | CartPage Java | `pages/Cart.tsx:49,67` | cart lines as cards/divs instead of table rows |
| 20 | `p.lead:has-text('Your cart is empty.')` (WIP) | CartPage Java | `pages/Cart.tsx:30` | dropping `.lead`/`<p>`, rewording |
| 21 | `h1:has-text('Not authorized')` | MyShopUiClient ×3 | `auth/RequireAdmin.tsx`, `auth/RequireCanPlaceOrder.tsx` | changing the heading tag or text |
| 22 | `#root`, exact `href`s, `a[aria-label='Product Title']` (WIP) | MyShopUiClient ×3, HomePage ×3, CatalogPage Java WIP | `index.html`, `StoreHeader.tsx`, `ProductCard.tsx:21` | renaming root id; making the nav items buttons; turning the title link into a non-`a` |

Cross-cutting: never duplicate a hooked element in the DOM (strict mode), and keep every hooked element's text content limited to the value the tests read.
