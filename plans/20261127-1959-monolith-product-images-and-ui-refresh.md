# 2026-11-27 19:59:24 UTC — Monolith product images + UI refresh

## TL;DR

**Why:** `plans/20261127-1904-frontend-ui-ux-refresh.md` brings ERP-owned product photos and a polished showcase path to the multitier stack (`frontend-react` + multitier backends) only. The monolith Java / .NET / TypeScript apps still show no product images and keep the older UI.
**End result:** Each monolith backend proxies product images from the ERP (`GET /api/products/{id}/image`), and each monolith UI shows the photos and the refreshed showcase path, matching `frontend-react`.

**Builds on:** the multitier plan (formerly `plans/20261127-1904-frontend-ui-ux-refresh.md`, coding done and committed, plan deleted), which provides the ERP images, `imageUrl` in the simulator and stub, the ERP contract tests, and the reference implementation in the multitier backends and `frontend-react`.

**Approach:** straight implementation, no ATDD flow. Port from the multitier reference rather than redesigning. No local system-test runs: `./compile-all.sh` is the local gate and CI runs the system tests.

## ▶ Next executable step (resume here)

Unblocked: the multitier reference is committed. Steps 1–3 are in progress per language (Java, .NET, TypeScript in parallel); then Step 4. Step 1: port the image proxy into `system/monolith/java`, `system/monolith/dotnet` and `system/monolith/typescript`, following how the matching multitier backend reads `imageUrl` from the ERP and streams the image.

## Steps

- [ ] Step 1: **Backend proxy (monolith)**: in each monolith backend (Java, .NET, TypeScript), read `imageUrl` from the ERP client/DTO, add `GET /api/products/{id}/image` (fetch from ERP and stream; 404 when there is no image), and expose the shop image URL on the product response, the same way the multitier backends do.
- [ ] Step 2: **Monolith UIs: images**: render product images on catalog and product detail with a placeholder fallback, and credit the photographers in the footer, matching `frontend-react`.
- [ ] Step 3: **Monolith UIs: showcase refresh**: port the `frontend-react` showcase-path polish (catalog → product detail → cart → checkout → confirmation, mobile layout). Keep e2e driver selectors stable across languages.
- [ ] Step 4: **System tests**: extend the e2e coverage for images to the monolith configs and keep selectors in sync (verified in CI, not locally).
- [ ] Step 5: Run `./compile-all.sh`, commit, and confirm the monolith acceptance stages are green in CI.
- [ ] Step 6 (moved from the deleted multitier plan): confirm CI is green for multitier and monolith, including the photo acceptance tests `shouldShowThePhotoOfEachProduct` / `shouldShowThePhotoOfTheProduct` in .NET and TypeScript (never run locally), via a clean dispatch of `meta-prerelease-stage.yml`. Fix any red without local system-test runs. **Stop and ask user before the quick-deploy to production** (user reviews the UI locally first).

## Decisions (carried over from the multitier plan)

- No local system-test runs (user decision, 2026-11-27): CI is the only system-test gate.
- Production deploy: stop and ask first.
- `frontend-react` design choices (for user review): Bestsellers / New arrivals are hand-picked SKUs in `HomeSections.tsx`; white-on-orange buttons use `#c2410c`; Manrope bundled locally (CSP blocks Google Fonts); hero is "Featured" with live data for DELL-U2723QE; wishlist doesn't persist; no savings, was-price or stock badges.
