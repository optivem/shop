# 2026-11-27 19:04:43 UTC — Frontend UI/UX refresh of the shop

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-27T19:59:24Z`

## TL;DR

**Why:** The shop UI was partly restyled in the earlier "Market redesign" (commit `578234dd`: catalog filters, dense cards, promo tiles with credited Unsplash photos), but product cards still have no product imagery and the refresh is not finished or consistent across all frontends.
**End result:** `frontend-react` (the production-deployed UI) has a polished, mobile-friendly showcase path (monolith UIs deferred), and each product shows a real photo that **comes from the ERP**: the made-up ERP (simulator + stub) owns the image files and returns an `imageUrl` per product, and the shop backend proxies the image to the browser.

**Approach:** straight implementation, no ATDD flow (no ATDD agents, no acceptance-test-first). The goal is UX polish for a job-application showcase; tests are still kept green and extended where the change needs them. From Step 7 on, verification is compile-only (user decision 2026-11-27): no local tests, no CI babysitting.

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

Step 7. Steps 1–6 are committed. Check CI for the latest pushes (`gh run list`), especially the acceptance-ui runs of the new tests `shouldShowThePhotoOfEachProduct` / `shouldShowThePhotoOfTheProduct` in .NET and TypeScript, which never ran locally. Fix any red without running system tests locally. Then **stop and ask the user before the quick-deploy to production**.

## Steps

- [ ] Step 7: Confirm CI is green for the Steps 1–6 pushes (the new photo tests in .NET/TS ran first in CI; local runs were skipped at the user's request), fix any red (no local system-test runs), then run a clean re-dispatch of `meta-prerelease-stage.yml`. **Stop and ask user before the quick-deploy to production** (user reviews the UI locally first).

## Decisions (resolved before execution, 2026-11-27)

- Backends: multitier only; the monolith backends and UIs are tracked in `plans/20261127-1959-monolith-product-images-and-ui-refresh.md`.
- No local system-test runs (user decision, 2026-11-27): CI is the only system-test gate. One commit per step, after `./compile-all.sh` passes, is pre-approved.
- Production deploy: stop and ask first.
- Design: ~~polish the existing Market look~~ → revised 2026-11-27: adopt the Retail direction (see Step 5).
- Legacy `mod11` ERP contract tests were left without the image tests: legacy is read-only and already has only `shouldBeAbleToGetProduct`.
- Steps 4–5 choices made by the executor (for user review): Bestsellers / New arrivals are hand-picked SKUs in `HomeSections.tsx` (rename to "Featured" if the labels feel like claims); white-on-orange buttons use `#c2410c` for 5.2:1 contrast; Manrope is bundled locally (the CSP blocks Google Fonts); the hero is "Featured" (not a fake deal), with live data for DELL-U2723QE; the wishlist doesn't persist; no savings, was-price or stock badges.
- `acceptance-parallel-ui` had 2/79 login timeouts (order history, admin coupons) that passed on rerun; treated as a parallel-login flake.
- Step 6/7: at the user's request, the local system-test runs for .NET/TS and the Step 7 sample runs were skipped (user was using `run-local.sh dev`); CI is the gate. Java new tests + `--sample` passed locally; `./compile-all.sh` passed except `frontend-react` `npm ci` (EPERM, locked by the running Vite dev server).
