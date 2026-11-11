# 2026-11-11 18:14:38 UTC — Market UI redesign for all storefront screens

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-11T18:30:58Z`

## TL;DR

**Why:** The storefront looks like a default Bootstrap app. After exploring nine home-page directions (`docs/design/home-redesign/`), the "Market" direction was chosen: dense, practical and modern, for a technical audience.
**End result:** All 11 storefront screens in frontend-react, then the Java, .NET and TypeScript monolith UIs, match the approved Market mockups (light theme). The look comes from Bootstrap 5.3 re-themed through its variables plus one small `market.css`, with Lucide icons and self-hosted Figtree; almost nothing else is hand-built. Markup, test labels and Bootstrap hook classes are unchanged, so the acceptance/e2e system tests stay green in all three languages with no test changes. Products show catalog images (photos only in hero/promo tiles, credited), and only data-backed extras appear. Unchanged: backend, APIs, data model, test code. The eight other explorations stay in `docs/design/home-redesign/`.

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
- **Monolith UIs are in this plan (Step 7)**, done after frontend-react and reusing the same theme CSS, so every variant looks the same for students.
- **Photos only in hero/promo tiles.** Product cards and detail pages show the image from catalog data (the existing `ProductImage` component), with a clean placeholder when there is none. Any Unsplash photo that ships gets a footer credit and stays listed in `SOURCES.md`.
- **Mockup-only elements appear only where data supports them.** "Deal" badges, the price filter, delivery-time lines and stock status ship only if the backend already provides the data (the existing "Free shipping over $1,000" message stays). No fake deals or delivery promises; anything unsupported becomes a follow-up idea, not part of this styling-only plan.
- **No dark mode in this plan.** Light Market theme only; because colours are theme variables, dark mode can be added later as a second set of values.
- **Test selectors stay as they are (Step 2 skipped).** Since Bootstrap stays, the driver selectors that use `.alert-danger`, `.form-check` and `table.table` keep working. Migrate them to role/ARIA only if Bootstrap is ever replaced.
- **Not now:** Tailwind CSS, and React-only kits (MUI, Chakra, Mantine, shadcn/ui). Tailwind is a possible later move once driver selectors are role/ARIA-based and a Node build in the monolith UIs is acceptable.

## ▶ Next executable step (resume here)

Steps 1, 4, 5 done: Market tokens in `system/multitier/frontend-react/src/styles/market.css` (Bootstrap 5.3 from npm, Fontsource Figtree, lucide-react); shared shell restyled (header, nav, footer with a `PHOTO_CREDITS` array, notifications, states, form parts). Next: Step 6, restyle the screens in order (Catalog first), using the test-hook inventory at `docs/design/home-redesign/test-hooks-inventory.md` (Step 1) so no driver selector breaks. Session scope agreed with the user: frontend-react only (Steps 1, 4, 5, 6, 8); per screen run tsc + lint + vitest; one `--sample` system-test run per language (TS, Java, .NET) at the end of the seam. Step 7 (monoliths), 9, 10 go in a later session.

## Steps

- [ ] Step 2 (skipped while Bootstrap stays — see Decisions): **Decouple tests from Bootstrap classes.** Move the driver selectors `.alert-danger` (CatalogPage, OrderDetailsPage), `.form-check:has(input:checked) label` (CatalogPage) and `table.table tbody tr td:nth-child(n)` (CouponManagementPage) to role/ARIA equivalents in all three languages, latest and legacy. Run `--sample` system tests per language.
- [x] Step 3 (approved by the user 2026-11-11): **Mock the remaining screens** on the canvas in the Market style (ProductDetail, Cart, Checkout, OrderConfirmation, NewOrder, QuickOrder, OrderHistory, OrderDetails, Account, AdminCoupons, SignIn, plus empty, loading and error states). Get user approval.
- [ ] Step 6: **Restyle screens one at a time**: Catalog (with filter sidebar and dense cards) → ProductDetail → Cart → Checkout → OrderConfirmation → OrderHistory → OrderDetails → NewOrder/QuickOrder → Account → SignIn → AdminCoupons. After each: `npx tsc --noEmit`, then run the `--sample` system tests for TypeScript, Java and .NET (they all drive the same UI).
- [ ] Step 7: **Monolith UIs.** First bump the .NET monolith from its vendored Bootstrap 5.1.0 (`wwwroot/lib`) to 5.3.x, because the Market theme needs 5.3 theme variables (Java and TS load 5.3.3 from a CDN). Use the hook map in `docs/design/home-redesign/test-hooks-inventory.md`. Apply the same tokens and styling to each monolith frontend that has its own UI, keeping their test hooks, then run the `--sample` system tests for each language.
- [ ] Step 8: **Images.** If product or hero photos ship, copy them from `docs/design/home-redesign/images/`, add footer credits, and keep `SOURCES.md` current.
- [ ] Step 9: **Full verification.** Run `./compile-all.sh`, then `./test-all.sh` (latest + legacy, all three languages). Do a visual check at desktop and phone widths.
- [ ] Step 10: **Docs.** Point `docs/design/home-redesign/README.md` at the chosen direction and the token file.

