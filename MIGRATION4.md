# Consolidated Migration Plan: Make Starter the Superset

Single sequenced plan combining ESHOP_COMPARISON (app code) and ESHOP_TESTS_COMPARISON (test code).

---

## Phase 1: Fix What's Wrong in Starter

### 1.1 Fix promotion pricing logic (all 3 languages)

Promotion discount is applied at the wrong level. Fix in system code + test assertions.

**Current (wrong):** `basePrice = unitPrice * quantity * promotionFactor`
**Correct:** `basePrice = unitPrice * quantity`, then `subtotalPrice = basePrice - promotionAmount - couponAmount`

Affected: backend system code (all 3 langs), test DSL defaults, test assertions.

---

## Phase 2: Restore Delivery Feature (App Level)

### 2.1 Backend — Add back delivery feature

- Restore `DELIVERED` to `OrderStatus` enum
- Restore `deliverOrder()` in `OrderService` (PLACED -> DELIVERED transition, use starter's code quality: no debug prints, `IllegalStateException`)
- Restore `POST /api/orders/{orderNumber}/deliver` endpoint in `OrderController`

### 2.2 Frontend — Add back delivery UI

- Restore `OrderActions.tsx` component (with starter's code quality: `Readonly<Props>`, no emojis)
- Restore `deliverOrder` in `order-service.ts`
- Restore `deliverOrder` and `isDelivering` in `useOrderDetails` hook
- Add `DELIVERED` to `OrderStatus` in `api.types.ts`
- Wire deliver button back into `OrderDetails.tsx` via `OrderActions`

---

## Phase 3: Restore Other App-Level Gaps

### 3.1 Backend — Add back OpenAPI/Swagger

- Restore `springdoc-openapi-starter-webmvc-ui` dependency in `build.gradle`
- Restore `OpenApiConfig.java` (with starter's package name `com.optivem.shop.backend`)

### 3.2 Frontend — Restore missing types and columns

- Add `GetCouponResponse` interface to `api.types.ts`
- Make `PlaceOrderRequest.country` required (not optional)
- Restore `country` and `appliedCouponCode` columns in `OrderHistoryTable`

### 3.3 Frontend — Restore country validation and coupon defaults

- Add back country validation in `useOrderForm.ts` ("Country must not be empty")
- Restore default `country: 'US'`
- Change `couponCode: string` back to `couponCode?: string` in `form.types.ts`
- Restore default `couponCode: undefined`

---

## Phase 4: Migrate Delivery Test Infrastructure

Depends on Phase 2 (app-level delivery must exist first).

### 4.1 Delivery test infrastructure (all 3 languages)

- `ShopDriver.deliverOrder()` — driver port interface method
- `ShopApiDriver.deliverOrder()` — API driver adapter (POSTs to `/{orderNumber}/deliver`)
- `ShopUiDriver.deliverOrder()` — UI driver adapter (Playwright)
- `OrderDetailsPage.clickDeliverOrder()` — UI page object (`[aria-label='Deliver Order']` selector)
- `OrderController.deliverOrder()` — API client method
- `DeliverOrder` use case class — DSL core
- `ShopDsl.deliverOrder()` — DSL entry point
- `GivenOrderImpl` — scenario setup uses delivery to create orders in DELIVERED state

---

## Phase 5: Migrate Missing Test Features

### 5.1 Add ThenFailureCoupon (all 3 languages)

Add the missing `ThenFailureCoupon` class to starter's DSL.
- .NET: `Dsl.Core/Scenario/Then/Steps/ThenFailureCoupon.cs`
- Java/TypeScript: equivalents

### 5.2 Add missing legacy module tests (all 3 languages)

Bring ~44 (.NET), ~39 (Java), ~41 (TypeScript) legacy test files from eshop-tests:
- ViewOrder E2e tests (Mod03-Mod08)
- TaxSmokeTest (Mod02-Mod09)
- Mod03 Smoke tests (ErpSmokeTest, ShopApiSmokeTest, ShopUiSmokeTest)
- Mod10: CancelOrder tests (4), ViewOrder tests (2), Coupon tests (3)
- Mod11: Tax contract tests (3)
- Mod06: SystemErrorAssertExtensions

---

## Phase 6: TypeScript Test Architecture Refactor

### 6.1 Refactor monolithic DSL to hexagonal architecture

Refactor `scenario-dsl.ts` (1,662 lines, 50+ inner classes) into multi-file hexagonal architecture matching .NET/Java structure.

Key decisions:
- Keep Jest (not Playwright) — simpler for students
- Keep `createScenario()` factory pattern
- Extract classes into individual files with proper imports
- Add PromiseLike deferred execution pattern from eshop-tests
- Add base classes (BaseGivenStep, BaseWhenStep)
- Preserve all starter-only features (promotion, ChannelMode, etc.)

See ESHOP_TESTS_COMPARISON.md section 9e for full target directory structure.

---

## Phase 7 (Optional): Backport Starter Improvements to eshop-tests

After starter is the confirmed superset, backport these to eshop-tests:
1. Promotion concept
2. ChannelMode
3. IAsyncDisposable (.NET)
4. ThenBrowseCoupons
5. Additional test scenarios
6. WeekdayTime/WeekendTime constants
7. Dec 31st / Clock isolated tests

---

## What Stays as-is in Starter (Already Better)

These are starter improvements over eshop — no action needed:

**Backend:** Spring Boot 3.5.6, H2 test DB, promotion system, direct health endpoint, `IllegalStateException` in gateways, `InterruptedException` handling, extracted constants in `GlobalExceptionHandler`, private constructor on `TypeValidationMessageExtractor`, `.toList()`, structured datasource config, Sonar rules, no debug prints.

**Frontend:** `Readonly<Props>`, no emojis, `useMemo` context, `globalThis`, `Number.parseFloat`/`Number.isNaN`, optional chaining, `private readonly baseUrl`, better React keys, LF line endings, no commented-out code.

**Tests:** Promotion DSL, ChannelMode, IAsyncDisposable (.NET), ThenBrowseCoupons, Dec 31st test, Clock contract test, Trait annotations, WeekdayTime/WeekendTime, additional test scenarios.

**Decisions (resolved):** Cancel logic keeps starter's way (23:59 start). Route keeps `/new-order`. Branding keeps "Shop". CouponService constants are dead code — stay removed.
