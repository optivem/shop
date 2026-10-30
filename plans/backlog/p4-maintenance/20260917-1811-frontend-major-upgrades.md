# 2026-09-17 18:11:19 UTC — Frontend major upgrades (React 19, Vite 7, Vitest 3)

## TL;DR

**Why:** `system/multitier/frontend-react` is on older major versions of React, Vite and Vitest. The upgrades were kept out of the TypeScript best-practice plan (decided 2026-09-17) because they carry upgrade risk rather than best-practice fixes.
**End result (refined):** done as three ordered, separately committed upgrades (Vite 7, Vitest 3, React 19), each gated by build, lint, unit/component/pact tests and a system-test sample; nothing else in the repo changes. frontend-react runs on React 19, Vite 7 and Vitest 3 with typecheck, lint, unit/component/pact tests and the system tests green.

## Resolved decisions

- **Targets stay React 19, Vite 7, Vitest 3** (as scoped). Newer majors are out of scope; revisit in a separate plan. Rationale: matches the plan's original scope and keeps risk bounded.
- **One step per tool, one commit each, in order Vitest/Vite first, React last.** Vite 7 needs Node 20.19+/22.12+ (CI/Docker images use Node 22, local is 22.21.1, so fine); Vitest 3 pairs with Vite 5-7; React 19 is the riskiest (types, test libs) so it goes last in isolation.
- **Verification gate per step:** `npm run build` (tsc -b + vite build), `npm run lint`, `test:unit`, `test:component`, `test:pact`, then `gh optivem system-test run --sample` for the multitier TypeScript config. Rebuild Docker images with --no-cache (stale image pitfall).

## Steps

- [ ] **2. Vitest 2 -> 3**: bump (also `vitest.opt-in.config.ts`), review migration guide (spy/mocks reset behavior, `workspace` -> `projects` naming, reporter changes), bump jsdom if peer requires, run gate. **In progress on branch `wip/frontend-vitest-3` (`5a78ab2b`), not on main:** the bump is done, but `npm run build` fails with TS2769 at `vite.config.ts(8,13)` and `npm run lint` reports 1 error (cause not looked at). Sample tests were stopped unfinished; unit, component and pact results are unknown. Merge the branch, fix, run the full gate, commit to main, delete the branch.
- [ ] **3. React 18 -> 19**: bump `react`, `react-dom`, `@types/react`, `@types/react-dom`; fix removed APIs (`ReactDOM.render`/`propTypes`/`defaultProps` on functions, string refs, `act` import from `react`), ensure `@testing-library/react` and `eslint-plugin-react-hooks` are compatible; run gate.
- [ ] **4. Wrap-up:** run the full multitier TypeScript system tests, bump VERSION via the normal flow, confirm the frontend commit-stage workflow is green.

