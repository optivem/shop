# 2026-11-27 19:59:24 UTC — Monolith product images + UI refresh

## TL;DR

**Why:** `plans/20261127-1904-frontend-ui-ux-refresh.md` brings ERP-owned product photos and a polished showcase path to the multitier stack (`frontend-react` + multitier backends) only. The monolith Java / .NET / TypeScript apps still show no product images and keep the older UI.
**End result:** Each monolith backend proxies product images from the ERP (`GET /api/products/{id}/image`), and each monolith UI shows the photos and the refreshed showcase path, matching `frontend-react`.

**Builds on:** the multitier plan (formerly `plans/20261127-1904-frontend-ui-ux-refresh.md`, coding done and committed, plan deleted), which provides the ERP images, `imageUrl` in the simulator and stub, the ERP contract tests, and the reference implementation in the multitier backends and `frontend-react`.

**Approach:** straight implementation, no ATDD flow. Port from the multitier reference rather than redesigning. No local system-test runs: `./compile-all.sh` is the local gate and CI runs the system tests.

## ▶ Next executable step (resume here)

Steps 1–4 are done and pushed. Next: a clean dispatch of `meta-prerelease-stage.yml` (`gh workflow run meta-prerelease-stage.yml --ref main`; the permission check blocked Claude from dispatching it, so the user runs it or allows it). Watch it, fix any red without local system-test runs, then **stop and ask the user before the quick-deploy to production**.

## Steps

- [ ] Step 5: Confirm the monolith acceptance stages are green in CI (all code is committed and pushed; `./compile-all.sh` passes for all 7 configs). The photo tests are UI-channel acceptance tests, not limited to multitier, so they already cover the monolith configs: no `system-test/` changes were needed.
- [ ] Step 6 (moved from the deleted multitier plan): confirm CI is green for multitier and monolith, including the photo acceptance tests `shouldShowThePhotoOfEachProduct` / `shouldShowThePhotoOfTheProduct` in .NET and TypeScript (never run locally), via a clean dispatch of `meta-prerelease-stage.yml`. Fix any red without local system-test runs. **Stop and ask user before the quick-deploy to production** (user reviews the UI locally first).

## Decisions (carried over from the multitier plan)

- No local system-test runs (user decision, 2026-11-27): CI is the only system-test gate.
- Production deploy: stop and ask first.
- `frontend-react` design choices (for user review): Bestsellers / New arrivals are hand-picked SKUs in `HomeSections.tsx`; white-on-orange buttons use `#c2410c`; Manrope bundled locally (CSP blocks Google Fonts); hero is "Featured" with live data for DELL-U2723QE; wishlist doesn't persist; no savings, was-price or stock badges.
