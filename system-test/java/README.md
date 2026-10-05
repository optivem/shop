# System Test (Java)

## Prerequisites

- gh CLI with the optivem extension: `gh extension install optivem/gh-optivem`
- Docker Desktop (running)
- JDK 21+

## Running Tests

All commands are run from the repo root. Point `GH_OPTIVEM_CONFIG` at the variant yaml at the repo root once per shell, then run the verbs without per-flag overrides:

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-monolith-java.yaml"
```

Bring up the system stack (real + stub) for the chosen architecture:

```bash
gh optivem system start
```

Prepare the test harness (gradle compile of test sources):

```bash
gh optivem system-test setup
```

Run all latest test suites:

```bash
gh optivem system-test run
```

Run legacy test suites — switch the env var first, then re-run setup (legacy has its own setupCommands block):

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-monolith-java-legacy.yaml"
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

Substitute `gh-optivem-multitier-java.yaml` for the multitier architecture.

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

Enable it with `KEYCLOAK_URL_REAL` / `KEYCLOAK_URL_STUB` (per external-system mode) or `KEYCLOAK_URL` (fallback), e.g. `KEYCLOAK_URL_REAL=http://localhost:8191` and `KEYCLOAK_URL_STUB=http://localhost:8192` for the local Java multitier stack. The authorization tests (`ApiAuthorizationTest`) are skipped when no Keycloak URL is set.

By default, admin-only operations (deliver order, publish coupon, `/api/admin/**`) use `admin1` and everything else uses `customer1` (`admin1` holds only ADMIN and cannot place orders; `customer2` is a second customer for isolation scenarios). A test can override this with `myShopApiClient.as(ApiIdentity.ANONYMOUS | CUSTOMER | ADMIN)`.
