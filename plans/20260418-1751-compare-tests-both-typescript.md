# TypeScript — System Test Alignment Plan

Reference report: [`reports/20260418-1751-compare-tests-both.md`](../reports/20260418-1751-compare-tests-both.md)

TypeScript has full cross-language parity at the **abstraction-layer** level for legacy modules (no layer is wrong in any module) and no progression regressions. The remaining action items are a mix of naming, folder structure, and missing shared-base-class abstractions in legacy modules. Ordered as per the spec: architectural layers → architecture layers → tests.

## 1. Architecture — Channels — fix channel type value casing

File: `system-test/typescript/src/testkit/channel/channel-type.ts`

Java (`ChannelType.java`) and .NET (`ChannelType.cs`) both expose `UI = "UI"` and `API = "API"` (upper-case). TypeScript currently exposes lowercase: `UI: 'ui'`, `API: 'api'`. Change to upper-case to match Java:

```ts
export const ChannelType = {
    UI: 'UI',
    API: 'API',
} as const;
```

Then audit all uses — in particular `system-test/typescript/src/testkit/driver/adapter/shared/playwright/withApp.ts` line 10 (`process.env.CHANNEL?.toLowerCase() || ChannelType.API`). Remove the `.toLowerCase()` if the env var is also now upper-case, or keep the normalization but update the constant the compare is against. Also audit `system-test/typescript/src/testkit/test-setup.ts` (lines 35, 51) and `system-test/typescript/src/testkit/dsl/core/scenario/app-context.ts` (line 9).

## 2. Architecture — Drivers — align error-DTO folder nesting (keep plural `errors/`)

The Java and .NET driver-port trees nest error DTOs under an `error/` (singular) subfolder:

- `system-test/java/src/main/java/com/optivem/shop/testkit/driver/port/shop/dtos/error/SystemError.java`
- `system-test/dotnet/Driver.Port/Shop/Dtos/Error/SystemError.cs`

TypeScript uses `errors/` (plural) for shop (`system-test/typescript/src/testkit/driver/port/shop/dtos/errors/SystemError.ts`) and does **not** nest at all for external systems (`clock/dtos/ClockErrorResponse.ts`, `erp/dtos/ErpErrorResponse.ts`, `tax/dtos/TaxErrorResponse.ts` are direct children of `dtos/`).

JS/TS convention is **plural** for folders holding multiple items of the same kind, so keep the TS `errors/` folder name rather than switching to Java's singular `error/`. Still fix the inconsistency *within* TypeScript by adding the subfolder for every DTO tree:

- Keep `system-test/typescript/src/testkit/driver/port/shop/dtos/errors/` as-is.
- Create `system-test/typescript/src/testkit/driver/port/external/clock/dtos/errors/` and move `ClockErrorResponse.ts` into it.
- Create `system-test/typescript/src/testkit/driver/port/external/erp/dtos/errors/` and move `ErpErrorResponse.ts` into it.
- Create `system-test/typescript/src/testkit/driver/port/external/tax/dtos/errors/` and move `TaxErrorResponse.ts` into it.

Update every import site (including the barrel `system-test/typescript/src/testkit/common/dtos.ts`) and any adapter under `system-test/typescript/src/testkit/driver/adapter/` that references these DTOs.

Singular/plural divergence from Java/.NET is an accepted idiom difference — do **not** rename to `error/`.

## 3. Architecture — Clients — align shared-client folder nesting (keep kebab-case filenames)

The Java/.NET trees wrap shared driver-adapter infrastructure under `shared/client/<http|playwright|wiremock>/...`:

- `system-test/java/src/main/java/com/optivem/shop/testkit/driver/adapter/shared/client/http/{HttpStatus.java, JsonHttpClient.java}`
- `system-test/java/src/main/java/com/optivem/shop/testkit/driver/adapter/shared/client/playwright/PageClient.java`
- `system-test/java/src/main/java/com/optivem/shop/testkit/driver/adapter/shared/client/wiremock/JsonWireMockClient.java`

TypeScript drops the `client/` nesting:

- `system-test/typescript/src/testkit/driver/adapter/shared/http/{HttpStatus.ts, http-client.ts}`
- `system-test/typescript/src/testkit/driver/adapter/shared/playwright/{PageClient.ts, withApp.ts}`
- `system-test/typescript/src/testkit/driver/adapter/shared/wiremock/wiremock-client.ts`

Align the *folder nesting* to Java but **keep TS filename conventions**. The existing TS convention in this codebase is: PascalCase for types/DTOs/enums (`HttpStatus.ts`, `PageClient.ts`), kebab-case for modules/services (`http-client.ts`, `wiremock-client.ts`, `scenario-dsl.ts`). Do not rename client modules to PascalCase just to mirror Java — that fights the existing idiom.

- Move `shared/http/http-client.ts` → `shared/client/http/json-http-client.ts` (kebab-case, with `json-` prefix to mirror Java's `JsonHttpClient` semantically).
- Move `shared/http/HttpStatus.ts` → `shared/client/http/HttpStatus.ts` (keep PascalCase — it's a type/enum).
- Move `shared/playwright/PageClient.ts` → `shared/client/playwright/PageClient.ts` (keep PascalCase — it's a class).
- Move `shared/playwright/withApp.ts` → `shared/client/playwright/withApp.ts` (keep camelCase — it's a function).
- Move `shared/wiremock/wiremock-client.ts` → `shared/client/wiremock/json-wiremock-client.ts` (kebab-case, `json-` prefix).

Update all importers — notably every test fixture file and every driver under `shared/`, `shop/`, and `external/`.

## 4. Architecture — Use Case DSL — rename root variable to `app`

Java and .NET expose the root of the use-case DSL as `app` / `App`:

- `app.shop().placeOrder()...` in Java (`system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod07/e2e/PlaceOrderPositiveTest.java` line 15).
- `App.Shop().PlaceOrder()...` in .NET.

TypeScript uses `useCase`:

- `useCase.shop().placeOrder()...` in `system-test/typescript/tests/legacy/mod07/e2e/place-order-positive-test.spec.ts` line 13.

Rename the TS root variable (and its fixture key) from `useCase` → `app`. Touchpoints include at least:

- `system-test/typescript/src/testkit/dsl/core/use-case-context.ts` (if the variable is exported from here).
- The mod07 and higher fixtures: `system-test/typescript/tests/legacy/mod07/e2e/fixtures.ts` (and similar for mod07 smoke if applicable).
- Every test file under `tests/legacy/mod07/` that destructures `{ useCase }` from the fixture.

## 5. Architecture — Scenario DSL — document the two-file factoring

Java (`system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/ExecutionResultContext.java`) and .NET (`system-test/dotnet/Dsl.Core/Scenario/ExecutionResultContext.cs`) both have a single file named `ExecutionResultContext`. TypeScript has two files in the same directory that together play this role (`scenario-context.ts` and `app-context.ts`).

Audit `system-test/typescript/src/testkit/dsl/core/scenario/{scenario-context.ts,app-context.ts}` and determine whether the split reflects a deliberate responsibility boundary (e.g. scenario-level state vs app-channel state).

- If the split **is** deliberate: keep the two files. Add a short header comment at the top of `scenario-context.ts` explaining the factoring and noting that Java/.NET collapse both into a single `ExecutionResultContext`. No code changes otherwise.
- If the split is **incidental** (no real SRP distinction): consolidate into a single `execution-result-context.ts` to mirror Java/.NET and adjust every import across `scenario/`, `scenario-dsl.ts`, `execution-result-builder.ts`, etc.

Default: **keep the split and document**. Do not merge files just to mirror Java — TS frequently factors smaller modules than Java does, and forcing a merge works against the existing TS structure.

## 6. Architecture — Scenario DSL — verify `defaults.ts` vs Java `ScenarioDefaults.java`

Confirm that `system-test/typescript/src/testkit/dsl/core/defaults.ts` is the direct equivalent of `system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/ScenarioDefaults.java` and `system-test/dotnet/Dsl.Core/Scenario/GherkinDefaults.cs`. If so, move it under `dsl/core/scenario/defaults.ts` (or `scenario-defaults.ts`) to match the Java directory. Update imports.

## 7. Test — Acceptance — align `totalPriceShouldBeSubtotalPricePlusTaxAmount` data rows

File: `system-test/typescript/tests/latest/acceptance/place-order-positive-test.spec.ts`

TypeScript currently adds a redundant `unitPrice` field to each case:

```ts
const totalPriceCases = [
    { country: 'UK', taxRate: '0.09', unitPrice: '50.00', subtotalPrice: '50.00', expectedTaxAmount: '4.50', expectedTotalPrice: '54.50' },
    { country: 'US', taxRate: '0.20', unitPrice: '100.00', subtotalPrice: '100.00', expectedTaxAmount: '20.00', expectedTotalPrice: '120.00' },
];
```

Java and .NET use five positional fields only — no separate `unitPrice` — and pass `subtotalPrice` into `.withUnitPrice(subtotalPrice)` (Java `PlaceOrderPositiveTest.java` line 167; .NET `PlaceOrderPositiveTest.cs` line 151).

Collapse the TS rows to five fields matching Java, and use the `subtotalPrice` variable where `unitPrice` is currently used. The resulting call `.withUnitPrice(subtotalPrice)` mirrors Java/.NET exactly.

## 8. Test — Acceptance — switch negative-empty cases to a shared provider

Files:
- `system-test/typescript/tests/latest/acceptance/place-order-negative-test.spec.ts` (five uses of `['', '   ']` inline at `test.eachAlsoFirstRow`)
- `system-test/typescript/tests/latest/acceptance/publish-coupon-negative-test.spec.ts` (`emptyCodes = ['', '   ']`)

Java uses `com.optivem.shop.systemtest.commons.providers.EmptyArgumentsProvider`; .NET uses `SystemTests.Commons.Providers.EmptyArgumentsProvider`. TypeScript has no equivalent module.

Create `system-test/typescript/tests/commons/providers/empty-arguments-provider.ts` (or the TS-idiomatic equivalent under `tests/commons/`) that exports:

```ts
export const emptyArguments = ['', '   '] as const;
```

Replace every inline `['', '   ']` occurrence in `place-order-negative-test.spec.ts` and `publish-coupon-negative-test.spec.ts` with an import of `emptyArguments`. This matches Java/.NET's shared abstraction and keeps test data centralized.

## 9. Test — Acceptance — coerce `shouldRejectOrderWithZeroOrNegativeUsageLimit` data to strings

File: `system-test/typescript/tests/latest/acceptance/publish-coupon-negative-test.spec.ts` (lines 51–65).

Current TS data: `[0, -1, -100]` (numbers). Java/.NET use strings: `{"0", "-1", "-100"}`. Change the TS cases to strings (`['0', '-1', '-100']`) to match Java/.NET — this ensures the HTTP request body serialization matches exactly.

## 10. Test — Acceptance — remove redundant `forChannels(UI, API)` wrapping around `eachAlsoFirstRow`

Files:
- `system-test/typescript/tests/latest/acceptance/cancel-order-positive-isolated-test.spec.ts`
- `system-test/typescript/tests/latest/acceptance/cancel-order-negative-isolated-test.spec.ts`

Both currently wrap `test.eachAlsoFirstRow(...)` inside `forChannels(ChannelType.UI, ChannelType.API)(() => { ... })`. Per `system-test/typescript/tests/latest/acceptance/base/fixtures.ts`, `eachAlsoFirstRow` is already pre-bound with `[API], [UI]` channel semantics (first row runs on UI, all rows run on API). Wrapping in `forChannels(UI, API)` either double-runs or conflicts. Java uses `@Channel(value={API}, alsoForFirstRow=UI)` alone; .NET uses `[ChannelData(API, AlsoForFirstRow = new[] { UI })]` alone.

Remove the outer `forChannels(...)` wrapping in both files — leave the `test.describe('@isolated', ...)` + `test.describe.configure({ mode: 'serial' })` intact, and let `test.eachAlsoFirstRow` handle channel fan-out on its own.

## 11. Test — Acceptance — fix `shouldRejectOrderWith{Negative,Zero}Quantity` data type

File: `system-test/typescript/tests/latest/acceptance/place-order-negative-test.spec.ts` (lines 54–74).

Java uses `.withQuantity(-10)` and `.withQuantity(0)` (int). TS uses `.withQuantity('-10')` / `.withQuantity('0')` (string). Change TS to integer literals to match.

## 12. Legacy — Architecture — add shared `BaseRawTest` module to mod03

Java: `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod03/base/BaseRawTest.java`. .NET: `system-test/dotnet/SystemTests/Legacy/Mod03/Base/BaseRawTest.cs`. TS has no per-module equivalent — tests import loose helpers from the global `tests/legacy/mod02/base/BaseRawTest.ts` but do not share per-module setup.

**TS idiom:** follow the existing pattern in `system-test/typescript/tests/legacy/mod02/base/BaseRawTest.ts` — a module that **exports helper functions and interfaces**, not an abstract class with inheritance. Java's `extends BaseRawTest` is translated in TS as importing and invoking these helpers (or consuming them via a Playwright fixture).

Add `system-test/typescript/tests/legacy/mod03/base/BaseRawTest.ts` — export helper functions/interfaces exposing `shopApiHttpClient`, `erpHttpClient`, `getErpBaseUrl()`, `getShopApiBaseUrl()` (these already exist individually). Update the mod03 e2e spec files to import from this per-module module instead of reaching into the global one.

Also add `system-test/typescript/tests/legacy/mod03/e2e/base/BaseE2eTest.ts` as a module exporting a Playwright fixture (or a `runBaseE2eSetup(test)` helper), mirroring Java's `legacy/mod03/e2e/base/BaseE2eTest.java`. Do **not** implement as an abstract class.

## 13. Legacy — Architecture — add shared `BaseClientTest` and `BaseE2eTest` modules to mod04

Java: `legacy/mod04/base/BaseClientTest.java` + `legacy/mod04/e2e/base/BaseE2eTest.java`. .NET: same files under `Mod04/Base/` and `Mod04/E2eTests/Base/`. TS mod04 has no matching modules.

Add both as TypeScript modules that **export Playwright fixtures and/or helper functions** — **not** abstract classes. Follow the existing mod02 `BaseRawTest.ts` pattern.

- `BaseClientTest.ts`: export Playwright fixtures exposing the typed clients used in mod04 tests (`shopApiClient`, `erpClient`). Consumers import and extend their `test` via Playwright's `test.extend(...)`, not via `class Foo extends BaseClientTest`.
- `BaseE2eTest.ts`: export a fixture (or `runBaseE2eSetup(test)` helper) that configures per-test setup matching Java's `BaseE2eTest.java`.

Refactor the four `place-order-*.spec.ts` files in mod04 to consume these modules via fixture composition.

## 14. Legacy — Architecture — add shared `*BaseTest`, `BaseDriverTest`, `BaseE2eTest` modules to mod05

Java mod05 has two patterns missing in TS:

- `mod05/base/BaseDriverTest.java` and `mod05/e2e/base/BaseE2eTest.java` (module-level bases).
- `mod05/e2e/PlaceOrderPositiveBaseTest.java` and `PlaceOrderNegativeBaseTest.java` — abstract test classes that hold the test body, with empty `Api` and `Ui` subclasses that only choose the driver.

TS currently duplicates each test body between `place-order-positive-api-test.spec.ts` and `place-order-positive-ui-test.spec.ts` (and the negative pair). This duplication is the only cross-language mismatch at mod05.

**TS idiom:** extract the shared body into a helper function (e.g. `runPlaceOrderPositive(test)`) exported from a shared module. Both `*-api-test.spec.ts` and `*-ui-test.spec.ts` then import and invoke it with their respective fixture (`apiTest` vs `uiTest`). This reproduces Java's abstract-base pattern via functions, **not** via `abstract class` inheritance. The existing `system-test/typescript/tests/legacy/mod05/smoke/system/ShopBaseSmokeTest.ts` already exemplifies this pattern (exports `runShopBaseSmokeTest(test)`) — reuse it.

- Add `mod05/e2e/place-order-positive-base-test.ts` exporting `runPlaceOrderPositive(test)`.
- Add `mod05/e2e/place-order-negative-base-test.ts` exporting `runPlaceOrderNegative(test)`.
- Have `*-api-test.spec.ts` and `*-ui-test.spec.ts` import and invoke these.

Also add `mod05/base/BaseDriverTest.ts` and `mod05/e2e/base/BaseE2eTest.ts` as per-module modules exporting fixtures/helpers (mirroring mod04 work in step 13).

For the smoke tests: `ShopBaseSmokeTest.ts` already implements the correct pattern. Ensure `ShopApiSmokeTest.ts` and `ShopUiSmokeTest.ts` (matching `ShopApiSmokeTest.java` + `ShopUiSmokeTest.java`) exist and invoke `runShopBaseSmokeTest(test)`.

## 15. Legacy — Architecture — add shared `BaseChannelDriverTest` and `BaseE2eTest` modules to mod06

Java: `mod06/base/BaseChannelDriverTest.java` + `mod06/e2e/base/BaseE2eTest.java`. .NET: same. TS: absent.

Add `system-test/typescript/tests/legacy/mod06/base/BaseChannelDriverTest.ts` and `system-test/typescript/tests/legacy/mod06/e2e/base/BaseE2eTest.ts` as **modules exporting Playwright fixtures or helper functions** (same pattern as mod02's `BaseRawTest.ts`). Do **not** implement as abstract classes. Refactor `place-order-positive-test.spec.ts` and `place-order-negative-test.spec.ts` to consume them via fixture composition.

## 16. Legacy — Architecture — add shared `BaseUseCaseDslTest` and `BaseE2eTest` modules to mod07

Java: `mod07/base/BaseUseCaseDslTest.java` + `mod07/e2e/base/BaseE2eTest.java`. .NET: same. TS: absent.

Add matching TS modules — exports fixtures/helpers, not abstract classes. Refactor the two mod07 e2e specs to consume them.

## 17. Legacy — Architecture — add shared `BaseScenarioDslTest` and `BaseE2eTest` modules to mod08

Java: `mod08/base/BaseScenarioDslTest.java` + `mod08/e2e/base/BaseE2eTest.java`. .NET: same. TS: absent.

Add matching TS modules — exports fixtures/helpers, not abstract classes. Refactor the two mod08 e2e specs to consume them.

## 18. Legacy — Architecture — add shared `BaseScenarioDslTest` module to mod09

Java: `mod09/base/BaseScenarioDslTest.java`. .NET: same. TS: absent.

Add `system-test/typescript/tests/legacy/mod09/base/BaseScenarioDslTest.ts` as a module exporting a fixture/helper (not an abstract class). Refactor the mod09 smoke specs (`clock-smoke-test.spec.ts`, `erp-smoke-test.spec.ts`, `tax-smoke-test.spec.ts`, `shop-smoke-test.spec.ts`) to consume it.

## 19. Legacy — Architecture — add shared `BaseAcceptanceTest` and `BaseScenarioDslTest` modules to mod10

Java: `mod10/base/BaseScenarioDslTest.java` + `mod10/acceptance/base/BaseAcceptanceTest.java`. .NET: same. TS: absent.

Add `system-test/typescript/tests/legacy/mod10/base/BaseScenarioDslTest.ts` and `system-test/typescript/tests/legacy/mod10/acceptance/base/BaseAcceptanceTest.ts` as modules exporting fixtures/helpers (not abstract classes). Refactor the four mod10 acceptance specs (`place-order-positive-test.spec.ts`, `place-order-negative-test.spec.ts`, `place-order-positive-isolated-test.spec.ts`, `place-order-negative-isolated-test.spec.ts`) to consume them.

## 20. Legacy — Architecture — add shared `BaseScenarioDslTest` and `BaseE2eTest` modules to mod11

Java: `mod11/base/BaseScenarioDslTest.java` + `mod11/e2e/base/BaseE2eTest.java`. .NET: same. TS: absent.

Add matching TS modules — exports fixtures/helpers, not abstract classes. Refactor `mod11/e2e/place-order-positive-test.spec.ts` to consume them.

## 21. Legacy — Contract — restructure Contract tests to match Java/.NET abstract-class pattern (optional)

TypeScript currently uses `registerXxxContractTests(test)` helper functions instead of abstract base classes with real/stub subclasses (as Java and .NET do). Functionally equivalent. If the user wants strict parity, refactor to an abstract-class pattern — but this is stylistic and the current approach is a reasonable TS idiom. Flag as a **suggestion only** and do not execute unless explicitly approved.

## Local verification & commit

1. From `system-test/typescript/`, run the latest and legacy suites via the standard entry point:
   ```powershell
   Run-SystemTests -Architecture monolith
   Run-SystemTests -Architecture monolith -Legacy
   ```
   Do **not** substitute `npm test`, `npx playwright test`, `npx vitest`, or any raw toolchain invocation — `Run-SystemTests.ps1` is the only supported entry point because it manages Docker containers and config selection.

2. Investigate and fix any failures reported by either run before moving on.

3. Commit the TypeScript changes as a single logical commit (or a small set of commits split by theme: channel type, folder structure, acceptance fixes, legacy bases) with messages describing the alignment (e.g. `Introduce per-module BaseTest abstractions for legacy mod03–mod11 to match Java/.NET`).
