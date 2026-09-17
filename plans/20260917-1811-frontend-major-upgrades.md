# 2026-09-17 18:11:19 UTC — Frontend major upgrades (React 19, Vite 7, Vitest 3)

## TL;DR

**Why:** `system/multitier/frontend-react` is on older major versions of React, Vite and Vitest. The upgrades were kept out of the TypeScript best-practice plan (decided 2026-09-17) because they carry upgrade risk rather than best-practice fixes.
**End result:** frontend-react runs on React 19, Vite 7 and Vitest 3 with typecheck, lint, unit/component/pact tests and the system tests green.

## ▶ Next executable step (resume here)

Design work remains: run `/refine-plan plans/20260917-1811-frontend-major-upgrades.md` to check the migration guides and break the upgrade into steps.

## Steps

- [ ] **Upgrade React 19, Vite 7, Vitest 3** in `system/multitier/frontend-react`.
