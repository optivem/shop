# System Test (Java)

## Prerequisites

- PowerShell 7+
- Docker Desktop (running)

## Running Tests

Run all latest test suites (multitier):

```powershell
./Run-SystemTests.ps1 -Architecture multitier
```

Run all latest test suites (monolith):

```powershell
./Run-SystemTests.ps1 -Architecture monolith
```

Run legacy test suites:

```powershell
./Run-SystemTests.ps1 -Architecture multitier -Legacy
./Run-SystemTests.ps1 -Architecture monolith -Legacy
```

Run a specific suite by ID:

```powershell
./Run-SystemTests.ps1 -Architecture multitier -Suite acceptance-api
```

Rebuild containers before running:

```powershell
./Run-SystemTests.ps1 -Architecture multitier -Rebuild
```

## Available Suite IDs

| ID | Description |
|----|-------------|
| `smoke-stub` | Smoke tests (stub) |
| `smoke-real` | Smoke tests (real) |
| `acceptance-api` | Acceptance tests - API channel |
| `acceptance-ui` | Acceptance tests - UI channel |
| `acceptance-isolated-api` | Isolated acceptance tests - API channel |
| `acceptance-isolated-ui` | Isolated acceptance tests - UI channel |
| `contract-stub` | Contract tests (stub) |
| `contract-stub-isolated` | Isolated contract tests (stub) |
| `contract-real` | Contract tests (real) |
| `e2e-api` | E2E tests (real) - API channel |
| `e2e-ui` | E2E tests (real) - UI channel |
