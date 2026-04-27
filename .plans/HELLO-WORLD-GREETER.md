# Create `optivem/greeter` — Minimal Template Repo

## Context

The `optivem/shop` repo (Shop domain) is a full e-commerce system with orders, coupons, pricing, external integrations (ERP, Clock, Tax), and a complex domain model. It serves as:
- The reference architecture for the ATDD course
- The scaffolding source for `gh optivem init`

We need a simpler template repo for:
1. **Pipeline course** — students learn CI/CD stages without domain complexity
2. **Beginners** — first-time `gh optivem` users who want a simpler starting point
3. **Real project scaffolding** — a complete template people can build on

The repo is called `greeter` (not `hello-world`) because it describes the domain. Archived `optivem/greeter-*` repos are old ATDD templates and won't conflict.

---

## Decisions

| Decision | Choice | Reason |
|---|---|---|
| **Repo name** | `optivem/greeter` | Describes the domain; old greeter repos are archived |
| **System name** | `"Greeter"` | Maps to GreeterService, GreeterApiController |
| **Separate repo vs inside shop** | Separate repo | Avoids doubling shop's ~68 workflow files |
| **Database** | Postgres | Template must support real project scaffolding with persistence |
| **CRUD** | POST + GET (list) + GET by id | Standard CRUD pattern for real-project scaffolding |
| **External systems** | Clock + Scorer | Clock for determinism (isolated via stub); Scorer for opaque external logic (non-isolated by design) |
| **Stubs (WireMock)** | Yes | Required to demonstrate acceptance testing |
| **External simulator** | Yes | Same pattern as shop, two endpoints |
| **Acceptance tests** | Yes, full harness | Template must be complete for real project scaffolding |
| **UI** | Yes, minimal | Text field + button + result display |
| **Frontend framework** | React + Vite (multitier), Thymeleaf/Razor/Next.js (monolith) | Same as shop |

---

## Domain

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/greetings` | Create a greeting |
| GET | `/api/greetings` | List all greetings |
| GET | `/api/greetings/{id}` | Get one greeting |

### POST /api/greetings

Request (`message` optional, defaults to `"Hello, World!"`):
```json
{ "message": "Hello, World!" }
```

Response (`201 Created`):
```json
{
  "id": 1,
  "message": "Hello, World!",
  "score": 42,
  "timestamp": "2026-04-17T10:30:00Z"
}
```

### GET /api/greetings/{id}

Response: same shape as POST response.

### GET /api/greetings

Response: array of greeting records, newest first.

### Business logic (`GreeterService.create`)

1. Accept `message` (default `"Hello, World!"`) — local logic
2. Call Scorer: `GET /api/score?text={message}` → `{"score": 42}` — **external I/O, non-isolated (opaque logic)**
3. Call Clock: `GET /api/clock` → `{"timestamp": "..."}` — **external I/O, isolated via stub (deterministic)**
4. Persist `{ id, message, score, timestamp }` — database
5. Return persisted record

### UI

```
[ Hello, World!                ] [ Greet ]

Recent greetings:
#2  Hello, Alice!   score: 38   2026-04-17T11:05:00Z
#1  Hello, World!   score: 42   2026-04-17T10:30:00Z
```

Input field defaults to `"Hello, World!"`. List shows persisted greetings (newest first).

### Architecture layers

```
Controller (API)  →  Service  →  External gateways (Clock, Scorer)
                              →  Repository  →  Database
     ↑
  DTO (response)
```

One entity (`Greeting`), one repository, one service. DB migrations managed per backend (Flyway / EF Core / TypeORM).

---

## Repo Structure

```
greeter/
├── system/
│   ├── monolith/
│   │   ├── java/                    # Spring Boot + Thymeleaf
│   │   ├── dotnet/                  # ASP.NET Core + Razor
│   │   └── typescript/              # Next.js
│   ├── multitier/
│   │   ├── backend-java/            # Spring Boot API
│   │   ├── backend-dotnet/          # ASP.NET Core API
│   │   ├── backend-typescript/      # NestJS API
│   │   └── frontend-react/          # React + Vite + Nginx
│   ├── external-real-sim/           # Node.js simulator (Clock + Score)
│   └── external-stub/               # WireMock mappings (Clock only — Score is always real)
├── system-test/
│   ├── java/                        # JUnit 5 + Playwright
│   ├── dotnet/                      # xUnit + Playwright
│   └── typescript/                  # Jest + Playwright
├── .github/workflows/               # CI/CD pipelines
├── docs/                            # Architecture docs
├── VERSION                          # Semantic version
└── README.md
```

### Comparison with shop

| Component | shop | greeter |
|---|---|---|
| **API controllers** | OrderApiController, CouponApiController, HealthController | GreeterApiController, HealthController |
| **Services** | OrderService, CouponService | GreeterService |
| **DTOs** | PlaceOrderRequest/Response, BrowseCouponsResponse, ViewOrderDetailsResponse, PublishCouponRequest | CreateGreetingRequest, GreetingResponse |
| **Entities** | Order, OrderStatus, Coupon | Greeting |
| **Repositories** | OrderRepository, CouponRepository | GreetingRepository |
| **External gateways** | ErpGateway, ClockGateway, TaxGateway | ClockGateway, ScorerGateway |
| **External DTOs** | GetPromotionResponse, GetTimeResponse, ProductDetailsResponse, TaxDetailsResponse | GetTimeResponse, GetScoreResponse |
| **Exception handling** | GlobalExceptionHandler, ValidationException, NotExistValidationException | GlobalExceptionHandler |
| **Web controllers (monolith)** | HomeController, ShopController, OrderHistoryController, OrderDetailsController, AdminCouponsController | HomeController |
| **Frontend pages** | Home, NewOrder, OrderHistory, OrderDetails, AdminCoupons | Home (single page) |
| **Docker Compose services** | App + DB + External Sim + WireMock | App + DB + External Sim + WireMock |
| **WireMock mappings** | Clock, ERP products, ERP promotions, Tax | Clock only (Score always uses real-sim — non-isolated by design) |

---

## Source Files Per Backend Variant

Each backend variant (e.g. `system/monolith/java/`) contains:

| File | Purpose |
|---|---|
| `GreeterApiController` | `POST /api/greetings`, `GET /api/greetings`, `GET /api/greetings/{id}` |
| `HealthController` | `GET /health` endpoint |
| `GreeterService` | Business logic: score + clock + persist |
| `Greeting` | Entity: id, message, score, timestamp |
| `GreetingRepository` | Persistence (Spring Data / EF Core / TypeORM) |
| `CreateGreetingRequest` | Request DTO: message (optional) |
| `GreetingResponse` | Response DTO: id, message, score, timestamp |
| `ClockGateway` | HTTP client for Clock external API |
| `ScorerGateway` | HTTP client for Scorer external API |
| `GetTimeResponse` | DTO for Clock API response |
| `GetScoreResponse` | DTO for Scorer API response |
| `GlobalExceptionHandler` | Error handling |
| `Application` (main class) | Spring Boot / ASP.NET / NestJS entry point |
| `Dockerfile` | Container build |
| `VERSION` | Semantic version |
| Build config | `build.gradle` / `.csproj` / `package.json` |
| DB migration (V1) | Create `greetings` table (Flyway / EF Core migration / TypeORM migration) |

~15 source files per variant vs ~25+ in shop.

---

## External Systems

Two external systems, intentionally different in **testability**:

| System | Stubbable? | Test isolation |
|---|---|---|
| **Clock** | Yes (WireMock) | Isolated — test sets exact timestamp, asserts exact timestamp |
| **Scorer** | No (real-sim only) | Non-isolated — test asserts only on shape (score is an integer) |

### External Real Simulator (`external-real-sim/`)

Node.js Express app with two endpoints:

```
GET /api/clock            → {"timestamp": "<current ISO timestamp>"}
GET /api/score?text=...   → {"score": <opaque integer>}
```

**Clock** returns `new Date().toISOString()`.

**Scorer** returns an integer derived from `text` via an **intentionally opaque** algorithm (e.g. `sum(charCodes) % 100`). The algorithm is not documented to consumers — this is the teaching point: the real system is a black box, so tests must not depend on specific values.

### External Stubs (`external-stub/`)

Only Clock is stubbed. In both `stub` and `real` Docker Compose variants, the Scorer is served by `external-real-sim`. This forces tests touching the Scorer to assert on shape, not values — demonstrating the non-isolated test pattern.

**Clock stub:**
```json
{
  "request": { "method": "GET", "url": "/api/clock" },
  "response": {
    "status": 200,
    "jsonBody": { "timestamp": "2026-01-15T10:30:00Z" },
    "headers": { "Content-Type": "application/json" }
  }
}
```

---

## System Tests

### Test Scenarios

Two styles of assertions, demonstrating **isolated** (Clock, stubbed) vs **non-isolated** (Scorer, real & opaque) external dependencies.

#### Isolated — Clock

```
Given the system is running
  And the Clock is set to "2026-01-15T10:30:00Z"
When I POST /api/greetings with body {"message": "Hello, World!"}
Then the response status is 201
  And the response message is "Hello, World!"
  And the response timestamp is "2026-01-15T10:30:00Z"
```

The Clock stub lets the test assert the exact timestamp — external output is fully controlled.

#### Non-isolated — Scorer

```
Given the system is running
When I POST /api/greetings with body {"message": "Hello, World!"}
Then the response status is 201
  And the response message is "Hello, World!"
  And the response score is an integer
```

The Scorer is always real. The test cannot predict the value, so it asserts only on shape — demonstrating how to test against an opaque external service.

#### Persistence round-trip

```
Given the system is running
  And the Clock is set to "2026-01-15T10:30:00Z"
When I POST /api/greetings with body {"message": "Hello, World!"}
  And I GET /api/greetings/{id} with the returned id
Then the returned record equals the POST response
```

#### UI e2e test (Playwright)

```
Given the system is running
  And the Clock is set to "2026-01-15T10:30:00Z"
When I type "Hello, World!" in the message field
  And I click the Greet button
Then the page shows a new greeting row with:
  - message "Hello, World!"
  - timestamp "2026-01-15T10:30:00Z"
  - an integer score
```

### Test Harness Structure

Same DSL layers as shop, minimal content:

```
system-test/{lang}/
├── Channel/               # Test channel config (API, UI)
├── Common/                # Shared utilities
├── Dsl.Core/
│   └── UseCase/
│       ├── Greeter/       # Greeting test DSL
│       └── External/      # Clock, Quote test DSL
├── Dsl.Port/
│   ├── Assume/            # Setup (Given)
│   ├── When/              # Actions (When)
│   └── Then/              # Assertions (Then)
├── Driver.Port/           # Interfaces
├── Driver.Adapter/
│   ├── Greeter.Api.Client/    # HTTP client for API
│   ├── Greeter.Ui.Client/     # Playwright browser client
│   └── External.Clock.Client/ # HTTP client for Clock stub (sets expected timestamp)
├── SystemTests/           # Actual test classes
├── docker-compose.*.yml   # Variants: local/pipeline × stub/real
└── build config           # build.gradle / .csproj / package.json
```

### Docker Compose Variants

| Variant | Services |
|---|---|
| `local.monolith.stub` | App + Postgres + WireMock (Clock) + Real-sim (Scorer) |
| `local.monolith.real` | App + Postgres + Real-sim (Clock + Scorer) |
| `local.multitier.stub` | Backend + Frontend + Postgres + WireMock (Clock) + Real-sim (Scorer) |
| `local.multitier.real` | Backend + Frontend + Postgres + Real-sim (Clock + Scorer) |
| `pipeline.monolith.stub` | App (from GHCR) + Postgres + WireMock (Clock) + Real-sim (Scorer) |
| `pipeline.monolith.real` | App (from GHCR) + Postgres + Real-sim (Clock + Scorer) |
| `pipeline.multitier.stub` | Backend + Frontend (from GHCR) + Postgres + WireMock (Clock) + Real-sim (Scorer) |
| `pipeline.multitier.real` | Backend + Frontend (from GHCR) + Postgres + Real-sim (Clock + Scorer) |

In `stub` variants, Clock is served by WireMock and Scorer by real-sim. In `real` variants, both are served by real-sim. Postgres is always present.

---

## CI/CD Workflows

Same naming convention as shop. Same stages, simplified content.

### Workflow files

For each `{arch}-{lang}` combination:
- `{arch}-{lang}-commit-stage.yml`
- `{arch}-{lang}-acceptance-stage.yml`
- `{arch}-{lang}-acceptance-stage-legacy.yml`
- `{arch}-{lang}-qa-stage.yml`
- `{arch}-{lang}-qa-signoff.yml`
- `{arch}-{lang}-prod-stage.yml`

Plus cloud-run variants (`-cloud.yml`) for applicable stages.

Plus shared:
- `_verify-pipeline.yml` — reusable workflow
- `verify-all.yml` — orchestration
- `cleanup-prereleases.yml`
- `bump-versions.yml`

### Differences from shop workflows

| Stage | shop | greeter |
|---|---|---|
| **Commit** | Build, unit test, checkstyle/lint, SonarCloud, Docker build+push | Same |
| **Acceptance** | Deploy app + DB + externals, run system tests (stub + real) | Same (Postgres + Clock + Scorer externals) |
| **Acceptance-legacy** | Same as acceptance, hourly schedule | Same |
| **QA** | Manual deployment + signoff | Same |
| **Prod** | Production deployment | Same |

---

## gh-optivem Changes

### 1. Add `--base` flag

**File:** `internal/config/config.go`

Add to Config struct:
```go
Base string // "shop" or "greeter"
```

Add flag parsing:
```go
base := flag.String("base", "shop", "Template base: shop or greeter")
```

Add validation:
```go
if *base != "shop" && *base != "greeter" {
    log.FatalExit("--base must be 'shop' or 'greeter'")
}
```

### 2. Dynamic template cloning

**File:** `internal/config/config.go`

Rename `cloneShop()` → `cloneTemplate(base string)`:
```go
func cloneTemplate(base string) (string, error) {
    dir, err := os.MkdirTemp("", base+"-")
    if err != nil {
        return "", fmt.Errorf("cannot create temp dir: %w", err)
    }
    repo := "optivem/" + base
    cmd := exec.Command("gh", "repo", "clone", repo, dir, "--", "--depth=1")
    out, err := cmd.CombinedOutput()
    if err != nil {
        os.RemoveAll(dir)
        return "", fmt.Errorf("gh repo clone failed: %s\n%s", err, string(out))
    }
    log.OKf("Cloned %s to %s", repo, dir)
    return dir, nil
}
```

### 3. Base-aware system name defaults

**File:** `internal/config/config.go`

The system name old/new mappings depend on the base:
- shop: `SysNamePascalOld = "Shop"`, `SysNameCamelOld = "shop"`, etc.
- greeter: `SysNamePascalOld = "Greeter"`, `SysNameCamelOld = "greeter"`, etc.

```go
switch cfg.Base {
case "shop":
    cfg.SysNamePascalOld = "Shop"
    cfg.SysNameCamelOld = "shop"
    cfg.SysNameKebabOld = "shop"
    cfg.SysNameLowerOld = "shop"
case "greeter":
    cfg.SysNamePascalOld = "Greeter"
    cfg.SysNameCamelOld = "greeter"
    cfg.SysNameKebabOld = "greeter"
    cfg.SysNameLowerOld = "greeter"
}
```

### 4. No changes needed to apply_template.go

The template application logic is architecture-based (monolith/multitier × monorepo/multirepo), not domain-based. Since greeter follows the exact same directory structure as shop, the existing `applyMonolithMonorepo`, `applyMultitierMonorepo`, etc. functions work as-is. The `ShopPath` just points to a different cloned directory.

### 5. Replacement rules

**File:** `internal/steps/replacements.go`

The replacement rules reference `"shop"` in SonarCloud keys and a few other places. These already get replaced by the repo name, so they should work if the greeter repo follows the same naming convention. Verify during integration testing.

### 6. Reserved words

**File:** `internal/config/config.go`

Add `"greeter"` and `"scorer"` to `isScaffoldReserved()` to prevent users from choosing system names that collide with template infrastructure names.

---

## Execution Plan

### Phase 1: Create repo and backend variants

| Step | Task | Details |
|---|---|---|
| 1.1 | Create `optivem/greeter` GitHub repo | Public, MIT license |
| 1.2 | Set up root structure | `system/`, `system-test/`, `.github/workflows/`, `docs/`, `VERSION`, `README.md` |
| 1.3 | Create Java monolith backend | `system/monolith/java/` — Spring Boot + Thymeleaf |
| 1.4 | Create .NET monolith backend | `system/monolith/dotnet/` — ASP.NET Core + Razor |
| 1.5 | Create TypeScript monolith backend | `system/monolith/typescript/` — Next.js |
| 1.6 | Create Java multitier backend | `system/multitier/backend-java/` — Spring Boot API |
| 1.7 | Create .NET multitier backend | `system/multitier/backend-dotnet/` — ASP.NET Core API |
| 1.8 | Create TypeScript multitier backend | `system/multitier/backend-typescript/` — NestJS API |

### Phase 2: Frontend and externals

| Step | Task | Details |
|---|---|---|
| 2.1 | Create React frontend | `system/multitier/frontend-react/` — single page |
| 2.2 | Create external real simulator | `system/external-real-sim/` — Node.js with Clock + Quote |
| 2.3 | Create external stubs | `system/external-stub/` — WireMock mappings |

### Phase 3: System tests

| Step | Task | Details |
|---|---|---|
| 3.1 | Create Java system test harness | `system-test/java/` — full DSL layers, 2 test scenarios |
| 3.2 | Create .NET system test harness | `system-test/dotnet/` — full DSL layers, 2 test scenarios |
| 3.3 | Create TypeScript system test harness | `system-test/typescript/` — full DSL layers, 2 test scenarios |
| 3.4 | Create Docker Compose files | 8 variants per language (local/pipeline × monolith/multitier × stub/real) |
| 3.5 | Wire per-language test runner | `gh optivem test system` integration per language |

### Phase 4: CI/CD workflows

| Step | Task | Details |
|---|---|---|
| 4.1 | Create commit-stage workflows | 6 variants (monolith + multitier backend/frontend × 3 languages) |
| 4.2 | Create acceptance-stage workflows | Per arch-lang combination |
| 4.3 | Create acceptance-stage-legacy workflows | Per arch-lang combination |
| 4.4 | Create qa-stage + qa-signoff workflows | Per arch-lang combination |
| 4.5 | Create prod-stage workflows | Per arch-lang combination |
| 4.6 | Create cloud-run variant workflows | For applicable stages |
| 4.7 | Create verify-all.yml | Orchestration workflow |
| 4.8 | Create _verify-pipeline.yml | Reusable workflow |
| 4.9 | Create cleanup-prereleases.yml | Pre-release cleanup |

### Phase 5: Documentation and config

| Step | Task | Details |
|---|---|---|
| 5.1 | Create docs/ | Architecture overview, use case docs |
| 5.2 | Create README.md | Badges, structure, getting started |
| 5.3 | Create SonarCloud projects | One per system variant |
| 5.4 | Set up GitHub environments | acceptance, qa, production |
| 5.5 | Set up GitHub secrets/variables | DockerHub, GHCR, SonarCloud tokens |

### Phase 6: Update gh-optivem

| Step | Task | Details |
|---|---|---|
| 6.1 | Add `--base` flag to config.go | Default: "shop", options: "shop", "greeter" |
| 6.2 | Rename cloneShop → cloneTemplate | Dynamic repo cloning based on --base |
| 6.3 | Add base-aware system name defaults | "Shop" for shop, "Greeter" for greeter |
| 6.4 | Add reserved words | "greeter", "quote" |
| 6.5 | Update README and docs | Document --base flag |
| 6.6 | Rebuild Go binary | `go build ./...` |

### Phase 7: Integration testing

| Step | Task | Details |
|---|---|---|
| 7.1 | Test `gh optivem init --base greeter --arch monolith --lang java` | Verify scaffolding works |
| 7.2 | Test all arch × lang × repo-strategy combinations | Verify replacements are correct |
| 7.3 | Verify CI pipelines run green | All stages pass for greeter repo itself |
| 7.4 | Verify scaffolded project CI passes | Commit + acceptance stages green |

### Phase 8: Archived repo cleanup (optional)

| Step | Task | Details |
|---|---|---|
| 8.1 | Decide whether to rename archived `greeter-*` repos | e.g. to `atdd-template-*` |
| 8.2 | Rename if desired | `gh repo rename` on each archived repo |

---

## Open Questions

1. **Cloud-run deploy variant:** Should greeter support `--deploy cloud-run` from day one, or start with Docker only?
   - **Recommended:** Docker only initially. Add cloud-run later if needed.
Agree

2. **Monolith frontend pages:** Shop has 5 web pages. Greeter has 1. Should the monolith variants even have SSR pages, or just serve the API?
   - **Recommended:** Include one simple SSR page (same greeting form) to match the pattern. Students should see how Thymeleaf/Razor/Next.js SSR works.
SSR

3. **Scorer algorithm:** The real-sim Scorer needs an opaque algorithm. Suggested: `sum(charCodes) % 100`.
   - **Recommended:** Single hidden algorithm, not documented in README. Teaches tests to assert on shape, not values.

4. **Naming in scaffolded projects:** When someone runs `gh optivem init --base greeter --system-name "Task Manager"`, the replacement system changes `Greeter` → `TaskManager`, `greeter` → `taskManager`, etc. Verify this works cleanly with no leftover `Greeter` strings.
   - **Action:** Test during Phase 7.
AGREE