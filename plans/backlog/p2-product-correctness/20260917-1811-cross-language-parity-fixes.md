# 2026-09-17 18:11:19 UTC — Cross-language parity fixes (Java, .NET, TypeScript)

## TL;DR

**Why:** The TypeScript best-practice plan (shop `a20bd626`…`ad4a0070`) fixed defects that were *assumed* to also exist in the Java/.NET twins, and deferred three changes that only make sense when all languages move together. A read-only audit on 2026-09-17 tested that assumption: **two of the three suspected defects do not exist outside TypeScript**, so this plan is much smaller than drafted, and the one heavy item (money wire format) has been split out.

**End result:** The coupon usage-limit race is closed in the four backends that still have it, so a coupon can never be redeemed past its `usageLimit` under concurrent orders. No backend echoes an exception message to a client any more — the six that still do adopt the generic `detail` that `backend-clean-java` already returns, and `monolith/typescript` (which today neither logs the exception nor has a central handler) starts logging it server-side. The order-cancellation blackout message finally tells the truth about its own window (22:00–22:30, not 23:00), consistently across seven backends, three system-test suites, the component tests, the frontend tests and the Pact contract.

**What is explicitly unchanged:** money stays a bare JSON number on the wire (that change now lives in `plans/20260917-1928-money-wire-format-strings.md`); request validation is untouched, because all five Java/.NET backends already return 422 with `TYPE_MISMATCH` field errors; internal money types are untouched, because `BigDecimal`/`decimal` is already used end to end; the separate 23:59 order-*placement* blackout is correct and stays as it is; and `backend-clean-java` needs no race fix — it is the precedent the others copy.

## Audit findings (2026-09-17)

Read-only audit of the five Java/.NET backends (`system/monolith/{java,dotnet}`, `system/multitier/{backend-java,backend-dotnet,backend-clean-java}`) against TypeScript Steps 4, 5 and 7. Two of the three suspected defects **do not exist** outside TypeScript:

| TS defect | Java/.NET status |
|---|---|
| **Step 4 — money round-trip through float** | **Not present.** All five are `BigDecimal`/`decimal` end to end (request → pricing → persistence). No `double`/`float` leak for money or rates. Columns are `NUMERIC(10,2)` (amounts) / `NUMERIC(5,4)` (rates). |
| **Step 5 — cast-instead-of-parse input** | **Not present.** All five use framework-native validation (Jackson + Bean Validation `@Valid`; ASP.NET model binding + DataAnnotations behind a `ValidationProblemFilter` with `SuppressModelStateInvalidFilter=true`) and already return **422 with field-level `errors[]` and `TYPE_MISMATCH` codes** for wrong-typed input — never 500. |
| **Step 7 — coupon usage race** | **Present in 4 of 5.** `monolith/java`, `monolith/dotnet`, `backend-java`, `backend-dotnet` all do an unguarded read-then-write `usedCount + 1` with no transaction, no `@Version`/concurrency token and no conditional UPDATE, so concurrent orders can exceed `usageLimit`. |

**One real gap the TS plan did not name:** rounding is *implicit* in four of the five backends. `monolith/java`, `backend-java`, `monolith/dotnet` and `backend-dotnet` contain **zero** explicit rounding calls — no `setScale`/`RoundingMode`, no `Math.Round`. Values are rounded only as a side effect of Postgres coercing them into `NUMERIC(p,s)` on INSERT. Only `backend-clean-java` rounds explicitly, once per field, HALF_UP, in the `OrderPricing` canonical constructor — the structural twin of TypeScript's `roundMoney`/`roundRate`.

**`backend-clean-java` is the in-repo precedent for the race fix.** `CouponJpaRepository.redeemIfAvailable` is a conditional `UPDATE … SET used_count = used_count + 1 WHERE code = :code AND (usage_limit IS NULL OR used_count < usage_limit)`, called via `CouponRepository.tryRedeem` from `PlaceOrder`, inside the `TransactionalUseCase` decorator — exactly the TypeScript fix. It is also the only backend with a concurrency test (`CouponRedemptionConcurrencyIntegrationTest`, which additionally pins the lost-update bug against a deliberately-vulnerable legacy `update()` path).

**Wire format today:** all five emit money/rates as bare JSON numbers. The fixed-looking 2dp/4dp is *incidental* — it falls out of `BigDecimal`/`decimal` scale surviving the DB round trip. Unlike TypeScript (`decimal-format.interceptor.ts`, `numeric.transformer.ts`), there is **no formatter to repurpose**; a string wire format needs a new Jackson serializer and a new `System.Text.Json` `JsonConverter` written from scratch. Field surface is identical in every backend: 6 amounts @2dp + 2 rates @4dp on order details, `totalPrice` on order-history items, `discountRate` on coupon browse/publish.

### ⚠️ Provenance of these findings — not all equally verified

This audit was produced by subagents, and **one of them fabricated evidence on its first pass** (wrong Java package root, a message text that does not exist in the repo, non-existent file paths, and a false negative on `contracts/frontend-backend.json`) before self-correcting on a re-run. Treat the findings accordingly:

- **Verified by hand, trust these:** everything in the *blackout message* item and the *500 response* item — message text, the 16 sites, the enforced 22:00–22:30 boundary, which backends leak, which backends log, and `backend-clean-java`'s `GENERAL_ERROR_DETAIL` precedent. Each was re-checked directly against the source after the subagent's first report proved unreliable.
- **High-confidence but NOT line-by-line verified:** the money findings (no `double` leak anywhere; rounding implicit in four backends) and the validation findings (all five already return 422 + `TYPE_MISMATCH`), and in particular **"the coupon race is present in exactly `monolith/java`, `monolith/dotnet`, `backend-java`, `backend-dotnet`"**. These came from two other subagents whose reports cross-checked as mutually consistent, but were not independently confirmed.

**How to apply:** when execution first opens each of those four backends, confirm the read-then-write increment is actually there before changing it, and confirm `backend-clean-java` really is exempt. If any backend turns out already fixed, the item shrinks — do not assume the table above is the final word.

## Steps

▶ **Next:** Explicit rounding. *(Coupon race verified closed 2026-11-03: all seven backends use a conditional `UPDATE … WHERE usage_limit IS NULL OR used_count < usage_limit` inside the order-placement transaction since U1 `04d4550d`; no gap, no code change.)*

- [ ] **Explicit rounding** in `monolith/java`, `backend-java`, `monolith/dotnet`, `backend-dotnet`, mirroring `OrderPricing` (HALF_UP, once per field; 2dp amounts, 4dp rates).
- [ ] **Don't leak internal error messages in 500 responses.** *(Counts corrected 2026-09-17 by direct verification — an earlier draft of this item said "seven backends, none log", and both halves were wrong.)* **Six of seven** leak `detail: "Internal server error: <exception message>"`; `backend-clean-java` already does it right and is the precedent to copy (`GlobalExceptionHandler.java:32`, `GENERAL_ERROR_DETAIL = "An unexpected error occurred. Please try again later."`, returned at `:195`, with a code comment at `:188-190` explaining why the message must not be exposed).
  - **Extra leak in the two legacy Java backends:** `monolith/java:215-217` and `backend-java:212-214` append `" | Root cause: " + rootCauseMessage` on top of the exception message.
  - **Logging is mostly already there** — `monolith/java:212`, `backend-java:209`, `monolith/dotnet:93`, `backend-dotnet:96`, `backend-typescript:127` and `backend-clean-java:191` all call `log.error("Unexpected error occurred", ex)` or equivalent. **`monolith/typescript` is the sole outlier:** it has no central handler at all — six route-level `catch` blocks each call `internalErrorResponse(message)` (`src/lib/errors.ts:65-76`) and nothing anywhere logs, so there the exception is leaked to the client *and* lost server-side. That backend needs logging added, not just the detail genericized.
  - Verified low-risk: **no test, contract or frontend assertion anywhere pins the 500 detail text** — the only `"Internal Server Error"` matches outside the handlers are WireMock stub bodies for *external* gateway 500s (the 502 path) and Javadoc examples.
- [ ] **Blackout message fix.** *(Corrected 2026-09-17 after verification — the original wording of this item named the wrong rule and the wrong message.)* The affected message is the **order-cancellation** blackout, not order placement: `"Order cancellation is not allowed on December 31st between 22:00 and 23:00"`. The enforced window really is 22:00–22:30 inclusive (`!isBefore(22:00) && !isAfter(22:30)` in all seven backends), so the behavior is the spec and only the text is wrong. Change `23:00` → `22:30` at all **16 sites**:
  - **Producers (7):** `system/monolith/java/…/core/services/OrderService.java:149`, `system/monolith/dotnet/Core/Services/OrderService.cs:152`, `system/monolith/typescript/src/app/api/orders/[orderNumber]/cancel/route.ts:23`, `system/multitier/backend-java/…/core/services/OrderService.java:174`, `system/multitier/backend-dotnet/Core/Services/OrderService.cs:181`, `system/multitier/backend-typescript/src/core/services/order.service.ts:212`, `system/multitier/backend-clean-java/…/domain/services/YearEndBlackoutPolicy.java:43`.
  - **Consumers (9):** `contracts/frontend-backend.json:888` ⚠️ *the Pact contract pins this string — the plan previously missed it; it must move in the same commit or the frontend↔backend contract breaks*; `system-test/{java,dotnet,typescript}` `CancelOrderNegativeIsolatedTest` (`:26`, `:29`, `:12`); component tests in `backend-java:48` and `backend-clean-java:39`; `backend-clean-java/src/test/…/YearEndBlackoutPolicyTest.java:17`; `frontend-react/src/test/interactions/order.interactions.ts:140` and `…/latest/component/cancel-order.component.test.tsx:32`.
  - **Leave alone:** the separate *placement* blackout (`"Orders cannot be placed between 23:59 and 00:00 on December 31st"`, `YearEndBlackoutPolicy:32`) — its message already matches its `23:59` behavior.
  - ⚠️ **Pact provider verification is the real gate, not just the contract file.** `BackendPactVerificationTest` in `backend-java`, `backend-dotnet` and `backend-typescript` replays `contracts/frontend-backend.json` against the live backend, so the contract must be regenerated from the updated consumer fixture (`order.interactions.ts:140`) in the *same* commit as the backend message change or provider verification fails in three projects.
  - **Will not break:** the unit tests in `backend-java`, `backend-dotnet` and `backend-typescript` assert only a partial match (`hasMessageContaining("December 31")` / `Assert.Contains("December 31", …)` / `.rejects.toThrow('December 31')`), so they are insensitive to the `23:00` → `22:30` edit.
  - **Rename while you are there:** `backend-clean-java`'s unit test `allowsCancellationAfterTheWindowClosesEvenThoughItsMessageClaims2300` (`YearEndBlackoutPolicyTest.java:67`) is named for the bug and becomes nonsense once the message is truthful.

## Resolved decisions

- **Q-C (user, 2026-11-03) — explicit rounding:** add explicit rounding to `monolith/java`,
  `backend-java`, `monolith/dotnet` and `backend-dotnet`, mirroring `backend-clean-java`'s
  `OrderPricing` constructor (once per field, HALF_UP, 2dp amounts / 4dp rates), so the rounding
  rule is visible in code rather than inferred from the DDL.
- **Q-C (user, 2026-11-03) — coupon race shape:** conditional
  `UPDATE … WHERE used_count < usage_limit` (same shape as production-readiness 1.2.a). U1 already
  landed atomic coupon increments (commit `04d4550d`), so the race item is **verify-only**: confirm
  every backend is fixed and fix only a gap.
- **Q-C (user, 2026-11-03) — 500 detail text:** adopt
  `"An unexpected error occurred. Please try again later."` verbatim in the other six backends.

- **Money wire format → strings is split into its own plan** (2026-09-17). Moved to
  `plans/20260917-1928-money-wire-format-strings.md`. *Why:* the audit showed it is a breaking API
  contract change needing net-new serializers in all five Java/.NET backends (nothing exists to
  repurpose), plus the Pact contract, three system-test suites and frontend types — and it is
  entangled with the unresolved String-only DSL surface question in
  `plans/20260722-1216-string-only-money-surface.md`. Keeping it here would hold the three small,
  verified fixes hostage to it.
- **Two of the three suspected TypeScript defects need no Java/.NET work** (2026-09-17) — the money
  float round-trip and cast-instead-of-parse validation do not exist outside TypeScript. Evidence in
  *Audit findings*. The original "audit the twins and fix in all three languages" item is therefore
  reduced to the coupon race alone.
