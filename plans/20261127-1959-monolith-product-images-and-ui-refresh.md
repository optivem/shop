# 2026-11-27 19:59:24 UTC — Monolith product images + UI refresh

## TL;DR

**Why:** `plans/20261127-1904-frontend-ui-ux-refresh.md` brings ERP-owned product photos and a polished showcase path to the multitier stack (`frontend-react` + multitier backends) only. The monolith Java / .NET / TypeScript apps still show no product images and keep the older UI.
**End result:** Each monolith backend proxies product images from the ERP (`GET /api/products/{id}/image`), and each monolith UI shows the photos and the refreshed showcase path, matching `frontend-react`.

**Depends on:** `plans/20261127-1904-frontend-ui-ux-refresh.md` being done first. That plan provides the ERP images, `imageUrl` in the simulator and stub, the ERP contract tests, and the reference implementation in the multitier backends and `frontend-react`.

**Approach:** straight implementation, no ATDD flow. Port from the multitier reference rather than redesigning.

## ▶ Next executable step (resume here)

Blocked until the multitier plan is done. After that, start with Step 1: port the image proxy into `system/monolith/java`, `system/monolith/dotnet` and `system/monolith/typescript`, following how the matching multitier backend reads `imageUrl` from the ERP and streams the image.

## Steps

- [ ] Step 1: **Backend proxy (monolith)**: in each monolith backend (Java, .NET, TypeScript), read `imageUrl` from the ERP client/DTO, add `GET /api/products/{id}/image` (fetch from ERP and stream; 404 when there is no image), and expose the shop image URL on the product response, the same way the multitier backends do.
- [ ] Step 2: **Monolith UIs: images**: render product images on catalog and product detail with a placeholder fallback, and credit the photographers in the footer, matching `frontend-react`.
- [ ] Step 3: **Monolith UIs: showcase refresh**: port the `frontend-react` showcase-path polish (catalog → product detail → cart → checkout → confirmation, mobile layout). Keep e2e driver selectors stable across languages.
- [ ] Step 4: **System tests**: run the e2e coverage for images against the monolith configs too, and keep selectors in sync.
- [ ] Step 5: Run `./compile-all.sh` and sample system tests per monolith language, then commit.
