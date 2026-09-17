# 2026-09-17 07:40:20 UTC — Give `frontend-react` a real linter instead of a second `tsc --noEmit`

## TL;DR

**Why:** `system/multitier/frontend-react/package.json` defines `"lint": "tsc --noEmit"` — identical to its `typecheck` script. The commit stage ("Run Linter") and `_prerelease-pipeline.yml` ("Run Linter (multitier frontend)") both call `npm run lint`, so the frontend is type-checked twice and never linted. The frontend has no ESLint dependency or config at all. Surfaced while resolving [optivem/gh-optivem#61](https://github.com/optivem/gh-optivem/issues/61) (explicit `npm run typecheck` scripts, shop `a79867d9`).
**End result:** `npm run lint` in `frontend-react` runs ESLint (flat config, TypeScript + React rules) and fails CI on violations, matching what the monolith (`eslint-config-next`) and multitier backend (`typescript-eslint`) already do. Type checking stays exclusively in `npm run typecheck`.

## Outcomes

- `frontend-react` "Run Linter" step catches lint violations (unused vars, hooks-rule breaches, etc.), not type errors a prior step already caught.
- `lint` and `typecheck` have distinct, non-overlapping jobs in every TypeScript project.
- Commit-stage workflow and `_prerelease-pipeline.yml` need **no edits** — they already call `npm run lint`.

## Current state (enumerated 2026-09-17)

| Project | `lint` script | ESLint deps | Config |
|---|---|---|---|
| `system/monolith/typescript` | `next lint` | `eslint@^9.23`, `eslint-config-next@^15.5` | `eslint.config.mjs` |
| `system/multitier/backend-typescript` | `eslint "{src,apps,libs,test}/**/*.ts" --fix` | `eslint@^9.18`, `typescript-eslint@^8.20`, prettier plugin | `eslint.config.mjs` |
| `system/multitier/frontend-react` | `tsc --noEmit` ❌ | none | none |
| `system-test/typescript` | — (no script) | none | none |

## Non-goals

- Changing the type-check path (`npm run typecheck` via `component-tests.yaml` `compileCommands`) — done in `a79867d9`.
- Adding Prettier/formatting enforcement to the frontend.
- Java/.NET linting parity.

## Open questions

1. **Backend `--fix` in CI.** `backend-typescript`'s `lint` runs `eslint ... --fix`, so in CI auto-fixable violations are silently rewritten in the runner's checkout and the step passes. *Recommendation:* split into `lint` (no `--fix`, used by CI) and `lint:fix` (local). Fold into this plan since it's the same "lint must actually gate" concern.
2. **Monolith `next lint` deprecation.** `next lint` is deprecated as of Next.js 15.5 and removed in Next.js 16. *Recommendation:* switch to `eslint .` against the existing `eslint.config.mjs` now, before a Next 16 upgrade breaks the commit stage. Fold in.
3. **`system-test/typescript` has no lint at all.** *Recommendation:* out of scope here — separate plan if wanted; it has no commit-stage lint step to fix.
4. **Student repos.** Repos scaffolded from the monolith template (e.g. `jasonribble/events-companion`) carry `next lint` too. *Recommendation:* fix the template only; students pick it up on their next template sync.

## ▶ Next executable step (resume here)

Resolve open questions 1–2 (accept or reject folding them in), then start Step 1.

## Steps

- [ ] **Step 1: Add ESLint to `frontend-react`.** Add devDependencies `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals` (the standard Vite React-TS template set), aligned to the major versions the backend already uses (`eslint@9`, `typescript-eslint@8`). Create `eslint.config.mjs` (flat config) covering `src/**/*.{ts,tsx}`; ignore `dist/`.
- [ ] **Step 2: Repoint the script.** `"lint": "eslint ."` in `frontend-react/package.json`. Keep `"typecheck": "tsc --noEmit"` unchanged.
- [ ] **Step 3: Fix existing violations.** Run `npm run lint`; fix findings in code. Only disable a rule in config with a one-line justification comment — no blanket disables to get green.
- [ ] **Step 4 (if OQ1 accepted): Backend `--fix` split.** `"lint": "eslint \"{src,apps,libs,test}/**/*.ts\""`, `"lint:fix": "... --fix"`.
- [ ] **Step 5 (if OQ2 accepted): Monolith off `next lint`.** `"lint": "eslint ."`; verify `eslint.config.mjs` already extends `next/core-web-vitals` + `next/typescript` so rule coverage is unchanged.
- [ ] **Step 6: Verify locally.** `npm ci && npm run lint && npm run typecheck && npm test` in each touched project; then `./compile-all.sh` from repo root. Deliberately introduce an unused variable in the frontend and confirm `npm run lint` fails, then revert.
- [ ] **Step 7: Commit via `/commit`, then observe CI.** Confirm `multitier-frontend-react-commit-stage` "Run Linter" runs ESLint (log shows eslint, not tsc) and is green.
