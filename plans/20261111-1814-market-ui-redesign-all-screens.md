# 2026-11-11 18:14:38 UTC — Market UI redesign for all storefront screens

## TL;DR

**Why:** The storefront looks like a default Bootstrap app. After exploring nine home-page directions (`docs/design/home-redesign/`), the "Market" direction was chosen: dense, practical and modern, for a technical audience.
**End result:** Every storefront screen, in every frontend that has a UI, uses the Market look through one shared set of design tokens. All acceptance/e2e system tests stay green in Java, .NET and TypeScript, and the other eight explorations stay in `docs/design/home-redesign/` for reference.

## Outcomes

- A **Market design system** (CSS tokens for colour, type, radius, spacing and elevation; Figtree font; teal `#0F766E` primary, ink `#0F172A`, page `#EEF1F5`, yellow `#FDE047` deal badges) applied once and reused by every screen.
- **Approved mockups of every screen** on the design canvas before any code changes, so the look is signed off up front.
- **All screens restyled**: Catalog/home, ProductDetail, Cart, Checkout, OrderConfirmation, NewOrder, QuickOrder, OrderHistory, OrderDetails, Account, AdminCoupons, SignIn, plus the shared shell (header with utility strip, search with category select, category nav row, footer, notifications, loading and empty states).
- **Acceptance tests unaffected**: every `aria-label`, `data-testid`, role and visible label the UI drivers use is preserved. The few CSS-class hooks (`.alert-danger`, `.form-check`, `table.table` column order) are either kept or the drivers are moved to role/ARIA selectors first, in all three languages.
- **Responsive and accessible**: works at phone width, WCAG AA contrast, visible focus rings, touch targets of at least 44px.
- **Image licensing on record**: any Unsplash photo that ships carries a footer credit and is listed in `docs/design/home-redesign/images/SOURCES.md`.
- The **other eight explorations stay** in `docs/design/home-redesign/`; nothing there is deleted.

## Decisions

- **Use established libraries wherever possible and keep from-scratch work to a minimum** (user, 2026-11-11). Hand-write only the Market theme layer. Before writing any custom component (stepper, badge, modal, toast, etc.), first check whether Bootstrap or an established library already provides it.
- **CSS foundation: Bootstrap 5.3, re-themed.** Market look comes from Bootstrap's theme variables (`--bs-primary`, radius, font, shadows) plus one small `market.css`. Why: it's plain CSS, so it works in all four frontends (React + Java/.NET/TypeScript monolith UIs); the test hook classes (`.alert-danger`, `.form-check`, `table.table`) keep working; no new build tooling in the Java/.NET projects; MIT licence.
- **Icons: Lucide** (ISC). Plain SVG in the monoliths, `lucide-react` in frontend-react.
- **Font: Figtree via Fontsource** (OFL), self-hosted instead of the Google Fonts CDN.
- **Tables: keep TanStack Table** in frontend-react (headless, takes Market styling).
- **Not now:** Tailwind CSS, and React-only kits (MUI, Chakra, Mantine, shadcn/ui). Tailwind is a possible later move once driver selectors are role/ARIA-based and a Node build in the monolith UIs is acceptable.

## ▶ Next executable step (resume here)

Mockups for all screens are approved (canvas page "Market — all screens" at https://claude.ai/artifact/6PfCB87jVYkf7vfxGhEJBk; sources in `docs/design/home-redesign/designs/M*.dc.html` + `MarketHome.dc.html`). Library choices are decided (see Decisions). Next: settle the remaining Open questions with `/refine-plan`, then start Step 1 (inventory of screens and test hooks) and Step 4 (design tokens layer).

## Steps

- [ ] Step 1: **Inventory.** List every UI screen and shared component in `system/multitier/frontend-react/src` and in each monolith UI (`system/monolith/java` ≈13 UI files, `system/monolith/dotnet` ≈19, `system/monolith/typescript` ≈22). For each, record the test hooks the drivers in `system-test/{java,dotnet,typescript}/**/Ui/**/Pages` use (aria-labels, test IDs, roles, labels, CSS classes, column order).
- [ ] Step 2: **Decouple tests from Bootstrap classes (optional, decide in Open questions).** Move the driver selectors `.alert-danger` (CatalogPage, OrderDetailsPage), `.form-check:has(input:checked) label` (CatalogPage) and `table.table tbody tr td:nth-child(n)` (CouponManagementPage) to role/ARIA equivalents in all three languages, latest and legacy. Run `--sample` system tests per language.
- [x] Step 3 (approved by the user 2026-11-11): **Mock the remaining screens** on the canvas in the Market style (ProductDetail, Cart, Checkout, OrderConfirmation, NewOrder, QuickOrder, OrderHistory, OrderDetails, Account, AdminCoupons, SignIn, plus empty, loading and error states). Get user approval.
- [ ] Step 4: **Design tokens layer.** Add one Market stylesheet (CSS custom properties plus a thin component layer) to `frontend-react`. Decide whether Bootstrap stays underneath, restyled, or is replaced (Open questions).
- [ ] Step 5: **Shared shell first.** StoreHeader (utility strip, search with category select, cart badge, sign-in), category nav row, StoreFooter (with photo credits if images ship), StoreLayout/Layout, Notification, LoadingSpinner, DataState/TableDataState, ErrorMessage, FormInput, SubmitButton, QuantityStepper.
- [ ] Step 6: **Restyle screens one at a time**: Catalog (with filter sidebar and dense cards) → ProductDetail → Cart → Checkout → OrderConfirmation → OrderHistory → OrderDetails → NewOrder/QuickOrder → Account → SignIn → AdminCoupons. After each: `npx tsc --noEmit`, then run the `--sample` system tests for TypeScript, Java and .NET (they all drive the same UI).
- [ ] Step 7: **Monolith UIs.** Apply the same tokens and styling to each monolith frontend that has its own UI, keeping their test hooks, then run the `--sample` system tests for each language.
- [ ] Step 8: **Images.** If product or hero photos ship, copy them from `docs/design/home-redesign/images/`, add footer credits, and keep `SOURCES.md` current.
- [ ] Step 9: **Full verification.** Run `./compile-all.sh`, then `./test-all.sh` (latest + legacy, all three languages). Do a visual check at desktop and phone widths.
- [ ] Step 10: **Docs.** Point `docs/design/home-redesign/README.md` at the chosen direction and the token file.

## Open questions

- **Step 2 timing:** do the driver selector migration before the redesign (frees the markup) or skip it while Bootstrap stays? *Recommended: skip while Bootstrap stays; revisit if it's replaced.*
- **Monolith scope:** do the monolith UIs (Java, .NET, TypeScript) get the same redesign in this plan, or a follow-up plan? *Recommended: same plan, after frontend-react is done, so all variants stay visually consistent for students.*
- **Photos in product:** ship real Unsplash product photos in the app (needs footer credits and asset hosting), or keep generic product images and use photos only in hero/promo tiles? *Recommended: hero/promo tiles only. Product imagery should come from catalog data, not stock photos.*
- **Market-only elements:** the mockup has a "Deal" badge, price filter, "Free shipping" lines and a top utility strip. The app has no data behind some of these. Drop them, or show them only where data exists? *Recommended: show only what the backend supports today; no fake deals.*
- **Dark mode:** in scope? *Recommended: not now; the tokens make it a later add-on.*
