# System Test (C#/.NET)

## Prerequisites

- gh CLI with the optivem extension: `gh extension install optivem/gh-optivem`
- Docker Desktop (running)
- .NET SDK 8+
- PowerShell 7+ (used by the per-suite Playwright browser install: `pwsh playwright.ps1 install`)

## Running Tests

All commands are run from the repo root. Point `GH_OPTIVEM_CONFIG` at the variant yaml at the repo root once per shell, then run the verbs without per-flag overrides:

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-monolith-dotnet.yaml"
```

Bring up the system stack (real + stub) for the chosen architecture:

```bash
gh optivem system start
```

Prepare the test harness (dotnet clean + build of test sources):

```bash
gh optivem system-test setup
```

Run all latest test suites:

```bash
gh optivem system-test run
```

Run legacy test suites — switch the env var first, then re-run setup (legacy has its own setupCommands block):

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-monolith-dotnet-legacy.yaml"
gh optivem system-test setup
gh optivem system-test run
```

Run only sample tests (one per suite, fast smoke):

```bash
gh optivem system-test run --sample
```

Run a specific suite by ID:

```bash
gh optivem system-test run --suite acceptance-parallel-api
```

Rebuild container images before bringing the system up:

```bash
gh optivem system build
```

Stop the system when done:

```bash
gh optivem system stop
```

Substitute `gh-optivem-multitier-dotnet.yaml` for the multitier architecture.

## Available Suite IDs

| ID | Description |
|----|-------------|
| `smoke-stub` | Smoke tests (stub) |
| `smoke-real` | Smoke tests (real) |
| `acceptance-parallel-api` | Acceptance tests (parallel) - API channel |
| `acceptance-parallel-ui` | Acceptance tests (parallel) - UI channel |
| `acceptance-isolated-api` | Acceptance tests (isolated) - API channel |
| `acceptance-isolated-ui` | Acceptance tests (isolated) - UI channel |
| `contract-stub` | Contract tests (stub) |
| `contract-stub-isolated` | Isolated contract tests (stub) |
| `contract-real` | Contract tests (real) |
| `e2e-api` | E2E tests (real) - API channel |
| `e2e-ui` | E2E tests (real) - UI channel |

You can also pass a **group alias** to `--suite`: `acceptance` runs every acceptance partition, while `acceptance-api` / `acceptance-ui` each run that channel's parallel + isolated partitions together.

## Authentication

The API client acquires Keycloak access tokens (password grant, client `shop-system-test`, realm `shop`) only when a Keycloak base URL is configured; otherwise no token is requested and no `Authorization` header is sent, so the suites still run against systems without auth.

Enable it with `KEYCLOAK_URL_REAL` / `KEYCLOAK_URL_STUB` (per external-system mode) or `KEYCLOAK_URL` (fallback), e.g. `KEYCLOAK_URL_REAL=http://localhost:8291` and `KEYCLOAK_URL_STUB=http://localhost:8292` for the local .NET multitier stack. The identity scenarios (marked `[RequiresKeycloakTheory]`) are skipped when no Keycloak URL is set.

By default, admin-only operations (deliver order, publish/browse coupons, `/api/admin/**`) use `admin1` and everything else uses `customer1`. A test can override this with `apiClient.As(ApiIdentity.Anonymous | Customer | Admin)`.

The UI driver logs in through the Keycloak login form as `customer1` by default and switches to a fresh browser session as `admin1` for admin-only operations (deliver order, publish and browse coupons).
