# Plan: Extend Domain — Taxation/Countries + Coupons

## Context

The starter repo already has the full architecture, test infrastructure, and CI/CD pipelines in place. This plan extends the **domain only**: adding coupon and tax/country features to the existing system.

**Scope boundaries:**
- ✅ IN scope: `starter` repo only — app code (entities, services, controllers, DTOs), test DSL layers (driver port/adapter, use case DSL, scenario DSL), test classes
- ❌ OUT of scope: any other repo (shop, shop-tests, courses, etc.), architecture changes, CI/CD pipelines, docker-compose files, GitHub Actions workflows, Run-SystemTests.ps1 scripts

**Why together:** Coupons and tax are coupled in the order pricing pipeline. Splitting them would require implementing half a pipeline — both features ship in one pass.

**Rule: working, tested code ships before any lesson content is written.**

---

## New Pricing Formula

```
basePrice        = unitPrice × quantity
promotionPrice   = basePrice × promotion.discountFactor          [already exists; discountFactor = promotion.discount if active, else 1.0]
discountedPrice  = promotionPrice × (1 − coupon.discountRate)    [or promotionPrice if no coupon]
totalPrice       = discountedPrice × (1 + tax.taxRate)
```

The promotion step already exists in the starter. The verbatim copy from shop adds coupon + tax on top — promotion is the one insertion point where the pipelines are joined.

New Order fields to add: `country`, `taxRate`, `discountRate`, `appliedCouponCode` (nullable), `subtotalPrice` (= discountedPrice before tax)

---

## Execution Rules

- **Verbatim copy first:** For app code, copy the relevant new files from shop verbatim (Coupon entity/service/repository/controller, TaxGateway, all related DTOs), then adapt (rename packages, adjust framework-specific details). Never rewrite from description — that introduces subtle bugs. We are *adding* this functionality, not removing it.
- **Java multitier is the reference:** After verbatim copy + adaptation, the Java multitier backend is the reference. .NET and TypeScript must match its behavior exactly (same JSON structure, status codes, field names, validation messages).
- **Test code:** Copy verbatim from shop-tests, then sync to the Java starter test reference (match test method names/content, add missing DSL methods, remove extra files not present in Java starter).
- Extend existing files where possible; create new files only for new entities/services/controllers/DSL classes.
- Architecture of each layer stays identical — add new classes following existing patterns, do not restructure.
- **docker-compose files already exist** — do not create or modify them. If a modification seems necessary, ask for approval first.
- **Never call docker directly** — all container lifecycle is managed by `Run-SystemTests.ps1`.
- No pipeline touches — not even environment variable additions to workflow files.

## Verification Sequence (per phase)

After each phase, run in this order:

1. Compilation — ensure the project compiles cleanly
2. `./Run-SystemTests.ps1 -SkipTests` — starts the system; `-Rebuild` is baked in here to pick up source changes
3. `./Run-SystemTests.ps1 -Suite acceptance-api`
4. `./Run-SystemTests.ps1 -Suite acceptance-isolated-api`
5. If both pass → `./Run-SystemTests.ps1 -Suite acceptance-ui`
6. `./Run-SystemTests.ps1 -Suite acceptance-isolated-ui`

No need to pass `-Rebuild` on subsequent test suite runs — the system is already built from step 2.

All steps must be green before moving to the next phase. Only after all phases complete → ask user for commit approval via `/commit`.

---

## Ordering

All multitier first (Java → .NET → TypeScript), then all monoliths (Java → .NET → TypeScript). Same order as MIGRATION.md.

---

## Phase 1: External Stub + Real-Sim ✅

Done once, shared by all backends.

**Create:**
- `system/external-stub/mappings/tax-countries-us.json` — `GET /tax/api/countries/US` → `{id:"US", countryName:"United States", taxRate:0.08}`
- `system/external-stub/mappings/tax-countries-de.json` — taxRate 0.19
- `system/external-stub/mappings/tax-countries-gb.json` — taxRate 0.20

**Modify:**
- `system/external-real-sim/mock-server.js` — add `/tax/health` and `/tax/api/countries/:country` routes with US/DE/GB seeded

No coupon stub needed — coupons are internal to the shop app.

---

## Phase 2: Multitier Java Backend ✅

**New files** (follow existing package/naming conventions exactly):
- `core/entities/Coupon.java` — `@Entity`, fields: id, code (unique), discountRate, validFrom/validTo (Instant nullable), usageLimit (Integer nullable = unlimited), usedCount
- `core/repositories/CouponRepository.java` — `findByCode(String)`
- `core/services/external/TaxGateway.java` — `getTaxDetails(String country)`, follows `ErpGateway.java` pattern
- `core/dtos/external/TaxDetailsResponse.java` — id, countryName, taxRate
- `core/dtos/PublishCouponRequest.java`, `PublishCouponResponse.java`, `BrowseCouponsResponse.java`
- `core/services/CouponService.java` — publishCoupon, getDiscount (validates exists/not expired/under limit), incrementUsageCount, getAllCoupons
- `api/controller/CouponController.java` — `POST /api/coupons`, `GET /api/coupons`; follows `OrderController.java` pattern

**Modify:**
- `core/entities/Order.java` — add: country, taxRate, discountRate, appliedCouponCode (nullable), subtotalPrice
- `core/services/OrderService.java` — inject TaxGateway + CouponService; update pricing pipeline
- `core/dtos/PlaceOrderRequest.java` — add: `country` (@NotBlank), `couponCode` (nullable, no validation)
- `core/dtos/ViewOrderDetailsResponse.java` — add new fields
- `core/dtos/BrowseOrderHistoryResponse.java` — add country to item
- `src/main/resources/application.yml` — add `tax.url: ${TAX_API_URL:http://localhost:9001/tax}`

**Verify:** `./gradlew test` passes (backend unit tests); `acceptance-api` + `acceptance-isolated-api` green for Java multitier latest + legacy

---

## Phase 3: Multitier .NET Backend ✅

Same domain changes as Java, translated to C#. Follow existing patterns in `backend-dotnet/`.

**New files:** Coupon entity, CouponRepository (EF Core), TaxGateway, all DTOs, CouponController
**Modify:** Order entity, OrderService, PlaceOrderRequest, response DTOs, `appsettings.json` (TAX_API_URL), `AppDbContext.cs` (add `DbSet<Coupon>`)

**Verify:** `acceptance-api` + `acceptance-isolated-api` green for .NET multitier latest + legacy

---

## Phase 4: Multitier TypeScript Backend (NestJS) ✅

Same domain changes, translated to TypeScript/NestJS. Follow existing patterns in `backend-typescript/`.

**New files:** `coupon.entity.ts` (TypeORM), `tax.gateway.ts`, `coupon.service.ts`, DTOs, `coupon.controller.ts`
**Modify:** `order.entity.ts`, `order.service.ts`, `place-order-request.dto.ts`, response DTOs, config for TAX_API_URL, `app.module.ts` (register Coupon entity + CouponService + TaxGateway)

**Verify:** `acceptance-api` + `acceptance-isolated-api` green for TypeScript multitier latest + legacy

---

## Phase 5: Monolith Java ✅

Same domain changes as multitier Java, applied to `system/monolith/java/`. SSR views (Thymeleaf) also need country + coupon fields on the order form and order detail view.

**Verify:** `acceptance-api` + `acceptance-isolated-api` green for Java monolith latest + legacy

---

## Phase 6: Monolith .NET ⬜

Same as Phase 3, applied to `system/monolith/dotnet/`. Razor pages updated for new fields.

**Verify:** `acceptance-api` + `acceptance-isolated-api` green for .NET monolith latest + legacy

---

## Phase 7: Monolith TypeScript (Next.js) ⬜

Same domain changes applied to `system/monolith/typescript/`. Tax gateway in `external.ts`; coupon logic in `db.ts` or new file; new `/api/coupons` route; Next.js pages updated.

**Verify:** `acceptance-api` + `acceptance-isolated-api` green for TypeScript monolith latest + legacy

---

## Phase 8: React Frontend (Multitier) ⬜

**Modify:**
- `api.types.ts` — add `couponCode?: string`, `country: string` to `PlaceOrderRequest`; add `subtotalPrice`, `taxRate`, `discountRate`, `appliedCouponCode` to response types
- `form.types.ts` — add `country: string`, `couponCode: string` to `OrderFormData`
- `OrderForm.tsx` — add Country input (required, `aria-label="Country"`) and Coupon Code input (optional, `aria-label="Coupon Code"`)
- `OrderDetailView.tsx` — add fields: Country, Subtotal Price, Discount Rate, Tax Rate, Applied Coupon Code

---

## Phase 9: Test DSL — Driver Port (all 3 languages) ⬜

Extend existing driver port interfaces — do not restructure.

**Modify:**
- `ShopDriver` interface — add `publishCoupon()`, `browseCoupons()`
- `PlaceOrderRequest` DTO — add `couponCode`, `country`
- Response DTOs for PlaceOrder/ViewOrder — add new fields

**New files** (follow ErpDriver pattern):
- `TaxDriver` interface — `goToTax()`, `getCountry(String country)`, `returnsCountry(ReturnsCountryRequest)`
- DTOs: `ReturnsCountryRequest`, `GetCountryResponse`, `PublishCouponRequest`, coupon response DTOs

---

## Phase 10: Test DSL — Driver Adapter (all 3 languages) ⬜

**Modify:**
- `ShopApiDriver` / `ShopUiDriver` — implement `publishCoupon`, `browseCoupons`; update `placeOrder` to include couponCode + country in request body

**New files** (follow ErpDriver/ErpRealDriver/ErpStubDriver pattern exactly):
- `TaxRealDriver`, `TaxStubDriver`
- `TaxRealClient`, `TaxStubClient` + client DTOs

---

## Phase 11: Test DSL — Use Case DSL (all 3 languages) ⬜

**New files** (follow ErpDsl pattern):
- `TaxDsl` — `returnsCountry()`, `getCountry()`, `goToTax()` use cases
- `PublishCoupon`, `BrowseCoupons`, `BrowseCouponsVerification` use cases under shop namespace

**Modify:**
- `UseCaseDsl` — add `tax()` method returning `TaxDsl`; inject `TaxDriver` supplier
- `ShopDsl` — add `publishCoupon()`, `browseCoupons()`
- `PlaceOrder` use case — add `couponCode()`, `country()` setters
- `PlaceOrderVerification`, `ViewOrderVerification` — add assertions for subtotalPrice, discountRate, taxRate, appliedCouponCode

---

## Phase 12: Test DSL — Scenario DSL (all 3 languages) ⬜

**New files:**
- `GivenCountryImpl` — `country(name).withTaxRate(rate)` → calls `app.tax().returnsCountry()...`
- `GivenCouponImpl` — `coupon().withCode(x).withDiscountRate(r)` → calls `app.shop().publishCoupon()...`

**Modify:**
- `GivenImpl` — add `country()` and `coupon()` factory methods; wire TaxDriver into setup
- `WhenPlaceOrderImpl` — add `withCouponCode()`, `withCountry()`
- `ThenOrderImpl` — add `hasSubtotalPrice`, `hasTaxRate`, `hasDiscountRate`, `hasAppliedCouponCode`
- All port interfaces — match new methods
- `ScenarioDefaults` — add `DEFAULT_COUNTRY = "US"` (existing PlaceOrder tests pass silently)
- `BaseScenarioDslTest` — initialize `TaxDriver` alongside Erp + Clock

---

## Phase 13: New Test Classes (all 3 languages) ⬜

**New acceptance tests:**
- `PublishCouponPositiveTest` — valid code + discountRate → succeeds
- `PublishCouponNegativeTest` — duplicate code, discountRate out of range → validation errors
- `BrowseCouponsPositiveTest` — published coupon appears in list

**New external system tests:**
- `BaseTaxContractTest`, `TaxRealContractTest`, `TaxStubContractTest` — `getCountry("US")` returns correct taxRate on both real and stub
- `TaxSmokeTest` — health check for tax external system

**Update existing tests:**
- `PlaceOrderPositiveTest` — add scenarios: tax applied to total; coupon + tax combined (e.g. price 100.00, 10% off, 20% tax → total 108.00)

---

## Phase 14: Course Module 18 (after all code is verified) ⬜

**Location:** `courses/02-ATDD/accelerator/course/18-coupons-and-tax/`

- `_index.md`
- `00-overview.md` — learning outcomes (ATDD over internal entity + external system simultaneously; exact numeric assertions; GivenCountry stub control)
- `01-business-request.md`
- `02-acceptance-criteria.md` — Gherkin with exact values for all scenarios (publish positive/negative, browse, place order with tax only, coupon only, both combined, expired/over-limit coupon)
- `03-sandbox-project.md` — milestones: (1) Three Amigos, (2) RED—coupon, (3) RED—tax contract, (4) GREEN—backend

---

## Final Verification Gate (before asking for commit approval)

After all phases complete, all of the following must be green across every combination:

| Suite | Languages | Architectures | Versions |
|---|---|---|---|
| `acceptance-api` | Java, .NET, TypeScript | multitier + monolith | latest + legacy |
| `acceptance-isolated-api` | Java, .NET, TypeScript | multitier + monolith | latest + legacy |
| `acceptance-ui` | Java, .NET, TypeScript | multitier + monolith | latest + legacy |
| `acceptance-isolated-ui` | Java, .NET, TypeScript | multitier + monolith | latest + legacy |

→ Only then: ask user for commit approval via `/commit`. Do NOT commit between phases.
