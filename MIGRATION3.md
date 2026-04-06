# Plan: Sync starter ↔ eshop-tests (Java → .NET → TypeScript)

## Goal

Make `starter` match `eshop-tests` as the source of truth at every layer, for all three languages — while preserving starter-only additions (promotion support, isolated tests, static-channel variants).

**Source of truth per language:**
- Java: `eshop-tests/java/`
- .NET: `eshop-tests/dotnet/`
- TypeScript: `eshop-tests/typescript/`

**Priority order:** Java → .NET → TypeScript

---

## Rules

- **Bottom-up always:** system → driver port → driver adapter → DSL core → DSL port → tests. Never add a test for functionality that doesn't exist at lower layers.
- **Verbatim copy first:** copy from eshop-tests, then adapt package names (`com.optivem.eshop` → `com.optivem.shop`, namespace equivalents for .NET/TypeScript).
- **Starter-only additions are preserved** — see list below. They are not removed or overwritten.
- **Conflicts resolved in favour of eshop-tests** by default. When the starter version appears richer or different in a non-trivial way, **stop and ask** before proceeding — do not decide unilaterally.
- **Test equivalence check — applies to all languages:** when comparing any test method (new or shared), verify ALL of the following, not just the body:
  - Channel scope: does eshop-tests run on `{UI, API}`, `API` only, or `UI` only? If it differs from starter, ask.
  - Isolation: isolated (`@Isolated` / equivalent) and non-isolated tests are **separate classes** — never merge them into one file, and never move a test from isolated to non-isolated or vice versa without asking.
  - Time-dependency: `@TimeDependent` / equivalent present in one but not the other?
  - Parameterization: `@DataSource`, `@ValueSource`, `@MethodSource` / equivalents — same values and types?
  - Method name: exact match?
  - Scenario body: same steps and assertions?
- **System before system-test:** for any language, all changes to `starter/system/` must be done and verified before touching `starter/system-test/`. This is the bottom-up rule applied at the repo level.
- **Never touch `Run-SystemTests.ps1` or any GitHub Actions workflow files** — these are out of scope for this migration entirely.
- Do not restructure any layer — add to existing files/packages following existing patterns.
- **Ask for approval at the end of every phase** before starting the next one — even if everything looks green.
- Never commit between phases. Only commit after all phases for a language are green via `/commit`.

### Phase Verification Sequence (applies to every phase end)

Run suites against multitier first. Only if multitier is fully green, run the same suites against monolith:

```
./Run-SystemTests.ps1 -Architecture multitier -Suite <suite>
# if green →
./Run-SystemTests.ps1 -Architecture monolith -Suite <suite>
```

Which suites to run depends on the phase — listed in each phase's Verify block. If any suite fails, stop and fix before proceeding. Only when both architectures are green: ask user for approval.

---

## Starter-Only Additions (Preserve — Do Not Remove)

These exist in starter but not in eshop-tests. They stay.

**Java DSL:**
- `GetPromotionResponse`, `ReturnsPromotionRequest` DTOs in driver port
- `GivenPromotionImpl`, `WhenPromotion*`, `ThenPromotion*` scenario steps
- `ReturnsPromotion` use case
- `PlaceOrderPositiveIsolatedTest` (tests timestamp + promotion discount logic)
- `ClockStubContractIsolatedTest`

**.NET DSL:**
- Same promotion-related additions as Java

**TypeScript:**
- `test/legacy/` directory (curriculum progression tests)
- Simplified `src/` flat structure (do not restructure to match eshop-tests layers until TypeScript phase is explicitly started)

**All languages:**
- `starter/system/` (the application under test — not in eshop-tests)
- Docker and pipeline config files

---

## Tracking

Mark each item ✅ when done. Mark the phase header ✅ when all items in it are done.

---

---

# JAVA

---

## Phase J1: System — Add CancelOrder ⬜

The system (both monolith and multitier) is missing the cancel order endpoint. The DSL and tests can't be added until this exists.

**Affects:** `starter/system/monolith/java/` and `starter/system/multitier/backend-java/`

### OrderStatus enum (both system variants)
- ⬜ Add `CANCELLED` to `OrderStatus` entity enum (`core/entities/OrderStatus.java`)

### OrderService (both system variants)
- ⬜ Add `cancelOrder(String orderNumber)` method:
  - Looks up order by number; throws `NotExistValidationException` if not found
  - Throws `ValidationException("Order has already been cancelled")` if already `CANCELLED`
  - Sets status to `CANCELLED` and saves

### OrderApiController / OrderController (both system variants)
- ⬜ Add `DELETE /api/orders/{orderNumber}` endpoint calling `orderService.cancelOrder()`; returns `204 No Content`

**Verify:**
```
./Run-SystemTests.ps1 -Architecture multitier -SkipTests
# if green →
./Run-SystemTests.ps1 -Architecture monolith -SkipTests
```
Then ask for approval.

---

## Phase J2: DSL Driver Port ⬜

**File:** `system-test/java/src/main/java/com/optivem/shop/dsl/driver/port/shop/`

- ⬜ `OrderStatus.java` — add `CANCELLED` (currently only has `PLACED`; add `DELIVERED` too to match eshop-tests)
- ⬜ `ShopDriver.java` — add `Result<Void, ErrorResponse> cancelOrder(String orderNumber)`

**Verify:** DSL compiles cleanly. Then ask for approval.

---

## Phase J3: DSL Driver Adapter ⬜

**Files:** `system-test/java/src/main/java/com/optivem/shop/dsl/driver/adapter/shop/`

### API adapter
- ⬜ `api/client/controllers/OrderController.java` — add `cancelOrder(String orderNumber)` calling `DELETE /api/orders/{orderNumber}`
- ⬜ `api/ShopApiDriver.java` — implement `cancelOrder(String orderNumber)` delegating to the client controller

### UI adapter
- ⬜ `ui/ShopUiDriver.java` — implement `cancelOrder(String orderNumber)` (Playwright: navigate to order, click Cancel button)
- ⬜ Add any required Playwright page method (follow pattern of existing page classes)

**Verify:** DSL compiles cleanly. Then ask for approval.

---

## Phase J4: DSL Core ⬜

**New files:**

- ⬜ `usecase/shop/usecases/CancelOrder.java` — verbatim from `eshop-tests/java/dsl-core/.../CancelOrder.java`, adapted to `com.optivem.shop` packages
- ⬜ `scenario/when/steps/WhenCancelOrderImpl.java` — verbatim from eshop-tests, adapted

**Modify existing files:**

- ⬜ `usecase/shop/ShopDsl.java` — add `cancelOrder()` method returning `new CancelOrder(driver, context)`
- ⬜ `scenario/when/WhenImpl.java` — add `cancelOrder()` method returning `new WhenCancelOrderImpl(app)`
- ⬜ `scenario/assume/AssumeImpl.java` — add `tax()` method (follows erp/clock pattern: `app.tax().goToTax().execute().shouldSucceed()`)
- ⬜ `scenario/given/steps/GivenCountryImpl.java` — add `withCode(String country)` as alias for `withCountry()` (keep `withCountry()` to avoid breaking existing tests)

**Verify:** DSL compiles cleanly. Then ask for approval.

---

## Phase J5: DSL Port ⬜

**New file:**

- ⬜ `port/when/steps/WhenCancelOrder.java` — verbatim from eshop-tests, adapted

**Modify existing files:**

- ⬜ `port/when/WhenStage.java` — add `WhenCancelOrder cancelOrder()`
- ⬜ `port/assume/AssumeStage.java` — add `AssumeRunning tax()`
- ⬜ `port/given/steps/GivenCountry.java` — add `GivenCountry withCode(String country)`

**Verify:** DSL compiles cleanly (no `-Architecture` flag needed — compilation only). Then ask for approval.

---

## Phase J6: Tests — New Files ⬜

All files verbatim from `eshop-tests/java/.../latest/`, adapted to `com.optivem.shop` packages.

**Acceptance:**
- ⬜ `acceptance/CancelOrderPositiveTest.java`
- ⬜ `acceptance/CancelOrderPositiveIsolatedTest.java`
- ⬜ `acceptance/CancelOrderNegativeTest.java`
- ⬜ `acceptance/CancelOrderNegativeIsolatedTest.java`
- ⬜ `acceptance/ViewOrderPositiveTest.java`
- ⬜ `acceptance/ViewOrderNegativeTest.java`

**Contract:**
- ⬜ `contract/tax/BaseTaxContractTest.java`
- ⬜ `contract/tax/TaxRealContractTest.java`
- ⬜ `contract/tax/TaxStubContractTest.java`

**Smoke:**
- ⬜ `smoke/external/TaxSmokeTest.java`

---

## Phase J7: Tests — Content Sync for Shared Files ⬜

Files that exist in both repos but have different content. Resolution noted per file.

### `acceptance/PlaceOrderPositiveTest.java`
eshop-tests has 12 methods; starter has 5. Add the 7 missing from eshop-tests:
- ⬜ `shouldBeAbleToPlaceOrderForValidInput`
- ⬜ `shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity`
- ⬜ `shouldPlaceOrderWithCorrectBasePriceParameterized` (parameterized with `@DataSource`)
- ⬜ `orderPrefixShouldBeORD` (rename starter's `orderNumberShouldStartWithORD` to match eshop-tests name)
- ⬜ `discountRateShouldBeAppliedForCoupon`
- ⬜ `discountRateShouldBeNotAppliedWhenThereIsNoCoupon`
- ⬜ `subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon`
- ⬜ `subtotalPriceShouldBeSameAsBasePriceWhenNoCoupon`
- ⬜ `correctTaxRateShouldBeUsedBasedOnCountry` (parameterized)
- ⬜ `totalPriceShouldBeSubtotalPricePlusTaxAmount` (parameterized)
- ⬜ `couponUsageCountHasBeenIncrementedAfterItsBeenUsed`

### `acceptance/PlaceOrderNegativeTest.java`
eshop-tests has 12 methods; starter has 6. Add missing from eshop-tests:
- ⬜ `shouldRejectOrderWithInvalidQuantity`
- ⬜ `shouldRejectOrderWithNegativeQuantity`
- ⬜ `shouldRejectOrderWithZeroQuantity`
- ⬜ `shouldRejectOrderWithEmptyCountry`
- ⬜ `shouldRejectOrderWithInvalidCountry`
- ⬜ `shouldRejectOrderWithNullCountry`
- ⬜ `cannotPlaceOrderWithNonExistentCoupon`
- ⬜ `cannotPlaceOrderWithCouponThatHasExceededUsageLimit`

### `acceptance/PlaceOrderNegativeIsolatedTest.java`
Two different scenarios — keep both (test different rules):
- ⬜ Add eshop-tests method `cannotPlaceOrderWithExpiredCoupon` (with `@TimeDependent`)
- Keep starter's `shouldRejectOrderPlacedAtYearEnd` (starter-only addition)

### `acceptance/PublishCouponPositiveTest.java`
eshop-tests has 3 methods; starter has 1. Add missing:
- ⬜ `shouldBeAbleToPublishCouponWithEmptyOptionalFields`
- ⬜ `shouldBeAbleToCorrectlySaveCoupon`
- ⬜ Rename starter's `shouldPublishCouponSuccessfully` → `shouldBeAbleToPublishValidCoupon` to match eshop-tests

### `acceptance/PublishCouponNegativeTest.java`
eshop-tests has 4 methods; starter has 3. Add missing:
- ⬜ `cannotPublishCouponWithDuplicateCouponCode`
- ⬜ `cannotPublishCouponWithZeroOrNegativeUsageLimit`

### `acceptance/BrowseCouponsPositiveTest.java`
**⚠️ Conflict — ask user before executing.**
- eshop-tests: `shouldBeAbleToBrowseCoupons`, `@Channel({UI, API})`, just calls `browseCoupons()` and checks success
- starter: `publishedCouponShouldAppearInList`, `@Channel(API)` only, sets up a coupon and verifies it appears in the list
- Starter version is richer/more specific. Which should be kept (or both)?

### `contract/clock/BaseClockContractTest.java`
- ⬜ `withTime()` (no-arg, starter) → `withTime("2024-01-02T09:00:00Z")` (explicit, eshop-tests)

**Verify after all J6+J7 changes (multitier first, then monolith):**
```
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-api
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-isolated-api
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-ui
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-isolated-ui
./Run-SystemTests.ps1 -Architecture multitier -Suite contract
./Run-SystemTests.ps1 -Architecture multitier -Suite smoke
# if all green →
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-api
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-isolated-api
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-ui
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-isolated-ui
./Run-SystemTests.ps1 -Architecture monolith -Suite contract
./Run-SystemTests.ps1 -Architecture monolith -Suite smoke
```
Then ask for approval.

---

---

# .NET

*(To be expanded in detail once Java phases are complete. High-level scope below.)*

The gap pattern mirrors Java exactly. Same phases J1–J7 apply, translated to C#:

- **J1 equivalent:** Add `cancelOrder` to all .NET system variants (monolith + multitier)
- **J2 equivalent:** `IShopDriver` — add `CancelOrderAsync`; `OrderStatus` enum — add `Cancelled`
- **J3 equivalent:** API + UI driver adapters — implement cancel order
- **J4 equivalent:** New `CancelOrder` use case, `WhenCancelOrder` step, wire `tax()` in `AssumeImpl`, add `WithCode()` to `GivenCountry`
- **J5 equivalent:** Port interfaces — `ICancelOrder`, `IWhenStage.CancelOrder()`, `IAssumeStage.Tax()`, `IGivenCountry.WithCode()`
- **J6 equivalent:** Same 10 test files, translated to C#
- **J7 equivalent:** Same content sync for shared test files, translated to C#

Additional .NET-specific note: the tax driver adapter layer (8 files) is entirely missing from starter — needs full verbatim copy from eshop-tests/dotnet.

---

---

# TypeScript

*(To be expanded in detail once .NET phases are complete. High-level scope below.)*

TypeScript has the most divergence. The starter uses a flat `src/` structure; eshop-tests uses a strict layered architecture. **Do not restructure** starter's `src/` — instead add missing functionality following the existing flat pattern.

- Add `cancelOrder` to the TypeScript system variants
- Add `cancelOrder` to `ShopDriver` interface and all adapter implementations
- Add cancel order DSL support in the existing `scenario-dsl.ts` style
- Add 9 missing test files in `test/latest/`
- Sync content of shared test files

Full layer-by-layer detail to be written before TypeScript work starts.

---

## Final Verification Gate (Java)

After all Java phases (J1–J7) complete, run the full matrix — multitier first, then monolith:

```
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-api
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-isolated-api
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-ui
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-isolated-ui
./Run-SystemTests.ps1 -Architecture multitier -Suite contract
./Run-SystemTests.ps1 -Architecture multitier -Suite smoke
# if all green →
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-api
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-isolated-api
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-ui
./Run-SystemTests.ps1 -Architecture monolith -Suite acceptance-isolated-ui
./Run-SystemTests.ps1 -Architecture monolith -Suite contract
./Run-SystemTests.ps1 -Architecture monolith -Suite smoke
```

All must be green. Then ask for approval → `/commit`.
