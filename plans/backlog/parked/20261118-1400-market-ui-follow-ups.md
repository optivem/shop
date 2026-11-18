# 2026-11-18 14:00 UTC — Market UI follow-ups (parked)

## TL;DR

**Why:** The Market UI redesign (plan `20261111-1814`, deleted 2026-11-18 after the user's visual check) shipped as styling only. It left one deferred test-selector step and eleven ideas that need new data, endpoints or a polish pass.
**End result:** Each idea is either done or consciously dropped. Nothing here is scheduled; it gets a unit only if the user un-parks it.

## ▶ Next executable step (resume here)

Parked. Nothing to execute until the user un-parks an item. Cheapest candidates if a polish pass happens: ideas 6, 7, 8.

## Steps

- [ ] Step 2: **Decouple tests from Bootstrap classes.** Move the driver selectors `.alert-danger` (CatalogPage, OrderDetailsPage), `.form-check:has(input:checked) label` (CatalogPage) and `table.table tbody tr td:nth-child(n)` (CouponManagementPage) to role/ARIA equivalents in all three languages, latest and legacy. Run `--sample` system tests per language. — ⏳ Deferred: only needed if Bootstrap is ever replaced (the Market redesign kept Bootstrap 5.3, so these hook classes keep working).

## Follow-up ideas (left out of the styling-only plan: no backing data or behaviour)

1. **Product data for mockup extras**: deal badges, ratings, stock status, per-product shipping lines, image gallery and specs list all need new `Product` fields. Recommendation: skip unless a course module needs them.
2. **Price min/max filter** in the catalog sidebar. Needs API query support. Recommendation: skip for now.
3. **Cart and checkout shipping and estimated-total rows, plus "qualifies for free shipping"**. Needs a shipping/discount preview endpoint. Recommendation: revisit with the coupon work.
4. **Apply-coupon button with an inline error at checkout**. Needs a coupon-validate endpoint. Recommendation: revisit with the coupon work.
5. **SignIn split layout** (photo, guest link): the app redirects straight to Keycloak. A themed Keycloak login page would be the real fix. Recommendation: low priority.
6. **AccountNav: show the signed-in user's name and avatar** as in the mockup. Recommendation: cheap, do it in a polish pass.
7. **`auth/AuthGate.tsx` redirect view** is unstyled; reuse the `.signin-redirect` classes from `pages-account.css`. Recommendation: cheap, do it in a polish pass.
8. **`components/DetailField.tsx` no longer used by the order pages.** Recommendation: delete it if nothing else uses it.
9. **The mockup's "Audio" promo tile was replaced by Monitors** because the catalog has no Audio category; `headphones.jpg` and `cable.jpg` don't ship. Recommendation: keep as is.
10. **Java monolith fonts and promo images live under `static/css/`** (`css/lib/fontsource-figtree/`, `css/images/promo/`), because `SecurityConfig` only lets signed-out visitors load `/css/**`. Recommendation: keep as is. Moving them to `/lib/` and `/images/` (to match .NET) needs those paths added to `STOREFRONT_PAGES`.
11. **The TS monolith's order-history and coupon tables keep their existing columns.** The Country/Coupon and Status columns the other UIs show were not added, so the driver column contract stays safe. Recommendation: align them only together with a driver check in all three languages.
