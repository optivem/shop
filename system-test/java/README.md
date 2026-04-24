# System Test (Java)

## Prerequisites

- PowerShell 7+
- Docker Desktop (running)
- JDK 21+

## Architectures

Shop ships two architectures side-by-side. Each has its own compose files and
usage examples:

- [monolith/README.md](monolith/README.md) — single-service system
- [multitier/README.md](multitier/README.md) — frontend + backend + external simulators

The entry-point script `Run-SystemTests.ps1` accepts `-Architecture monolith|multitier`
and dot-sources the selected architecture's configuration from its subdirectory.

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
