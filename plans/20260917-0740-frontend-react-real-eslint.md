# 2026-09-17 07:40:20 UTC — Best-practice TypeScript linting across all TypeScript projects

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-09-17T07:50:38Z`

## TL;DR

**Why:** `system/multitier/frontend-react/package.json` defines `"lint": "tsc --noEmit"` — identical to its `typecheck` script — so the frontend is type-checked twice and never linted. The other TypeScript projects lint, but not to best practice: the monolith uses deprecated `next lint` with only `next/core-web-vitals` (no TypeScript rules), the backend runs `--fix` in CI and downgrades type-aware rules to warnings that never fail the build, and `system-test/typescript` has no linter at all. Surfaced while resolving [optivem/gh-optivem#61](https://github.com/optivem/gh-optivem/issues/61) (explicit `npm run typecheck` scripts, shop `a79867d9`).
**End result:** Every TypeScript project in shop (monolith, backend, frontend, system-test) — plus the monolith-derived student repo `jasonribble/events-companion` — lints with ESLint 9 flat config + typed `typescript-eslint` rules + its framework plugin, and `npm run lint` fails on any violation (errors *or* warnings). `lint` and `typecheck` have distinct jobs.

## Outcomes

- `npm run lint` catches lint violations in all four shop TypeScript projects, never duplicates `tsc`, never auto-fixes in CI, and never passes with warnings.
- One consistent best-practice baseline (below) across projects; differences are only the framework plugin.
- Commit-stage workflows and `_prerelease-pipeline.yml` need **no edits** for the three system projects — they already call `npm run lint`.

## Best-practice baseline (applies to every project)

1. **ESLint 9 flat config** in `eslint.config.mjs` (no `.eslintrc`). `FlatCompat` only where the framework config is not flat-native (`eslint-config-next@15`).
2. **`@eslint/js` recommended + `typescript-eslint` `recommendedTypeChecked` + `stylisticTypeChecked`** (typescript-eslint's documented starting point for typed linting; `strictTypeChecked` deferred until the team is fluent) with `parserOptions.projectService: true` (typed linting — catches floating promises, unsafe `any` flows, misused promises, which syntax-only rules cannot).
3. **Framework plugin:** Next → `eslint-config-next` (`core-web-vitals` + `typescript`); Vite React → `eslint-plugin-react-hooks` + `eslint-plugin-react-refresh`; Playwright → `eslint-plugin-playwright`; Nest/Jest → `globals.jest`.
4. **CI script is check-only and warning-strict:** `"lint": "eslint . --max-warnings 0"`; `"lint:fix": "eslint . --fix"` for local use.
5. **No blanket rule downgrades.** A rule set to `warn`/`off` needs a one-line justification comment; existing violations are fixed in code, not silenced.
6. **Prettier (backend only, already present):** unchanged in this plan — see Non-goals.

## Current state (enumerated 2026-09-17)

| Project | `lint` script | ESLint deps | Config |
|---|---|---|---|
| `system/monolith/typescript` | `next lint` (deprecated) | `eslint@^9.23`, `eslint-config-next@^15.5` | `eslint.config.mjs` — `FlatCompat` + `next/core-web-vitals` only |
| `system/multitier/backend-typescript` | `eslint "{src,apps,libs,test}/**/*.ts" --fix` | `eslint@^9.18`, `typescript-eslint@^8.20`, prettier plugin | `recommendedTypeChecked`, with `no-explicit-any` off and 2 rules → `warn` |
| `system/multitier/frontend-react` | `tsc --noEmit` ❌ | none | none |
| `system-test/typescript` | — (no script) | none | none |

## Decisions (resolved 2026-09-17)

- Backend `--fix` in CI → **fold in** (split `lint` / `lint:fix`).
- Monolith `next lint` → **fold in** (switch to `eslint .`).
- `system-test/typescript` → **fold in** (add ESLint).
- `jasonribble/events-companion` → **fold in** (apply the monolith + system-test changes; separate repo, separate commit).
- Strictness → **`recommendedTypeChecked` + `stylisticTypeChecked`**. Measured violations at that level: frontend 58, monolith 68, backend 17, system-test 502 errors + 132 warnings (~367 from one untyped `eachAlsoFirstRow` fixture helper; 80 `playwright/expect-expect` from DSL assertions → `assertFunctionNames`). Strict+stylistic would be 83 / 94 / 79 / 605.
- `||` → `??` fixes are behavior-reviewed individually (empty string / `0` semantics), never blind `--fix`.

## Open questions

None — all resolved before execution.

## Non-goals

- Changing the type-check path (`npm run typecheck` via `component-tests.yaml` `compileCommands`) — done in `a79867d9`.
- Changing the backend's Prettier integration (`eslint-plugin-prettier`) or adding Prettier to other projects.
- Adding a lint step to CI for `system-test/typescript` (no workflow currently runs it; local `npm run lint` only).
- Java/.NET linting parity.

## ▶ Next executable step (resume here)

Shop changes (Steps 2–7 + best-practice audit: `isRecord` request guards, typed `getParamValue` overloads replacing `!`, idiomatic fetch-in-effect hooks, `defineConfig`) are committed. Next: confirm the three TypeScript commit-stage runs (`monolith-typescript-commit-stage`, `multitier-backend-typescript-commit-stage`, `multitier-frontend-react-commit-stage`) are green, then Step 8 — regenerate the events-companion patches from the committed shop diff (rename `MyShop`→`EventsCompanion`), verify, commit there.

## Steps

- [ ] **Step 8: events-companion.** Apply Steps 3–4 equivalents to `jasonribble/events-companion` (`system/`, `system-test/`); verify; commit there separately.
- [ ] **Step 9: Observe shop CI** — the three commit-stage "Run Linter" steps run ESLint and are green.
