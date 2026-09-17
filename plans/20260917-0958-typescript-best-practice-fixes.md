# 2026-09-17 09:58:34 UTC — TypeScript best-practice fixes across all TypeScript projects

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-09-17T15:22:16Z`

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
- The applicable fixes are also applied to the student repo `jasonribble/events-companion` (Next.js `system/` + `system-test/`).

## ▶ Next executable step (resume here)

**Step 16 — port the fixes to `jasonribble/events-companion`** (separate repo, own commit). Steps 1–15 have landed in shop. Start by auditing which defects actually exist there (its domain differs from shop), then apply the equivalents listed in Step 16. Shop precedents to copy: `assertNotAwaited` (Step 3), zod `parse*Request` (Step 5) and response schemas (Step 6), the `common/env.ts` env parser and `test.use({ externalSystemMode })` option fixture (Step 9), `as const` + type for enums (Step 11), `consistent-type-imports` + `no-import-type-side-effects` lint rules and the tsconfig flags (Step 12). Gate: that repo's typecheck, lint and unit tests; ask before running its system tests locally.

## Steps

### Port — student repo `jasonribble/events-companion`

- [ ] **Step 16: Apply the fixes to events-companion** (local clone at `$GITHUB_ROOT/student/jasonribble/events-companion`, sibling of `optivem/`; precedent: the linting plan landed shop `0666d51d` + events-companion `26cac89`). Its layout is a Next.js `system/` (monolith-shaped) + `system-test/` (+ `external-systems/simulators`), and its domain differs from shop, so first audit which defects actually exist there, then apply the equivalents:
  - Already done in shop, still to port: Step 3 (DSL thenable guard, `assertNotAwaited` helper) and Step 4 monolith part (money/decimals exact internally, wire shape unchanged) — only if the domain has money/decimal fields.
  - Monolith/system-test parts of Steps 5, 6, 7 (if a similar usage-limit race exists), 9, 11, 12, 13, 14, 15.
  - Not applicable (no NestJS backend or React frontend): Steps 1, 2, 8, 10 and the backend/frontend parts of the other steps.
  - Gate: that repo's `npm run typecheck`, `npm run lint`, unit tests, and its system tests (ask before running them locally). Commit to that repo separately with its own message.

## Follow-up plans (out of scope here)

- **Cross-language parity plan** (Java, .NET, TypeScript — decided 2026-09-17): check the Java/.NET twins for the same defects as Steps 4, 5 and 7 (money round-trip, cast-instead-of-parse input validation, coupon usage race) and fix them in all three. Also carries:
  - **Money wire format → strings** (`"20.00"`), the recorded string-canonical direction. It changes the API contract, so every backend, all three system-test suites, Pact contracts and frontend types move together in one coordinated change.
  - **Don't leak internal error messages in 500 responses.** All six backends (monolith + multitier, Java/.NET/TypeScript) return `detail: "Internal server error: <exception message>"`. Log the exception server-side and return a generic detail in all of them together (moved here from Step 15, 2026-09-17, so the TypeScript monolith doesn't diverge from the other five).
  - **Blackout message fix.** Every implementation (all Java/.NET/TS backends, backend-clean-java `YearEndBlackoutPolicy`) enforces 22:00–22:30 inclusive, and every test's boundary data agrees (22:30:00 blocked, 22:30:01 allowed), but the error message everywhere says "between 22:00 and 23:00". Behavior is the consistent spec; change the message to "between 22:00 and 22:30" in all backends, system tests (3 languages), component tests and `frontend-react/src/test/interactions/order.interactions.ts:140`.
- **Frontend major upgrades plan** (decided 2026-09-17): React 19, Vite 7, Vitest 3, kept separate because it carries upgrade risk rather than best-practice fixes.

## Deferred

- (none — the course-materials check was cleared 2026-09-17: course docs only reference `EXTERNAL_SYSTEM_MODE` in Java / as the env var, and show no TS `OrderStatus` enum.)
