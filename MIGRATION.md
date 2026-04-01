# Plan: Port eshop to starter (Application + Tests + Infrastructure)

## Context

Port the eshop ecosystem into the starter repo, covering application code, test infrastructure, and CI/CD — organized by execution order.

**Phasing:** All multitier first (Java, then .NET, then TypeScript — verbatim copy from eshop). Then all monoliths (Java, .NET, TypeScript — requires rewrite since eshop has no monolith).

**Execution rules:**
- **Verify as you go:** After completing each step, run whatever verification makes sense (compile, docker build, docker-compose up, tests, etc.) before proceeding to the next step. Don't batch file creation and defer verification to the end.
- **Ask user approval to `/commit`** after each verified step.
- **Run backend unit tests** (`./gradlew test`) after modifying backend code, not just system tests.
- **Show failures to user** before fixing — if something fails, it may indicate a gap in the plan.
- **Ask user approval for post-verbatim fixes:** After verbatim copy + tax/coupon removal, any additional code changes (behavior fixes, framework-specific adaptations, error format adjustments) must be shown to the user and approved before applying. The Java backend behavior is the reference, but it may also be evolving.
- **Identical backend behavior across languages:** All backend implementations (Java, .NET, TypeScript) must return identical API responses for the same inputs — same JSON structure, status codes, field names, and validation messages. The **Java backend is the reference implementation** — .NET and TypeScript must match its behavior exactly. Note: the Java backend behavior may differ from eshop (the user is actively changing things). The acceptance tests are language-agnostic and must be able to run against any backend. Test edge cases (empty strings, whitespace, null, type mismatches) where framework defaults diverge.
- **Language-scoped docker-compose names:** Every docker-compose file must include the language in its project name (e.g. `starter-java-multitier-real`, `starter-dotnet-multitier-stub`). This prevents conflicts when Java and .NET (and later TypeScript) systems run concurrently. The Run-SystemTests.ps1 container names must match.
- **Copy first, then delete:** When porting files, always copy the source file verbatim first, then surgically remove the specific lines for tax/coupon/cancel/deliver. Never rewrite or regenerate code from description — that introduces subtle bugs (e.g. wrong assertion methods, missing attributes). This applies to:
  - **Multitier backend (Java only)** — verbatim copy from eshop backend, then remove tax/coupon/cancel/deliver code
  - **Multitier frontend** — verbatim copy from eshop frontend (including components like Notification), then remove tax/coupon/cancel/deliver code
  - **Test code (ATDD)** — verbatim copy from eshop-tests, then remove tax/coupon/cancel/deliver code
  - **Multitier backend (.NET, TypeScript)** — eshop has no .NET or TypeScript backends, so these are **translations** from the Java backend reference. Expect framework-specific issues (DateTime/Instant handling, JSON serialization defaults, validation pipeline differences). Test thoroughly against the Java backend's behavior as the reference.
  - **Monolith is the exception** — monolith is a rewrite based on the multitier frontend/backend, NOT a verbatim copy from eshop (since eshop has no monolith)

**Execution order:**

### Phase A: Java Multitier

0. ✅ Delete old scaffolding (Echo + Todo) from system implementation and tests → **verify:** builds still pass, no dead code remains
1. ✅ External services (shared infrastructure — external-real-sim + external-stub, including product stub mappings for all SKUs)
2. ✅ Multitier backend Java → **verify:** `./gradlew test` in `system/multitier/backend-java/` passes (backend's own unit tests, not just system tests). When adding dependencies like JPA/PostgreSQL, ensure existing tests still work (e.g. add H2 test profile for `BackendApplicationTests`).
3. ✅ Delete old docker-compose files (`docker-compose.monolith.yml`, `docker-compose.multitier.yml`), create all new docker-compose files (4 multitier + 4 monolith), update Java GitHub Actions workflows to use new file names (e.g. `docker-compose.pipeline.multitier.real.yml`) → **verify:** `docker compose up -d` with local.multitier.real.yml, health checks pass, PlaceOrder + ViewOrder work via API
4. ✅ Multitier frontend React (+ visual style) → **verify:** rebuild, UI loads, can place and view orders in browser
5. ✅ Docker-compose local.multitier.stub.yml → **verify:** `docker compose up -d` with stub, health checks pass, PlaceOrder works against WireMock
6. ✅ Run-SystemTests.ps1 + Run-SystemTests.Config.ps1 → **verify:** script runs `-SkipTests` successfully (starts both systems, health checks pass)
7. ✅ Test configuration (YAML config files, ConfigurationLoader, PropertyLoader, Environment) → **verify:** existing smoke tests pass using config-driven URLs
8. ✅ Remove all hardcoded URLs: frontend pages must use relative URLs (not `localhost:8080`), all test classes (API and UI) must use config-driven URLs from `ConfigurationLoader`. Restructure test folders to match eshop-tests convention (`smoketests/` → `latest/smoke/system/`, `e2etests/` → `latest/e2e/`) → **verify:** `Run-SystemTests.ps1` passes (all suites, no `-Suite` filter)
9. ✅ Test code (ATDD architecture) → **verify:** `./gradlew compileJava compileTestJava` — zero errors (both source and test compilation)
10. ✅ Run full system tests locally (`Run-SystemTests.ps1`) — smoke, e2e, acceptance pass
11. ✅ Acceptance stage workflow (GitHub Actions) → **verify:** CI passes
12. ✅ Trigger `verify-all` with `language=java, architecture=multitier` → **verify:** acceptance stage green (QA/prod stages require manual signoff by design)

### Phase B: .NET Multitier

13. .NET multitier (backend, frontend, docker-compose, tests, workflows) → **verify:** `Run-SystemTests.ps1` passes, unit tests pass
14. Trigger `verify-all` with `language=dotnet, architecture=multitier` → **verify:** all .NET multitier workflows green

### Phase C: TypeScript Multitier

15. TypeScript multitier (backend, frontend, docker-compose, tests, workflows) → **verify:** `Run-SystemTests.ps1` passes, unit tests pass
16. Trigger `verify-all` with `language=typescript, architecture=multitier` → **verify:** all TypeScript multitier workflows green

### Phase D: Java Monolith

17. Monolith backend Java (full SSR with Thymeleaf — PlaceOrder + ViewOrder pages) → **verify:** `docker compose up -d` with local.monolith.real.yml, app loads, PlaceOrder + ViewOrder work; `./gradlew test` in `system/monolith/java/` passes
18. Monolith stub → **verify:** local.monolith.stub.yml works
19. Monolith Run-SystemTests config + tests → **verify:** `Run-SystemTests.ps1` passes for monolith
20. Monolith acceptance stage workflow → **verify:** CI passes
21. Trigger `verify-all` with `language=java, architecture=monolith` → **verify:** all Java monolith workflows green

### Phase E: .NET Monolith

22. .NET monolith (backend, docker-compose, tests, workflows) → **verify:** `Run-SystemTests.ps1` passes, unit tests pass
23. Trigger `verify-all` with `language=dotnet, architecture=monolith` → **verify:** all .NET monolith workflows green

### Phase F: TypeScript Monolith

24. TypeScript monolith (backend, docker-compose, tests, workflows) → **verify:** `Run-SystemTests.ps1` passes, unit tests pass
25. Trigger `verify-all` with `language=typescript, architecture=monolith` → **verify:** all TypeScript monolith workflows green

**Simplifications vs. eshop:**
- Remove Tax (no TaxGateway, no tax calculations)
- Remove Coupons (no CouponService, no discount logic)
- Remove CancelOrder, DeliverOrder
- PlaceOrder: `sku`, `quantity` only (no `country`, `couponCode`)
- `totalPrice = unitPrice * quantity` (no tax, no discount)
- Clock restriction: reject orders placed between 23:59 and 00:00 on December 31st
- ViewOrder: keep `orderNumber`, `sku`, `quantity`, `unitPrice`, `totalPrice`, `status`, `timestamp`

---

## Step 0: Delete Old Scaffolding (Echo + Todo)

Remove all Echo and Todo functionality from every language/architecture. This is legacy scaffolding that is fully superseded by the Order/eShop functionality.

### Multitier backend

| Language | Files to delete |
|---|---|
| Java | `TodoController.java`, `Todo.java` model, `todos.api.host` from `application.yml` |
| .NET | `TodosController.cs`, `ExternalApis:JsonPlaceholder` from `appsettings.json` |
| TypeScript | Todo routes + `getTodo`/`getTodosPageHtml` from `app.controller.ts`/`app.service.ts`, `todosApiBaseUrl` from `app.config.ts` |

Also remove Echo controller/endpoint from each language if present.

### Monolith

| Language | Files to delete |
|---|---|
| Java | `TodoApiController.java`, `TodoWebController.java`, `Todo.java` model, `todos.html` static page, `todos.api.host` from `application.yml` |
| .NET | `TodosController.cs`, `Todos.cshtml` + `Todos.cshtml.cs` Razor pages |
| TypeScript | `app/todos/page.tsx`, `app/api/todos/[id]/route.ts`, nav link to `/todos` from home page |

Also remove Echo controller/endpoint and page from each language if present.

### Frontend React (multitier)

Remove Todo Fetcher page and any nav links to it (if not already replaced by Order pages in step 4).

### Tests

| Language | Files to delete |
|---|---|
| Java | `legacy/e2e/UiE2eTest.java`, `legacy/e2e/ApiE2eTest.java` |
| TypeScript | `e2e-tests/ui-e2e-test.spec.ts`, `e2e-tests/api-e2e-test.spec.ts` |

Also remove any Echo-related test files.

**Verify:** `./gradlew test` passes for Java backends (monolith + multitier), `dotnet build` for .NET, `npm run build` for TypeScript. No references to `todo` or `echo` remain in source code.

---

## Step 1: External Services (starter/system/)

### external-real-sim/ (Node.js simulator)

**Source:** `eshop/external-real-sim/mock-server.js`

Port ERP and Clock endpoints only (no Tax):

| Endpoint | Action |
|---|---|
| `GET /erp/health` | copy |
| `GET /erp/api/products` | copy (returns product list with prices) |
| `GET /clock/health` | copy |
| `GET /clock/api/time` | copy (returns fixed timestamp) |
| `GET /tax/health` | ❌ remove |
| `GET /tax/api/countries` | ❌ remove |

**Location in starter:** `starter/system/external-real-sim/` (shared by both monolith and multitier)

### external-stub/ (WireMock mappings)

**Source:** `eshop/external-stub/mappings/`

| Mapping file | Action |
|---|---|
| `erp-health.json` | copy |
| `clock-health.json` | copy |
| `clock-time.json` | copy |
| `tax-health.json` | ❌ remove |
| `erp-products-hp15.json` | **new** — stub for `GET /erp/api/products/HP-15` (price: 699.99) |
| `erp-products-dell-xps.json` | **new** — stub for `GET /erp/api/products/DELL-XPS` (price: 1299.99) |
| `erp-products-lenovo-t14.json` | **new** — stub for `GET /erp/api/products/LENOVO-T14` (price: 999.99) |

**Note:** eshop's WireMock stubs only had health checks — product lookups were only in the real simulator. The starter needs explicit product stubs so PlaceOrder works in stub mode.

**Location in starter:** `starter/system/external-stub/` (shared by both monolith and multitier)

---

## Step 2: Multitier Backend (starter/system/multitier/backend-{lang}/)

Currently has: Echo endpoint + Todo fetcher (REST API only).

**Add per language (java, dotnet, typescript):**

| Component | Source (eshop) | Action |
|---|---|---|
| OrderController | `backend/api/controller/OrderController.java` | Port `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{orderNumber}` only. Remove cancel/deliver. |
| OrderService | `backend/core/services/OrderService.java` | Port `placeOrder`, `getOrder`, `browseOrderHistory`. Remove tax/coupon logic. `totalPrice = unitPrice * quantity`. |
| Order entity | `backend/core/entities/Order.java` | Remove `country`, `couponCode`, `discountRate`, `discountAmount`, `subtotalPrice`, `taxRate`, `taxAmount`. Keep `orderNumber`, `sku`, `quantity`, `unitPrice`, `totalPrice`, `status`, `orderTimestamp`. |
| OrderStatus enum | `backend/core/entities/OrderStatus.java` | Keep `PLACED` only (no CANCELLED/DELIVERED). |
| OrderRepository | `backend/core/repositories/OrderRepository.java` | Port as-is (minus cancel/deliver queries). |
| PlaceOrderRequest DTO | `backend/api/dtos/PlaceOrderRequest.java` | `sku`, `quantity` only. |
| PlaceOrderResponse DTO | `backend/api/dtos/PlaceOrderResponse.java` | Copy. |
| ViewOrderDetailsResponse DTO | `backend/api/dtos/ViewOrderDetailsResponse.java` | Remove tax/coupon/discount fields. |
| BrowseOrderHistoryResponse DTO | `backend/api/dtos/BrowseOrderHistoryResponse.java` | Remove `country`, `appliedCouponCode`. |
| ErpGateway | `backend/core/services/external/ErpGateway.java` | Copy (calls ERP for product price). |
| ClockGateway | `backend/core/services/external/ClockGateway.java` | Copy (calls Clock for time). |
| TaxGateway | — | ❌ Do NOT port. |
| CouponService | — | ❌ Do NOT port. |

- CORS: Allow frontend origin
- Database: PostgreSQL
- Add H2 test profile for `BackendApplicationTests`

---

## Step 3: Docker-Compose + Workflows

Delete old files (`docker-compose.monolith.yml`, `docker-compose.multitier.yml`) and replace with new layout. Update GitHub Actions workflows to use new file names.

**New layout (per language, 8 files):**

Local variants (build from source):

| File | Services | Ports |
|---|---|---|
| `docker-compose.local.monolith.real.yml` | monolith + postgres + external-real | App: 2101, External: 9103 |
| `docker-compose.local.monolith.stub.yml` | monolith + postgres + external-stub (WireMock) | App: 2102, External: 9104 |
| `docker-compose.local.multitier.real.yml` | backend + frontend + postgres + external-real | FE: 3101, BE: 8101, External: 9101 |
| `docker-compose.local.multitier.stub.yml` | backend + frontend + postgres + external-stub (WireMock) | FE: 3102, BE: 8102, External: 9102 |

Pipeline variants (pre-built images from ghcr.io):

| File | Services | Ports |
|---|---|---|
| `docker-compose.pipeline.monolith.real.yml` | monolith + postgres + external-real | App: 2101, External: 9103 |
| `docker-compose.pipeline.monolith.stub.yml` | monolith + postgres + external-stub (WireMock) | App: 2102, External: 9104 |
| `docker-compose.pipeline.multitier.real.yml` | backend + frontend + postgres + external-real | FE: 3101, BE: 8101, External: 9101 |
| `docker-compose.pipeline.multitier.stub.yml` | backend + frontend + postgres + external-stub (WireMock) | FE: 3102, BE: 8102, External: 9102 |

**Local vs. Pipeline:** Local files use `build:` to compile from source Dockerfiles. Pipeline files use `image:` to pull pre-built images from `ghcr.io/optivem/starter/`.

**Key environment variables (backend/monolith service):**

| Variable | real value | stub value |
|---|---|---|
| `ERP_API_URL` | `http://external-real:9000/erp` | `http://external-stub:8080/erp` |
| `CLOCK_API_URL` | `http://external-real:9000/clock` | `http://external-stub:8080/clock` |
| `EXTERNAL_SYSTEM_MODE` | `real` | `stub` |

**Port strategy:** Different ports per mode — all combinations can run concurrently. Ports avoid conflicts with eshop (3001-3002, 8081-8082, 9001-9002).

**Port convention:** `{service-prefix}{language}{mode}` where service prefix = 2X (monolith), 3X (frontend), 8X (backend), 9X (external); language = 1 (Java), 2 (.NET), 3 (TS); mode = 01/03 (real), 02/04 (stub).

**Full port allocation table:**

| Combo | Monolith (2X) | Frontend (3X) | Backend (8X) | External (9X) |
|---|---|---|---|---|
| **Java multitier real** | — | 3101 | 8101 | 9101 |
| **Java multitier stub** | — | 3102 | 8102 | 9102 |
| Java monolith real | 2101 | — | — | 9103 |
| Java monolith stub | 2102 | — | — | 9104 |
| .NET multitier real | — | 3201 | 8201 | 9201 |
| .NET multitier stub | — | 3202 | 8202 | 9202 |
| .NET monolith real | 2201 | — | — | 9203 |
| .NET monolith stub | 2202 | — | — | 9204 |
| TS multitier real | — | 3301 | 8301 | 9301 |
| TS multitier stub | — | 3302 | 8302 | 9302 |
| TS monolith real | 2301 | — | — | 9303 |
| TS monolith stub | 2302 | — | — | 9304 |

---

## Step 4: Multitier Frontend (starter/system/multitier/frontend-react/)

Currently has: Home page + Todo Fetcher page.

**Add from eshop frontend:**

| Component | Source (eshop) | Action |
|---|---|---|
| OrderHistory page | `frontend/src/pages/OrderHistory.tsx` | Port. Remove cancel/deliver status display. |
| OrderDetails page | `frontend/src/pages/OrderDetails.tsx` | Port. Remove cancel/deliver actions. Remove tax/coupon display. |
| OrderForm component | `frontend/src/features/orders/OrderForm.tsx` | Port. Remove country/coupon fields. |
| OrderDetailView component | `frontend/src/features/orders/OrderDetailView.tsx` | Port. Remove tax/coupon/discount rows. |
| OrderHistoryTable component | `frontend/src/features/orders/OrderHistoryTable.tsx` | Port. Remove country/coupon columns. |
| OrderActions component | — | ❌ Do NOT port (no cancel/deliver). |
| order-service.ts | `frontend/src/services/order-service.ts` | Port `placeOrder`, `getOrder`, `browseOrderHistory` only. |
| useOrderForm hook | `frontend/src/hooks/useOrderForm.ts` | Port. Remove coupon/country validation. |
| useOrderDetails hook | `frontend/src/hooks/useOrderDetails.ts` | Port. Remove cancel/deliver actions. |
| useOrders hook | `frontend/src/hooks/useOrders.ts` | Port. |
| API types | `frontend/src/types/api.types.ts` | Port order-related types. Remove tax/coupon/discount fields. |

- nginx proxies `/api/*` to backend
- Use relative URLs (not hardcoded `localhost:8080`)

### Visual Style

Port the eshop frontend visual design. Bootstrap 5 via CDN + custom CSS.

| Aspect | What to port |
|---|---|
| Global styles | CSS/theme files, color palette, fonts, layout |
| Component styling | Form inputs, buttons, tables, cards, navigation |
| Page layout | Header, nav, content area, responsive design |
| Branding | Rebrand to "eShop Starter" |

---

## Step 6: Run-SystemTests.ps1 + Config

**Source:** `eshop/Run-SystemTests.ps1` (470 lines)

Port to `starter/system-test/{lang}/Run-SystemTests.ps1` with these changes:

| Aspect | eshop | starter |
|---|---|---|
| Architecture dimension | None (always multitier) | Monolith vs. Multitier (`-Architecture` param) |
| External modes | `real`, `stub` | `real`, `stub` (same) |
| Compose file pattern | `docker-compose.$Mode.$ExternalMode.yml` | `docker-compose.$Mode.$Architecture.$ExternalMode.yml` |
| Container names | `eshop-real`, `eshop-stub` | `starter-$Architecture-$ExternalMode` (e.g. `starter-monolith-real`) |
| System components | FE: 3001/3002, BE: 8081/8082 | FE: 3101/3102, BE: 8101/8102 (no eshop conflicts) |
| External systems | ERP+Tax: 9001/9002, Clock: 9002 | ERP+Clock: 9101/9102 (no Tax, no eshop conflicts) |
| `-Mode` param | `local` / `pipeline` | `local` / `pipeline` (same) |
| Config loading | `Run-SystemTests.Config.ps1` | Split into 3 files (see below) |
| `-Legacy` switch | N/A | New param: runs legacy suites instead of latest |

**Config file split (3 files):**

| File | Contents |
|---|---|
| `Run-SystemTests.Config.ps1` | Shared settings only: `TestFilter`, `BuildCommands` |
| `Run-SystemTests.Latest.Config.ps1` | `Suites` for latest tests only |
| `Run-SystemTests.Legacy.Config.ps1` | `Suites` for legacy modules (mod02–mod11) |

The main script loads shared config from `Run-SystemTests.Config.ps1`, then loads suites from either `Latest` or `Legacy` config based on the `-Legacy` switch:
- `.\Run-SystemTests.ps1` → runs Latest suites (default)
- `.\Run-SystemTests.ps1 -Legacy` → runs Legacy suites

**Run-SystemTests.Latest.Config.ps1:**

| Aspect | eshop-tests | starter |
|---|---|---|
| Test suites | latest only | Same — must include all test types that the pipeline runs |
| Suite IDs | `smoke-stub`, `smoke-real`, etc. | `smoke-stub`, `smoke-real`, `acceptance-api`, `acceptance-ui`, `acceptance-isolated-api`, `acceptance-isolated-ui`, `contract-stub`, `contract-real`, `e2e-api`, `e2e-ui` (matches eshop latest ordering) |
| System properties | `-DexternalSystemMode=real/stub -Denvironment=local` | Same |
| Test filter pattern | `-Dversion=latest -Dtype=smoke`, etc. | Same — uses `-Dversion` + `-Dtype` system properties (NOT `--tests` globs, which match across all modules) |

**Run-SystemTests.Legacy.Config.ps1:**

| Aspect | eshop-tests | starter |
|---|---|---|
| Test suites | mod02-mod11 | Same modules, using `-Dversion=modXX -Dtype=smoke/e2e/acceptance/contract` system properties (NOT `--tests` globs) |
| Suite IDs | `mod02-smoke`, `mod03-e2e`, etc. | Same IDs as eshop-tests |
| System properties | `-DexternalSystemMode=real/stub -Denvironment=local` | Same |

---

## Step 7: Test Configuration

YAML config files, ConfigurationLoader, PropertyLoader, Environment, ExternalSystemMode — so tests use config-driven URLs instead of hardcoded ports.

- `test-config-local-real.yml` — URLs for real mode ports
- `test-config-local-stub.yml` — URLs for stub mode ports
- Forward system properties (`environment`, `externalSystemMode`, `channel`, `mode`, etc.) in `build.gradle`
- `build.gradle` test filter must support `-Dversion` + `-Dtype` combined filtering (matching eshop-tests pattern: `includeTestsMatching "*${version}*${type}*"`), NOT `--tests` glob patterns which match across all modules

---

## Step 8: Test Folder Restructure

Rename test folders to match eshop-tests convention:
- `smoketests/` → `latest/smoke/system/`
- `e2etests/` → `latest/e2e/`

Update all test classes to use config-driven URLs from `ConfigurationLoader`.

**Post-step 9 cleanup:** Pre-ATDD test files (ApiSmokeTest, UiSmokeTest, ApiE2eTest, UiE2eTest) were removed — they are superseded by the ATDD DSL-based tests and the full `legacy/` module journey (mod02–mod11) copied from eshop-tests.

---

## Step 9: ATDD Test Code (eshop-tests → starter/system-test)

**Source:** eshop-tests/java/ (and dotnet/, typescript/)

Package names kept identical (`com.optivem.eshop.dsl`). Only changes: remove Tax, remove Coupon, simplify PlaceOrder/ViewOrder.

### Domain Simplifications

| eshop-tests | starter |
|---|---|
| `Shop` / `shop` | unchanged |
| `ERP` / `Erp` / `erp` | unchanged |
| `Tax` / `tax` | ❌ removed |
| `Clock` / `clock` | unchanged |
| `PlaceOrder`, `ViewOrder` | unchanged |
| `CancelOrder`, `DeliverOrder`, `BrowseCoupons`, `PublishCoupon` | ❌ removed |
| `GivenCountry`, `ThenCountry`, `GivenCoupon`, `ThenCoupon` | ❌ removed |
| `CouponManagementPage` | ❌ removed |

**PlaceOrder simplifications:**
- Request: `sku`, `quantity` only (remove `country`, `couponCode`)
- `totalPrice = unitPrice * quantity` (no tax)
- Clock restriction: reject orders placed between 23:59 and 00:00 on December 31st

**ViewOrderResponse:** keep `orderNumber`, `sku`, `quantity`, `unitPrice`, `totalPrice`, `status`, `timestamp` — remove tax/coupon/discount fields

### File-by-File Port

Source root: `eshop-tests/java/`
Target main: `starter/system-test/java/src/main/java/`
Target test: `starter/system-test/java/src/test/java/`

#### common/
| File | Action |
|---|---|
| `common/Closer.java` | copy |
| `common/Converter.java` | copy |
| `common/Result.java` | copy |
| `common/ResultAssert.java` | copy |

#### driver/port/
| File | Action |
|---|---|
| `driver/port/shop/ShopDriver.java` | keep `goToShop`, `placeOrder`, `viewOrder` |
| `driver/port/shop/dtos/PlaceOrderRequest.java` | remove `country`, `couponCode` |
| `driver/port/shop/dtos/PlaceOrderResponse.java` | copy |
| `driver/port/shop/dtos/ViewOrderResponse.java` | remove tax/coupon/discount fields; keep `orderNumber`, `sku`, `quantity`, `unitPrice`, `totalPrice`, `status`, `timestamp` |
| `driver/port/shared/dtos/ErrorResponse.java` | copy |
| `driver/port/external/erp/ErpDriver.java` | copy |
| `driver/port/external/erp/dtos/GetProductRequest.java` | copy |
| `driver/port/external/erp/dtos/GetProductResponse.java` | copy |
| `driver/port/external/erp/dtos/ReturnsProductRequest.java` | copy |
| `driver/port/external/clock/ClockDriver.java` | copy |
| `driver/port/external/clock/dtos/GetTimeResponse.java` | copy |
| `driver/port/external/clock/dtos/ReturnsTimeRequest.java` | copy |

#### driver/adapter/ — shared
| File | Action |
|---|---|
| `driver/adapter/shared/client/http/JsonHttpClient.java` | copy |
| `driver/adapter/shared/client/http/HttpStatus.java` | copy |
| `driver/adapter/shared/client/wiremock/JsonWireMockClient.java` | copy |
| `driver/adapter/shared/client/playwright/PageClient.java` | copy |

#### driver/adapter/ — shop API
| File | Action |
|---|---|
| `driver/adapter/shop/api/ShopApiDriver.java` | keep `goToShop`, `placeOrder`, `viewOrder` |
| `driver/adapter/shop/api/SystemErrorMapper.java` | copy |
| `driver/adapter/shop/api/client/ShopApiClient.java` | copy |
| `driver/adapter/shop/api/client/controllers/HealthController.java` | copy |
| `driver/adapter/shop/api/client/controllers/OrderController.java` | remove `cancelOrder`, `deliverOrder` |
| `driver/adapter/shop/api/client/dtos/errors/ProblemDetailResponse.java` | copy |

#### driver/adapter/ — shop UI
| File | Action |
|---|---|
| `driver/adapter/shop/ui/ShopUiDriver.java` | keep `goToShop`, `placeOrder`, `viewOrder` |
| `driver/adapter/shop/ui/client/ShopUiClient.java` | copy |
| `driver/adapter/shop/ui/client/pages/BasePage.java` | copy |
| `driver/adapter/shop/ui/client/pages/HomePage.java` | copy |
| `driver/adapter/shop/ui/client/pages/NewOrderPage.java` | copy |
| `driver/adapter/shop/ui/client/pages/OrderDetailsPage.java` | copy |
| `driver/adapter/shop/ui/client/pages/OrderHistoryPage.java` | copy |

#### driver/adapter/ — external ERP
| File | Action |
|---|---|
| `driver/adapter/external/erp/BaseErpDriver.java` | copy |
| `driver/adapter/external/erp/ErpRealDriver.java` | copy |
| `driver/adapter/external/erp/ErpStubDriver.java` | copy |
| `driver/adapter/external/erp/client/BaseErpClient.java` | copy |
| `driver/adapter/external/erp/client/ErpRealClient.java` | copy |
| `driver/adapter/external/erp/client/ErpStubClient.java` | copy |
| `driver/adapter/external/erp/client/dtos/ExtProductDetailsResponse.java` | copy |
| `driver/adapter/external/erp/client/dtos/error/ExtErpErrorResponse.java` | copy |

#### driver/adapter/ — external Clock
| File | Action |
|---|---|
| `driver/adapter/external/clock/ClockRealDriver.java` | copy |
| `driver/adapter/external/clock/ClockStubDriver.java` | copy |
| `driver/adapter/external/clock/client/ClockRealClient.java` | copy |
| `driver/adapter/external/clock/client/ClockStubClient.java` | copy |
| `driver/adapter/external/clock/client/dtos/ExtGetTimeResponse.java` | copy |
| `driver/adapter/external/clock/client/dtos/error/ExtClockErrorResponse.java` | copy |

#### channel/
| File | Action |
|---|---|
| `channel/ChannelType.java` | copy |

#### dsl/core/shared/
| File | Action |
|---|---|
| `dsl/core/shared/UseCase.java` | copy |
| `dsl/core/shared/BaseUseCase.java` | copy |
| `dsl/core/shared/UseCaseContext.java` | copy |
| `dsl/core/shared/UseCaseResult.java` | copy |
| `dsl/core/shared/ResponseVerification.java` | copy |
| `dsl/core/shared/ErrorVerification.java` | copy |
| `dsl/core/shared/VoidVerification.java` | copy |

#### dsl/core/usecase/
| File | Action |
|---|---|
| `dsl/core/usecase/Configuration.java` | copy |
| `dsl/core/usecase/UseCaseDsl.java` | remove tax, keep shop+erp+clock |
| `dsl/core/usecase/shop/ShopDsl.java` | keep `goToShop`, `placeOrder`, `viewOrder` |
| `dsl/core/usecase/shop/commons/SystemResults.java` | copy |
| `dsl/core/usecase/shop/usecases/base/BaseShopUseCase.java` | copy |
| `dsl/core/usecase/shop/usecases/GoToShop.java` | copy |
| `dsl/core/usecase/shop/usecases/PlaceOrder.java` | remove country/coupon |
| `dsl/core/usecase/shop/usecases/PlaceOrderVerification.java` | copy |
| `dsl/core/usecase/shop/usecases/ViewOrder.java` | copy |
| `dsl/core/usecase/shop/usecases/ViewOrderVerification.java` | remove tax/coupon/discount |
| `dsl/core/usecase/external/erp/ErpDsl.java` | copy |
| `dsl/core/usecase/external/erp/usecases/base/BaseErpUseCase.java` | copy |
| `dsl/core/usecase/external/erp/usecases/GoToErp.java` | copy |
| `dsl/core/usecase/external/erp/usecases/GetProduct.java` | copy |
| `dsl/core/usecase/external/erp/usecases/GetProductVerification.java` | copy |
| `dsl/core/usecase/external/erp/usecases/ReturnsProduct.java` | copy |
| `dsl/core/usecase/external/clock/ClockDsl.java` | copy |
| `dsl/core/usecase/external/clock/usecases/base/BaseClockUseCase.java` | copy |
| `dsl/core/usecase/external/clock/usecases/GoToClock.java` | copy |
| `dsl/core/usecase/external/clock/usecases/GetTime.java` | copy |
| `dsl/core/usecase/external/clock/usecases/GetTimeVerification.java` | copy |
| `dsl/core/usecase/external/clock/usecases/ReturnsTime.java` | copy |

#### dsl/core/scenario/
| File | Action |
|---|---|
| `dsl/core/ScenarioDslImpl.java` | copy |
| `dsl/core/scenario/ExecutionResult.java` | copy |
| `dsl/core/scenario/ExecutionResultBuilder.java` | keep `orderNumber` only (no couponCode) |
| `dsl/core/scenario/ExecutionResultContext.java` | keep `orderNumber` only |
| `dsl/core/scenario/ScenarioDefaults.java` | keep ORDER_NUMBER, SKU, QUANTITY, UNIT_PRICE; remove country/coupon/tax |
| `dsl/core/scenario/assume/AssumeImpl.java` | remove tax; keep erp+clock+shop |
| `dsl/core/scenario/given/GivenImpl.java` | remove country/coupon; keep product+order+clock |
| `dsl/core/scenario/given/steps/BaseGivenStep.java` | copy |
| `dsl/core/scenario/given/steps/GivenProductImpl.java` | copy |
| `dsl/core/scenario/given/steps/GivenOrderImpl.java` | remove country/coupon |
| `dsl/core/scenario/given/steps/GivenClockImpl.java` | copy |
| `dsl/core/scenario/when/WhenImpl.java` | remove tax ensureDefaults; keep placeOrder+viewOrder |
| `dsl/core/scenario/when/steps/BaseWhenStep.java` | copy |
| `dsl/core/scenario/when/steps/WhenPlaceOrderImpl.java` | remove country/coupon |
| `dsl/core/scenario/when/steps/WhenViewOrderImpl.java` | copy |
| `dsl/core/scenario/then/ThenImpl.java` | remove country/tax; keep product+clock |
| `dsl/core/scenario/then/ThenResultImpl.java` | copy |
| `dsl/core/scenario/then/steps/BaseThenStep.java` | remove coupon |
| `dsl/core/scenario/then/steps/ThenSuccessImpl.java` | copy |
| `dsl/core/scenario/then/steps/ThenFailureImpl.java` | copy |
| `dsl/core/scenario/then/steps/ThenOrderImpl.java` | remove tax/coupon/discount |
| `dsl/core/scenario/then/steps/ThenProductImpl.java` | copy |
| `dsl/core/scenario/then/steps/ThenClockImpl.java` | copy |

#### configuration + infrastructure (src/main/java)
| File | Action |
|---|---|
| `systemtest/configuration/BaseConfigurableTest.java` | remove tax driver creation |
| `systemtest/infrastructure/playwright/BrowserLifecycleExtension.java` | copy |

#### test classes (src/test/java)
| File | Action |
|---|---|
| `latest/base/BaseScenarioDslTest.java` | copy |
| `latest/e2e/base/BaseE2eTest.java` | copy |
| `latest/e2e/PlaceOrderPositiveTest.java` | copy |
| `latest/acceptance/base/BaseAcceptanceTest.java` | copy |
| `latest/acceptance/PlaceOrderPositiveTest.java` | copy |
| `latest/acceptance/PlaceOrderNegativeTest.java` | copy |
| `latest/acceptance/PlaceOrderPositiveIsolatedTest.java` | dummy (TODO) |
| `latest/acceptance/PlaceOrderNegativeIsolatedTest.java` | dummy (TODO) |
| `latest/smoke/system/ShopSmokeTest.java` | copy |
| `latest/smoke/external/ErpSmokeTest.java` | copy |
| `latest/smoke/external/ClockSmokeTest.java` | copy |
| `latest/contract/base/BaseExternalSystemContractTest.java` | copy (no Tax) |
| `latest/contract/erp/BaseErpContractTest.java` | copy |
| `latest/contract/erp/ErpStubContractTest.java` | copy |
| `latest/contract/erp/ErpRealContractTest.java` | copy |
| `latest/contract/clock/BaseClockContractTest.java` | copy |
| `latest/contract/clock/ClockStubContractTest.java` | copy |
| `latest/contract/clock/ClockRealContractTest.java` | copy |

**Important:** When migrating test types, ensure **all test types that the acceptance-stage workflow runs** (smoke, acceptance, contract, e2e) are also present as suites in `Run-SystemTests.Latest.Config.ps1`. A mismatch means CI failures won't be caught locally.

### Legacy modules (test journey)

Copy the `legacy/` directory from eshop-tests into starter's test source tree, then apply the same exclusions as the rest of the starter (remove Tax, Coupon, CancelOrder, DeliverOrder, Country). Only PlaceOrder and ViewOrder functionality should remain. Package name: `com.optivem.eshop.systemtest.legacy`.

| Module | Focus |
|---|---|
| `legacy/mod02` | smoke tests (raw HTTP + Playwright) |
| `legacy/mod03` | e2e tests (raw HTTP + Playwright) |
| `legacy/mod04` | client abstraction layer |
| `legacy/mod05` | driver abstraction (API + UI channels) |
| `legacy/mod06` | channel driver pattern |
| `legacy/mod07` | use case DSL |
| `legacy/mod08` | scenario DSL |
| `legacy/mod09` | external system smoke tests (Clock) |
| `legacy/mod10` | acceptance tests |
| `legacy/mod11` | contract tests |

These are not executed in CI — they exist purely to show the journey for learning purposes. They can be run locally via `.\Run-SystemTests.ps1 -Legacy` (or with `-Suite mod05-e2e` to run a specific one).

#### Files to delete entirely

| Module | File | Reason |
|---|---|---|
| mod02–mod09 | `smoke/external/TaxSmokeTest.java` | Tax excluded |
| mod10 | `acceptance/CancelOrderPositiveTest.java` | CancelOrder excluded |
| mod10 | `acceptance/CancelOrderPositiveIsolatedTest.java` | CancelOrder excluded |
| mod10 | `acceptance/CancelOrderNegativeTest.java` | CancelOrder excluded |
| mod10 | `acceptance/CancelOrderNegativeIsolatedTest.java` | CancelOrder excluded |
| mod10 | `acceptance/PublishCouponPositiveTest.java` | Coupon excluded |
| mod10 | `acceptance/PublishCouponNegativeTest.java` | Coupon excluded |
| mod10 | `acceptance/BrowseCouponsPositiveTest.java` | Coupon excluded |
| mod11 | `contract/tax/BaseTaxContractTest.java` | Tax excluded |
| mod11 | `contract/tax/TaxRealContractTest.java` | Tax excluded |
| mod11 | `contract/tax/TaxStubContractTest.java` | Tax excluded |

#### Surgical removals across legacy modules

| What to remove | Where |
|---|---|
| `TaxRealClient` import, field, instantiation, teardown | `mod04/base/BaseClientTest.java` |
| `TaxRealDriver` import, field, instantiation, teardown | `mod05/base/BaseDriverTest.java`, `mod06/base/BaseChannelDriverTest.java` |
| `getTaxBaseUrl()` method, `taxHttpClient` field + setup | `mod02/base/BaseRawTest.java`, `mod03/base/BaseRawTest.java` |
| `.country()` / `COUNTRY` from PlaceOrder requests **and** delete country-specific test methods (`shouldRejectOrderWithEmptyCountry`, `shouldRejectOrderWithInvalidCountry`, `shouldRejectOrderWithNullCountry`) | e2e tests in mod03–mod08, mod10 |
| `.coupon()` / `.withCouponCode()` / `.withDiscountRate()` from DSL calls | mod10–mod11 acceptance/e2e tests |
| `subtotalPrice`, `discountRate`, `discountAmount`, `taxRate`, `taxAmount` from assertions | e2e tests in mod03–mod11 |
| `Configuration` type: use `com.optivem.eshop.systemtest.configuration.Configuration` (not `dsl.core.usecase.Configuration`) | `mod02/base/BaseRawTest.java`, `mod03/base/BaseRawTest.java`, `mod04/base/BaseClientTest.java`, `mod05/base/BaseDriverTest.java`, `mod06/base/BaseChannelDriverTest.java` |
| `ExternalSystemMode` type: use `com.optivem.eshop.systemtest.configuration.ExternalSystemMode` (not `dsl.port.ExternalSystemMode`) | e2e base classes in mod03–mod08, mod10 acceptance base, mod11 contract base + all concrete contract tests (clock/erp Real+Stub) |

**Verify:** `./gradlew compileJava compileTestJava` — zero errors (both source and test compilation).

---

## Step 13: Monolith (starter/system/monolith/)

Currently has: Echo endpoint + Todo fetcher (SSR with Thymeleaf/Razor/Next.js).

**Add per language (java, dotnet, typescript):**

| Component | Source (eshop) | Action |
|---|---|---|
| OrderController | `backend/api/controller/OrderController.java` | Port `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{orderNumber}` only. Remove cancel/deliver. |
| OrderService | `backend/core/services/OrderService.java` | Port `placeOrder`, `getOrder`, `browseOrderHistory`. Remove tax/coupon logic. `totalPrice = unitPrice * quantity`. |
| Order entity | `backend/core/entities/Order.java` | Simplified fields only. |
| ErpGateway, ClockGateway | Copy from eshop. |
| Order UI pages | Port as SSR templates: New Order form (sku + quantity), Order History, Order Details. No cancel/deliver buttons. |

**Monolith-specific notes:**
- Server-side rendered (Thymeleaf for Java, Razor for .NET, Next.js for TypeScript)
- Single service with embedded frontend — no nginx, no CORS
- Database: PostgreSQL

---

## Decisions

1. **Port strategy:** Different ports per mode — all combinations run concurrently (same as eshop). Real and stub each have their own port range.
2. **`-Mode` param:** Keep both `local` and `pipeline`. Local builds images from source; pipeline uses pre-built images from container registry (needed for acceptance stage CI/CD, same as eshop).
3. **Database:** PostgreSQL (same as eshop).
4. **Monolith SSR:** Full SSR (Thymeleaf for Java, Razor for .NET, Next.js for TypeScript). No embedded JS or shared REST API layer.
5. **Tracer bullet approach for legacy modules:** Each legacy module (e.g. mod03) covers exactly one positive and one negative scenario per feature — not exhaustive validation coverage. The single negative (`shouldRejectOrderWithNonIntegerQuantity`) is intentionally duplicated across API and UI test classes to make the duplication pain visible. Full validation coverage belongs at the acceptance test level (`latest/`) where the DSL handles it concisely.

---

> **Cleanup reminder:** Once all migration steps are complete and verified, delete this file (`starter-migration-plan.md`) from the starter repo — it is a temporary planning artifact and should not be kept in the final codebase.
