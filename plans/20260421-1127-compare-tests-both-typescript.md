# 20260421-1127 — TypeScript System Test Alignment Plan (both)

Reference report: [20260421-1127-compare-tests-both.md](../reports/20260421-1127-compare-tests-both.md)

Timestamp: 20260421-1127
Mode: both
Reference implementation: **Java** (align TS to Java unless explicitly stated otherwise).

All action items below respect the **Known Language-Specific Divergences (Exceptions)** list in the `compare-tests` agent spec:
- TS plural `errors/` folders for driver-port DTOs (vs Java/.NET singular `error/`) — accepted, not flagged.
- kebab-case filenames for client/service modules — accepted, not flagged.
- Two-file scenario context split (`scenario-context.ts` + `app-context.ts`) — accepted.
- Playwright fixtures / `Base*Test.ts` helper modules instead of abstract classes — accepted.
- Contract tests via `registerXxxContractTests(test)` helpers — accepted.
- `then-failure-and.ts` + inline `ThenFailureAnd` class — accepted.
- Absence of `then-success-and.ts` / `ThenSuccessAnd` — accepted.
- `ScenarioDsl.close()` method on TS only — accepted.
- `common/dtos.ts` barrel re-export file — accepted.

Ordering: architectural mismatches → architecture layers (clients → drivers → channels → use-case DSL → scenario DSL → common) → tests (acceptance → contract → e2e → smoke).

---

## 1. Architectural mismatches (legacy)

None.

## 2. Architecture — Clients

### 2.1 Reconcile `dtos/error` (singular) vs `dtos/errors` (plural) within TS

**Within TS**, driver-adapter error folders use singular `error/` while driver-port error folders use plural `errors/`. The exception list allows TS to use plural (diverging from Java/.NET), but TS must be **internally consistent** — not mix singular and plural between adapter and port.

- Rename the following TS adapter folders from `error/` to `errors/`:
  - `system-test/typescript/src/testkit/driver/adapter/external/clock/client/dtos/error/` → `errors/`
  - `system-test/typescript/src/testkit/driver/adapter/external/erp/client/dtos/error/` → `errors/`
  - `system-test/typescript/src/testkit/driver/adapter/external/tax/client/dtos/error/` → `errors/`
- Update all imports of `ExtClockErrorResponse.ts`, `ExtErpErrorResponse.ts`, `ExtTaxErrorResponse.ts` from `.../error/...` → `.../errors/...`.
- Leave the singular `error/` in Java/.NET untouched (this is the TS-only rename).
- Reference: the TS convention already in use at `system-test/typescript/src/testkit/driver/port/**/dtos/errors/`.

APPROVED

### 2.2 Verify `shared/client/playwright/withApp.ts` is needed

- File: `system-test/typescript/src/testkit/driver/adapter/shared/client/playwright/withApp.ts`
- No equivalent in Java or .NET. Either:
  - (a) If actively used by page-object adapters or fixtures, document its purpose with a top-of-file comment. Then treat as a TS-specific Playwright idiom and no further action.
  - (b) If it is dead code or only used historically, delete it.
- Recommended: perform a usage check (`grep -r "from.*withApp"` across `typescript/src` and `typescript/tests`). If there are 0 callers, delete.

APPROVED

## 3. Architecture — Drivers (ports)

### 3.1 Add missing `driver/port/external/erp/dtos/GetPromotionResponse.ts`

Java has `system-test/java/src/main/java/com/optivem/shop/testkit/driver/port/external/erp/dtos/GetPromotionResponse.java`. .NET has `system-test/dotnet/Driver.Port/External/Erp/Dtos/GetPromotionResponse.cs`. TS has only the adapter-level `ExtGetPromotionResponse.ts`.

- Add new file: `system-test/typescript/src/testkit/driver/port/external/erp/dtos/GetPromotionResponse.ts`.
- Shape: mirror Java `GetPromotionResponse.java` (fields and types).
- Update any adapter/use-case code that currently references `ExtGetPromotionResponse` at the port-level boundary to use the new port DTO.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/driver/port/external/erp/dtos/GetPromotionResponse.java`.


VJ: Wjhy in textenral the Java and .net workflows dont have Ext as prefix? Java has `system-test/java/src/main/java/com/optivem/shop/testkit/driver/port/external/erp/dtos/GetPromotionResponse.java`. .NET has `system-test/dotnet/Driver.Port/External/Erp/Dtos/GetPromotionResponse.cs`.

## 4. Architecture — Channels

None.

## 5. Architecture — Use Case DSL

### 5.1 Consolidate duplicate `use-case-context.ts`

TS currently has two files:
- `system-test/typescript/src/testkit/dsl/core/use-case-context.ts`
- `system-test/typescript/src/testkit/dsl/core/shared/use-case-context.ts`

Java has exactly one: `UseCaseContext` under `shared/`. 

- Identify which TS copy is authoritative (check with a usage grep).
- If `core/use-case-context.ts` is a barrel re-export, it may be kept as a convenience, but ensure there is only **one** source-of-truth class.
- If both define the class, delete the non-authoritative copy and update all imports to point to `core/shared/use-case-context.ts` (matching Java's `shared/UseCaseContext`).
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/shared/UseCaseContext.java`.

APPROVED

## 6. Architecture — Scenario DSL

### 6.1 Restructure `core/scenario/then/` into per-entity files (match Java)

Java structure (`dsl/core/scenario/then/`):
- `ThenImpl.java`, `ThenResultImpl.java`
- `steps/BaseThenStep.java`
- `steps/ThenClockImpl.java`, `ThenCountryImpl.java`, `ThenCouponImpl.java`, `ThenOrderImpl.java`, `ThenProductImpl.java`, `ThenFailureImpl.java`, `ThenSuccessImpl.java`

TS current structure (`dsl/core/scenario/then/`):
- `then-browse-coupons.ts`, `then-cancel-order.ts`, `then-contract.ts`, `then-place-order.ts`, `then-publish-coupon.ts`, `then-view-order.ts` — each file bundles `ThenResultStage` + `ThenSuccess` + `ThenOrder` + `ThenCoupon` + `ThenClock` + `ThenFailure` + `ThenFailureAnd` inline. The same `ThenOrder` / `ThenCoupon` / etc. classes are duplicated across files.

- Extract the duplicated `ThenOrder`, `ThenCoupon`, `ThenClock`, `ThenProduct`, `ThenCountry`, `ThenSuccess`, `ThenFailure`, `ThenFailureAnd` classes into dedicated files under `system-test/typescript/src/testkit/dsl/core/scenario/then/steps/`:
  - `system-test/typescript/src/testkit/dsl/core/scenario/then/steps/then-order.ts` — the `ThenOrder` class
  - `.../then/steps/then-coupon.ts` — the `ThenCoupon` class
  - `.../then/steps/then-clock.ts` — the `ThenClock` class
  - `.../then/steps/then-product.ts` — the `ThenProduct` class (currently only inline in `then-contract.ts`)
  - `.../then/steps/then-country.ts` — the `ThenCountry` class
  - `.../then/steps/then-success.ts` — the `ThenSuccess` class
  - `.../then/steps/then-failure.ts` — the `ThenFailure` class
  - `.../then/steps/then-failure-and.ts` — the `ThenFailureAnd` class
- Update each of the existing `then-place-order.ts`, `then-cancel-order.ts`, `then-browse-coupons.ts`, `then-publish-coupon.ts`, `then-view-order.ts`, `then-contract.ts` to import these classes rather than re-define them inline.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/then/steps/ThenOrderImpl.java` and siblings.

VJ: Did you also look at .NET? can you write inthe agent that regarding async stuff in Scenario DSL (Gherkin), to also use .NET as a refernece. Repeat this comparson with .NET/

### 6.2 Add `BaseGivenStep`, `BaseThenStep`, `BaseWhenStep` base classes

Java has:
- `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/given/steps/BaseGivenStep.java`
- `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/then/steps/BaseThenStep.java`
- `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/when/steps/BaseWhenStep.java`

TS has none of these.

- Add: `system-test/typescript/src/testkit/dsl/core/scenario/given/steps/base-given-step.ts` mirroring Java's `BaseGivenStep` shape.
- Add: `system-test/typescript/src/testkit/dsl/core/scenario/then/steps/base-then-step.ts` mirroring Java's `BaseThenStep`.
- Add: `system-test/typescript/src/testkit/dsl/core/scenario/when/steps/base-when-step.ts` mirroring Java's `BaseWhenStep`.
- Update the corresponding step classes (`given-product.ts`, `given-order.ts`, `when-place-order.ts`, etc.) to extend these base classes where they currently duplicate common context/state-management code.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/{given,then,when}/steps/Base{Given,Then,When}Step.java`.


APPROVED

### 6.3 Add `markAsExecuted()` safeguard on `ScenarioDsl`

Java `ScenarioDslImpl.markAsExecuted()` and .NET `ScenarioDsl.MarkAsExecuted()` both prevent a test from calling `.given()` or `.when()` twice on the same scenario (throws an explicit error). TS does not have this safeguard.

- File: `system-test/typescript/src/testkit/dsl/core/scenario/scenario-dsl.ts`
- Add a private `executed: boolean` flag initialized to `false`.
- Add a public `markAsExecuted(): void` method that sets `this.executed = true`.
- Modify `given()` and `when()` to throw `new Error('Scenario has already been executed. ...')` if `executed === true`.
- Have the scenario execution path (likely `ThenResultStage` once it finishes) call `markAsExecuted()` after the primary use-case executes.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/ScenarioDslImpl.java` lines 11, 22, 27, 32, 36-40.

APPROVED

### 6.4 Complete TS `DEFAULTS` to cover Java `ScenarioDefaults` keys

TS `defaults.ts` is missing some keys present in Java `ScenarioDefaults`:
- `DEFAULT_ORDER_STATUS` (OrderStatus.PLACED)
- `DEFAULT_VALID_FROM` (`2024-01-01T00:00:00Z`)
- `DEFAULT_VALID_TO` (`2024-12-31T23:59:59Z`)
- `DEFAULT_USAGE_LIMIT` (`1000`)
- `EMPTY` (null)

- File: `system-test/typescript/src/testkit/dsl/core/scenario/defaults.ts`
- Add the missing entries to the `DEFAULTS` const. Use TS-idiomatic casing (`ORDER_STATUS`, `VALID_FROM`, `VALID_TO`, `USAGE_LIMIT`, `EMPTY`).
- Import `OrderStatus` from `../../../driver/port/shop/dtos/OrderStatus.js` for `ORDER_STATUS: OrderStatus.PLACED`.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/ScenarioDefaults.java`.

VJ: Not jsut add them but use them. Cna you check if used eveyrwhrre like in Java?

## 7. Architecture — Common

None (per the exceptions list, TS-only `dtos.ts` barrel is accepted).
VJ: What does this mean?

## 8. Tests — Acceptance

None.

## 9. Tests — Contract

None.

## 10. Tests — E2E

None.

## 11. Tests — Smoke

None.

---

## Local verification & commit

1. From `system-test/typescript/`:
   - Run `Run-SystemTests -Architecture monolith` (latest suite). Do not substitute raw `npm test` / `npx playwright test`.
   - Run `Run-SystemTests -Architecture monolith -Legacy` (legacy suite).
2. Fix any failures introduced by the restructuring (most likely: broken import paths after moving Then{Entity} classes and renaming `error/` → `errors/`).
3. Commit the TypeScript alignment changes as a single logical commit with a message such as:
   `typescript: restructure scenario/then/, add base step classes, markAsExecuted safeguard, driver/port GetPromotionResponse, align DEFAULTS, unify errors/ folder naming`
