# 2026-09-17 09:58:34 UTC — TypeScript best-practice fixes across all TypeScript projects

## TL;DR

**Why:** A read-only review of the four TypeScript projects (`system/monolith/typescript`, `system/multitier/backend-typescript`, `system/multitier/frontend-react`, `system-test/typescript`) found the code modern and well-linted (zero `any`, zero suppressions, type-aware ESLint), but with a handful of real defects: money round-trips through float, the backend isn't in strict mode, untrusted input is `as`-cast instead of parsed, a few user-visible frontend bugs, and a system-test DSL "thenable" trap that hangs instead of failing.
**End result:** All four projects compile under `strict` with the extra safety flags, money stays exact end to end, every trust boundary (request bodies, HTTP/JSON responses) is parsed rather than cast, the known frontend and test-DSL bugs are fixed, and the low-priority hygiene items (dead code, unused deps, enums, `engines`) are cleaned up.

## Outcomes

- Money is never a JS float between input and persistence/response: `Decimal` end to end, rounded once (`ROUND_HALF_UP`, 2 dp), serialized as strings — in both backends.
- `backend-typescript` compiles with `"strict": true` + `noUncheckedIndexedAccess` + `noImplicitOverride`, matching the other projects.
- Invalid request input (wrong types, NaN, invalid dates) returns 422 with field errors, never 500 — in both backends.
- HTTP/JSON boundaries are validated (type guards or a schema lib), so a malformed response fails with a clear message instead of a downstream `TypeError`.
- Coupon usage limits can't be exceeded under concurrent orders.
- Frontend: coupon-load errors are shown, the coupon form keeps input on a failed save, clearing the discount field doesn't produce `NaN`, and the order form resets to a consistent initial state.
- System-test: an incomplete scenario (`await scenario.when().placeOrder()` with no `.then()…`) fails immediately with a clear message instead of hanging to the 60s timeout; local `npm test` scripts behave like CI.
- Dead code and unused dependencies removed; `engines: node >= 22` declared everywhere; `enum`s replaced by union / `as const`.

## ▶ Next executable step (resume here)

**Step 5 — parse, don't cast, request input** (both backends). Monolith `src/lib/validation.ts`, `app/api/coupons/route.ts`, `app/api/orders/route.ts`: validator returns `{ ok: true, value } | { ok: false, errors }`; reject non-number/NaN/non-string/invalid-date with 422. Backend: replace `custom-validation.pipe.ts` with Nest `ValidationPipe({ transform: true, exceptionFactory })`, preserving the current error-response shape. Note Steps 1–4 landed: backend is now `strict`, money is `Decimal` internally (monolith rounds in `lib/db.ts`, backend via `numeric.transformer.ts`), and the frontend now sends `discountRate: null` for an empty field, so the backend must return 422 for it. Gate: typecheck, lint, unit tests, the Docker-backed component/pact/integration suites, and `--sample` system tests for monolith + multitier TypeScript.

## Steps

### Medium — trust boundaries, races, framework idioms

- [ ] **Step 5: Parse, don't cast, request input.** Monolith `src/lib/validation.ts:35,60`, `app/api/coupons/route.ts:21-30`, `app/api/orders/route.ts:54,57` — validator returns `{ ok: true, value } | { ok: false, errors }` (or zod); reject non-number/NaN/non-string/invalid-date with 422. Backend: replace hand-rolled `custom-validation.pipe.ts` (2 `eslint-disable`s) with Nest `ValidationPipe({ transform: true, exceptionFactory })`, preserving the current error-response shape.
- [ ] **Step 6: Validate HTTP/JSON responses.**
  - Monolith `src/lib/external.ts:25,45,58,77` and page fetches (`order-details/page.tsx:71,77`).
  - Backend `erp.gateway.ts:43,70`, `tax.gateway.ts:40`, `clock.gateway.ts:54` — one `fetchJson<T>()` helper, typed `HttpStatusError` instead of message-prefix rethrow, `new Error(msg, { cause })`, `encodeURIComponent` on `sku`/`country`.
  - Frontend `common.ts:119-155` — guards on responses; separate `fetchNoContent(): Promise<Result<void>>` instead of `undefined as T`.
  - System-test `json-http-client.ts:24-55` — build a full `SystemError` (`fieldErrors: []`) instead of `{ message } as unknown as E`, so `then-place-order.ts:382` fails as an assertion, not a `TypeError`.
- [ ] **Step 7: Coupon usage race.** Monolith `orders/route.ts:30`→`:122` and backend `coupon.service.ts:69-77` — transaction, or conditional `UPDATE … SET used_count = used_count + 1 WHERE used_count < usage_limit` and check row count.
- [ ] **Step 8: Backend Nest idioms.** `order.controller.ts:16,32,41` — `@HttpCode(204)` / `@Res({ passthrough: true })` instead of raw `@Res()`. `component-harness.ts:101-149` — build from `AppModule` with `.overrideProvider(...)` and a shared `configureApp(app)` used by `main.ts` (currently drifted: missing `AdminController`). `app.module.ts:28` — parse `POSTGRES_DB_PORT`. `main.ts:30` — `bootstrap().catch(...)` with exit.
- [ ] **Step 9: System-test config and runner hygiene.**
  - Replace top-of-file `process.env.EXTERNAL_SYSTEM_MODE = …` in 23 spec/fixture files with a Playwright option fixture (`test.use({ externalSystemMode: 'stub' })`); parse env once with a throwing guard (`withApp.ts:15-16`, `test-setup.ts:35`, `UseCaseDsl.ts:9`).
  - `package.json` scripts: `--grep-invert @isolated` on the parallel scripts + a separate `--workers=1` isolated script, mirroring CI.
  - `withApp.ts:5,19` — use Playwright's `browser` fixture (or a worker-scoped one) instead of launching Chromium per test; `try/finally` cleanup.
- [ ] **Step 10: Frontend dead code and scripts.** Delete `common.ts:6-102` (unused `showNotification`/`showApiError`/`handleResult`, direct `innerHTML`) and `hooks/useNotification.ts` — verify first: `pages/AdminCoupons.tsx` calls a `handleResult`, so confirm which one before deleting. Fix `test:pact` (points at non-existent `src/test/pact`) and `test:unit` (misses `src/test/unit/*.unit.test.ts`; re-check — it currently points at `src/test/harness.test.tsx`, which exists). Add tests for Step 2's fixes (coupon-load error shown, form keeps input on failed save); refresh the stale "NaN state" comment in `ui-frontend-driver.tsx` `publishCoupon`. `useCoupons.ts:65` — call `refresh()` directly instead of `setTimeout(refresh, 100)`.

### Low — hygiene

- [ ] **Step 11: Types.** `enum OrderStatus` → string-literal union / `as const` object in frontend `types/api.types.ts:3` and system-test `common/domain/OrderStatus.ts:1` (`hasStatus` takes `OrderStatus`); monolith `db.ts:28` `status: string` → `OrderStatus`; backend `externalSystemMode: string` → `'real' | 'stub'`. Monolith: move `FieldError`/`ErrorData` redeclared in 4 pages into one shared `lib/api-types.ts`.
- [ ] **Step 12: tsconfig / lint consistency.** Add `noUncheckedIndexedAccess` (monolith, system-test), `noImplicitReturns` + `noFallthroughCasesInSwitch` (monolith), `noImplicitOverride` (system-test), `isolatedModules` (system-test); enable `verbatimModuleSyntax` or `@typescript-eslint/consistent-type-imports` everywhere. Remove leftovers: backend `baseUrl`/`declaration`/`allowSyntheticDefaultImports`, frontend unused `baseUrl` + `@/*` alias. Frontend: `tsconfig.app`/`tsconfig.node` split so `vite.config.ts` is type-checked (and fix `__dirname` in ESM).
- [ ] **Step 13: package.json.** `"engines": { "node": ">=22" }` in all four. Backend: remove unused `nock`, `@eslint/eslintrc`, `ts-loader`, `source-map-support`. System-test: drop direct `playwright` dep (re-exported by `@playwright/test`), align `@playwright/test` minimum, `@types/node` → `^22`.
- [ ] **Step 14: Dead code.** Backend `AppService.getHello`, `getAppConfig`/`AppConfig`. System-test `ThenFailureAnd` (`then-place-order.ts:401`), no-op `withOrderNumber()` (`when-place-order.ts:19`).
- [ ] **Step 15: Small correctness / test-quality items.**
  - Monolith `errors.ts:71` — don't return internal error messages in 500 responses.
  - Monolith `src/__tests__/app.spec.ts` placeholder — add unit tests for `validation.ts` / `decimal-format.ts`.
  - Replace `!` after `toBeDefined()` with `toMatchObject` / optional chaining (monolith `db.integration.spec.ts:97-100`, system-test 14 sites, backend `global-exception.filter.ts:177`).
  - System-test then-stages: add context to `expect(result.success).toBe(true)` (e.g. `expect(result.success, JSON.stringify(result))`).
  - Frontend: `setFormData(prev => …)` updater form in `CouponForm.tsx`; remove redundant `cleanup()` in `test/setup.ts`; keyboard/`aria-sort` on sortable headers (`CouponTable.tsx:103`, `OrderHistoryTable.tsx:159`); drop double filtering in `OrderHistoryTable.tsx`.

## Follow-up plans (out of scope here)

- **Cross-language parity plan** (Java, .NET, TypeScript — decided 2026-09-17): check the Java/.NET twins for the same defects as Steps 4, 5 and 7 (money round-trip, cast-instead-of-parse input validation, coupon usage race) and fix them in all three. Also carries:
  - **Money wire format → strings** (`"20.00"`), the recorded string-canonical direction. It changes the API contract, so every backend, all three system-test suites, Pact contracts and frontend types move together in one coordinated change.
  - **Blackout message fix.** Every implementation (all Java/.NET/TS backends, backend-clean-java `YearEndBlackoutPolicy`) enforces 22:00–22:30 inclusive, and every test's boundary data agrees (22:30:00 blocked, 22:30:01 allowed), but the error message everywhere says "between 22:00 and 23:00". Behavior is the consistent spec; change the message to "between 22:00 and 22:30" in all backends, system tests (3 languages), component tests and `frontend-react/src/test/interactions/order.interactions.ts:140`.
- **Frontend major upgrades plan** (decided 2026-09-17): React 19, Vite 7, Vitest 3, kept separate because it carries upgrade risk rather than best-practice fixes.

## Deferred

- **Course materials:** whether `OrderStatus` enum → union (Step 11) or system-test env-mode fixtures (Step 9) appear in docs/articles that need re-sync. Deferred by the user 2026-09-17; check before executing those steps.
