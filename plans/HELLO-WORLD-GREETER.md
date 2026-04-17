# Create `optivem/greeter` — Minimal Template Repo

## Context

The `optivem/starter` repo (Shop domain) is a full e-commerce system with orders, coupons, pricing, external integrations (ERP, Clock, Tax), and a complex domain model. It serves as:
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
| **Separate repo vs inside starter** | Separate repo | Avoids doubling starter's ~68 workflow files |
| **Database** | None | Simplicity; no migrations, no connection strings |
| **CRUD** | No — single GET endpoint | Minimal surface area |
| **External systems** | Clock + Quote API | Clock for determinism; Quote API for external I/O pattern |
| **Stubs (WireMock)** | Yes | Required to demonstrate acceptance testing |
| **External simulator** | Yes | Same pattern as starter, two endpoints |
| **Acceptance tests** | Yes, full harness | Template must be complete for real project scaffolding |
| **UI** | Yes, minimal | Text field + button + result display |
| **Frontend framework** | React + Vite (multitier), Thymeleaf/Razor/Next.js (monolith) | Same as starter |

---

## Domain

### Endpoint

```
GET /api/greetings?name=World
```

### Response

```json
{
  "message": "Hello, World!",
  "quote": "Have a great day!",
  "timestamp": "2026-04-17T10:30:00Z"
}
```

### Business logic (GreeterService)

1. Generate message: `"Hello, {name}!"` — local logic
2. Call Quote API: `GET /api/quotes` → `{"quote": "Have a great day!"}` — external I/O
3. Call Clock API: `GET /api/clock` → `{"timestamp": "2026-04-17T10:30:00Z"}` — external I/O (determinism)
4. Return combined response

### UI display

```
[ World          ] [ Greet ]

Hello, World! Have a great day! The current time is 2026-04-17T10:30:00Z.
```

### Architecture layers

```
Controller (API)  →  Service (business logic)  →  External gateways (Clock, Quote)
     ↑                                                      ↓
  DTO (response)                                    External API calls
```

No repository layer. No entities. No database.

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
│   ├── external-real-sim/           # Node.js simulator (Clock + Quote)
│   └── external-stub/               # WireMock mappings (Clock + Quote)
├── system-test/
│   ├── java/                        # JUnit 5 + Playwright
│   ├── dotnet/                      # xUnit + Playwright
│   └── typescript/                  # Jest + Playwright
├── .github/workflows/               # CI/CD pipelines
├── docs/                            # Architecture docs
├── VERSION                          # Semantic version
└── README.md
```

### Comparison with starter

| Component | starter (Shop) | greeter |
|---|---|---|
| **API controllers** | OrderApiController, CouponApiController, HealthController | GreeterApiController, HealthController |
| **Services** | OrderService, CouponService | GreeterService |
| **DTOs** | PlaceOrderRequest/Response, BrowseCouponsResponse, ViewOrderDetailsResponse, PublishCouponRequest | GreetingResponse |
| **Entities** | Order, OrderStatus, Coupon | None |
| **Repositories** | OrderRepository, CouponRepository | None |
| **External gateways** | ErpGateway, ClockGateway, TaxGateway | ClockGateway, QuoteGateway |
| **External DTOs** | GetPromotionResponse, GetTimeResponse, ProductDetailsResponse, TaxDetailsResponse | GetTimeResponse, GetQuoteResponse |
| **Exception handling** | GlobalExceptionHandler, ValidationException, NotExistValidationException | GlobalExceptionHandler |
| **Web controllers (monolith)** | HomeController, ShopController, OrderHistoryController, OrderDetailsController, AdminCouponsController | HomeController |
| **Frontend pages** | Home, NewOrder, OrderHistory, OrderDetails, AdminCoupons | Home (single page) |
| **Docker Compose services** | App + DB + External Sim + WireMock | App + External Sim + WireMock (no DB) |
| **WireMock mappings** | Clock, ERP products, ERP promotions, Tax | Clock, Quote |

---

## Source Files Per Backend Variant

Each backend variant (e.g. `system/monolith/java/`) contains:

| File | Purpose |
|---|---|
| `GreeterApiController` | `GET /api/greetings?name=...` endpoint |
| `HealthController` | `GET /health` endpoint |
| `GreeterService` | Business logic: combine message + quote + timestamp |
| `GreetingResponse` | Response DTO: message, quote, timestamp |
| `ClockGateway` | HTTP client for Clock external API |
| `QuoteGateway` | HTTP client for Quote external API |
| `GetTimeResponse` | DTO for Clock API response |
| `GetQuoteResponse` | DTO for Quote API response |
| `GlobalExceptionHandler` | Error handling |
| `Application` (main class) | Spring Boot / ASP.NET / NestJS entry point |
| `Dockerfile` | Container build |
| `VERSION` | Semantic version |
| Build config | `build.gradle` / `.csproj` / `package.json` |

~12 source files per variant vs ~25+ in starter.

---

## External Systems

### External Real Simulator (`external-real-sim/`)

Node.js Express app with two endpoints:

```
GET /api/clock     → {"timestamp": "<current ISO timestamp>"}
GET /api/quotes    → {"quote": "Have a great day!"}
```

The quote can be a hardcoded value or rotate through a small list — doesn't matter for the template.

### External Stubs (`external-stub/`)

WireMock mappings:

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

**Quote stub:**
```json
{
  "request": { "method": "GET", "url": "/api/quotes" },
  "response": {
    "status": 200,
    "jsonBody": { "quote": "Have a great day!" },
    "headers": { "Content-Type": "application/json" }
  }
}
```

---

## System Tests

### Test Scenarios

**API e2e test:**
```
Given the system is running
  And the Clock returns "2026-01-15T10:30:00Z"
  And the Quote API returns "Have a great day!"
When I call GET /api/greetings?name=World
Then the response status is 200
  And the message is "Hello, World!"
  And the quote is "Have a great day!"
  And the timestamp is "2026-01-15T10:30:00Z"
```

**UI e2e test (Playwright):**
```
Given the system is running
  And the Clock returns "2026-01-15T10:30:00Z"
  And the Quote API returns "Have a great day!"
When I type "World" in the name field
  And I click the Greet button
Then the page displays "Hello, World! Have a great day! The current time is 2026-01-15T10:30:00Z."
```

### Test Harness Structure

Same DSL layers as starter, minimal content:

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
│   └── External.Clock.Client/ # HTTP client for Clock stubs
│   └── External.Quote.Client/ # HTTP client for Quote stubs
├── SystemTests/           # Actual test classes
├── docker-compose.*.yml   # Variants: local/pipeline × stub/real
├── Run-SystemTests.ps1    # Test runner script
└── build config           # build.gradle / .csproj / package.json
```

### Docker Compose Variants

| Variant | Services |
|---|---|
| `local.monolith.stub` | App + WireMock |
| `local.monolith.real` | App + External Sim |
| `local.multitier.stub` | Backend + Frontend + WireMock |
| `local.multitier.real` | Backend + Frontend + External Sim |
| `pipeline.monolith.stub` | App (from GHCR) + WireMock |
| `pipeline.monolith.real` | App (from GHCR) + External Sim |
| `pipeline.multitier.stub` | Backend + Frontend (from GHCR) + WireMock |
| `pipeline.multitier.real` | Backend + Frontend (from GHCR) + External Sim |

No Postgres in any variant.

---

## CI/CD Workflows

Same naming convention as starter. Same stages, simplified content.

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

### Differences from starter workflows

| Stage | starter | greeter |
|---|---|---|
| **Commit** | Build, unit test, checkstyle/lint, SonarCloud, Docker build+push | Same |
| **Acceptance** | Deploy app + DB + externals, run system tests (stub + real) | Deploy app + externals, run system tests (stub + real) — no DB |
| **Acceptance-legacy** | Same as acceptance, hourly schedule | Same |
| **QA** | Manual deployment + signoff | Same |
| **Prod** | Production deployment | Same |

---

## gh-optivem Changes

### 1. Add `--base` flag

**File:** `internal/config/config.go`

Add to Config struct:
```go
Base string // "starter" or "greeter"
```

Add flag parsing:
```go
base := flag.String("base", "starter", "Template base: starter or greeter")
```

Add validation:
```go
if *base != "starter" && *base != "greeter" {
    log.FatalExit("--base must be 'starter' or 'greeter'")
}
```

### 2. Dynamic template cloning

**File:** `internal/config/config.go`

Rename `cloneStarter()` → `cloneTemplate(base string)`:
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
- starter: `SysNamePascalOld = "Shop"`, `SysNameCamelOld = "shop"`, etc.
- greeter: `SysNamePascalOld = "Greeter"`, `SysNameCamelOld = "greeter"`, etc.

```go
switch cfg.Base {
case "starter":
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

The template application logic is architecture-based (monolith/multitier × monorepo/multirepo), not domain-based. Since greeter follows the exact same directory structure as starter, the existing `applyMonolithMonorepo`, `applyMultitierMonorepo`, etc. functions work as-is. The `StarterPath` just points to a different cloned directory.

### 5. Replacement rules

**File:** `internal/steps/replacements.go`

The replacement rules reference `"starter"` in SonarCloud keys and a few other places. These already get replaced by the repo name, so they should work if the greeter repo follows the same naming convention. Verify during integration testing.

### 6. Reserved words

**File:** `internal/config/config.go`

Add `"greeter"` and `"quote"` to `isScaffoldReserved()` to prevent users from choosing system names that collide with template infrastructure names.

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
| 3.5 | Create Run-SystemTests.ps1 | Test runner scripts per language |

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
| 6.1 | Add `--base` flag to config.go | Default: "starter", options: "starter", "greeter" |
| 6.2 | Rename cloneStarter → cloneTemplate | Dynamic repo cloning based on --base |
| 6.3 | Add base-aware system name defaults | "Shop" for starter, "Greeter" for greeter |
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

2. **Monolith frontend pages:** Starter has 5 web pages. Greeter has 1. Should the monolith variants even have SSR pages, or just serve the API?
   - **Recommended:** Include one simple SSR page (same greeting form) to match the pattern. Students should see how Thymeleaf/Razor/Next.js SSR works.
SSR

3. **Quote API response variety:** Should the stub always return the same quote, or have multiple mappings?
   - **Recommended:** Single fixed quote (`"Have a great day!"`). Keeps stubs trivial.
AGREE

4. **Naming in scaffolded projects:** When someone runs `gh optivem init --base greeter --system-name "Task Manager"`, the replacement system changes `Greeter` → `TaskManager`, `greeter` → `taskManager`, etc. Verify this works cleanly with no leftover `Greeter` strings.
   - **Action:** Test during Phase 7.
AGREE