# 2026-11-27 19:04:43 UTC — Frontend UI/UX refresh of the shop

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-27T19:59:24Z`

## TL;DR

**Why:** The shop UI was partly restyled in the earlier "Market redesign" (commit `578234dd`: catalog filters, dense cards, promo tiles with credited Unsplash photos), but product cards still have no product imagery and the refresh is not finished or consistent across all frontends.
**End result:** `frontend-react` (the production-deployed UI) has a polished, mobile-friendly showcase path (monolith UIs deferred), and each product shows a real photo that **comes from the ERP**: the made-up ERP (simulator + stub) owns the image files and returns an `imageUrl` per product, and the shop backend proxies the image to the browser.

**Approach:** straight implementation, no ATDD flow (no ATDD agents, no acceptance-test-first). The goal is UX polish for a job-application showcase; tests are still kept green and extended where the change needs them.

## Outcomes

- **Product images are ERP data.** The ERP owns the photos the way it owns title and price: both ERP implementations (simulator `mock-server.js` and the WireMock stub) serve the same committed image files and return `imageUrl` on each product (e.g. `"imageUrl": "/erp/images/HP-15.jpg"`).
- **The ERP stays internal.** The browser never calls the ERP; the shop backend exposes `GET /api/products/{id}/image`, which fetches the image from the ERP and streams it back (same flow as prices: ERP → backend → UI).
- Each product card and product detail page shows the product photo (e.g. "HP Laptop 15" shows a laptop photo). Photos are Unsplash images downloaded and committed to the repo, so nothing depends on Unsplash at runtime.
- **Every product has a photo** — no imageless products in the simulator or stub data. A neutral placeholder exists only as a defensive fallback (image fails to load), never as an intended state.
- ERP contract tests cover the new `imageUrl` field and the image endpoint, so the stub and the simulator can't drift apart.
- Unsplash photographers are credited the same way the promo tiles already are (footer credit block).
- `frontend-react` (the one quick-deploy ships to production, i.e. what a recruiter opens) has a polished showcase path: catalog → product detail → cart → checkout → order confirmation, working well on mobile. Admin and order-history screens are out of scope. Monolith UIs are deferred.
- System tests (smoke/acceptance/e2e) stay green; UI selectors used by e2e drivers are unchanged or updated in all languages together.

## ▶ Next executable step (resume here)

Step 4 (frontend images in `system/multitier/frontend-react`). Steps 1–3 are done: the ERP serves `/erp/images/<ID>.jpg`, and the multitier backends return `imageUrl: "/api/products/<SKU>/image"` (or `null`) on `GET /api/products` and `GET /api/products/{sku}`, and serve that image publicly. Render the image on catalog cards (`src/features/catalog/ProductCard.tsx`) and on product detail (`src/pages/ProductDetail.tsx`), with a neutral placeholder `onError` fallback. Add the product photographers from `external-systems/erp-images/SOURCES.md` to the Unsplash credit block in `src/components/StoreFooter.tsx`. Keep the e2e driver selectors stable. Gate: `npx tsc --noEmit`, the frontend tests, and a local look via `./run-local.sh` dev mode.

## Steps

- [ ] Step 4: **Frontend images (`frontend-react`)** — render the product image on catalog cards and product detail via the shop image URL, with a placeholder fallback; extend the existing Unsplash credit block with the product photographers.
- [ ] Step 5: **Showcase-path refresh (`frontend-react`)** — polish the screens a visitor sees in a 2-minute click-through: catalog, product detail, cart, checkout, order confirmation, plus mobile layout (16px gutter, no horizontal scroll, touch-sized controls). Decided (revised 2026-11-27): adopt the **Retail** direction from the mockup canvas https://claude.ai/artifact/C1GPrvhmiRd5cJWktQ5bv7 (artboards "C · Retail", "Retail · Product detail", "Retail · Cart", "Retail · Checkout", "Retail · Mobile"): navy `#0b3a6f` + orange `#ea580c` on `#f4f6f9`, Manrope; utility bar, Categories button + prominent search, deal hero, service tiles, round-photo category shortcuts, Bestsellers / New arrivals rows, wishlist heart on cards. Re-theme via the Market tokens in `src/styles/market.css` on top of Bootstrap; keep e2e-driver selectors stable. Savings badges / struck-through "was" prices and wishlist are visual-only until the ERP or backend provides the data (don't invent prices).
- [ ] Step 6: **System tests** — adjust e2e coverage where Steps 4–5 change the UI (every product shows its image), keeping driver selectors in sync across languages.
- [ ] Step 7: Compile all (`./compile-all.sh`), run sample system tests per language, commit. **Stop and ask user before the quick-deploy to production** (user reviews the UI locally first).

## Decisions (resolved before execution, 2026-11-27)

- Backends: multitier only; the monolith backends and UIs are tracked in `plans/20261127-1959-monolith-product-images-and-ui-refresh.md`.
- Local Docker/system-test runs and one commit per verified step are pre-approved.
- Production deploy: stop and ask first.
- Design: ~~polish the existing Market look~~ → revised 2026-11-27: adopt the Retail direction (see Step 5).
- Legacy `mod11` ERP contract tests were left without the image tests: legacy is read-only and already has only `shouldBeAbleToGetProduct`.
