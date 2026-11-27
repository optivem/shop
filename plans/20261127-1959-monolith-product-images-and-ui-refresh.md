# 2026-11-27 19:59:24 UTC — Monolith product images + UI refresh

## TL;DR

**Why:** `plans/20261127-1904-frontend-ui-ux-refresh.md` brings ERP-owned product photos and a polished showcase path to the multitier stack (`frontend-react` + multitier backends) only. The monolith Java / .NET / TypeScript apps still show no product images and keep the older UI.
**End result:** Each monolith backend proxies product images from the ERP (`GET /api/products/{id}/image`), and each monolith UI shows the photos and the refreshed showcase path, matching `frontend-react`.

**Builds on:** the multitier plan (formerly `plans/20261127-1904-frontend-ui-ux-refresh.md`, coding done and committed, plan deleted), which provides the ERP images, `imageUrl` in the simulator and stub, the ERP contract tests, and the reference implementation in the multitier backends and `frontend-react`.

**Approach:** straight implementation, no ATDD flow. Port from the multitier reference rather than redesigning. No local system-test runs: `./compile-all.sh` is the local gate and CI runs the system tests.

## ▶ Next executable step (resume here)

Step 3 is ported but NOT committed: TypeScript is done and its checks pass (tsc, jest, lint). Java was in progress by a subagent, so check `git status system/monolith/java` and finish whatever is missing against the .NET reference (commit e99166af). Then run `./compile-all.sh` and commit Java and TypeScript separately, leaving out the `frontend-react` DemoWakeUp files (another session's work). Commits ba85210d / e99166af / 92f07ba5 are local and not pushed (the permission check blocked the push), so the user pushes. Then Step 4, Step 5, and Step 6 (CI, ask before deploy).

## Steps

- [ ] Step 3: **Monolith UIs: showcase refresh, full parity** (user decision 2026-11-27): .NET is done (e99166af). Bring Java (has base tokens/font/CTA only) and TypeScript (photos only, old Market theme) up to the .NET level: home sections, product buy box, checkout progress, mobile 44px controls. Categories menu, wishlist heart and "More in <category>" stay out in all three. Keep e2e driver selectors stable across languages.
- [ ] Step 4: **System tests**: extend the e2e coverage for images to the monolith configs and keep selectors in sync (verified in CI, not locally).
- [ ] Step 5: Run `./compile-all.sh`, commit, push, and confirm the monolith acceptance stages are green in CI.
- [ ] Step 6 (moved from the deleted multitier plan): confirm CI is green for multitier and monolith, including the photo acceptance tests `shouldShowThePhotoOfEachProduct` / `shouldShowThePhotoOfTheProduct` in .NET and TypeScript (never run locally), via a clean dispatch of `meta-prerelease-stage.yml`. Fix any red without local system-test runs. **Stop and ask user before the quick-deploy to production** (user reviews the UI locally first).

## Decisions (carried over from the multitier plan)

- No local system-test runs (user decision, 2026-11-27): CI is the only system-test gate.
- Production deploy: stop and ask first.
- `frontend-react` design choices (for user review): Bestsellers / New arrivals are hand-picked SKUs in `HomeSections.tsx`; white-on-orange buttons use `#c2410c`; Manrope bundled locally (CSP blocks Google Fonts); hero is "Featured" with live data for DELL-U2723QE; wishlist doesn't persist; no savings, was-price or stock badges.
