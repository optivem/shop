# System Test (Java)

## Prerequisites

- gh CLI with the optivem extension: `gh extension install optivem/gh-optivem`
- Docker Desktop (running)
- JDK 21+

## Running Tests

All commands are run from the repo root.

Bring up the system stack (real + stub) for the chosen architecture:

```bash
gh optivem run system --system docker/java/monolith/system.json
```

Run all latest test suites:

```bash
gh optivem test system --system docker/java/monolith/system.json --tests system-test/java/tests-latest.json
```

Run legacy test suites:

```bash
gh optivem test system --system docker/java/monolith/system.json --tests system-test/java/tests-legacy.json
```

Run only sample tests (one per suite, fast smoke):

```bash
gh optivem test system --system docker/java/monolith/system.json --tests system-test/java/tests-latest.json --sample
```

Run a specific suite by ID:

```bash
gh optivem test system --system docker/java/monolith/system.json --tests system-test/java/tests-latest.json --suite acceptance-api
```

Rebuild container images before bringing the system up:

```bash
gh optivem build system --system docker/java/monolith/system.json
```

Stop the system when done:

```bash
gh optivem stop system --system docker/java/monolith/system.json
```

Substitute `docker/java/multitier/system.json` for the multitier architecture.

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
