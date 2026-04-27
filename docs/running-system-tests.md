# Running System Tests

The `gh optivem` CLI extension orchestrates the docker-compose stacks and runs
the test suites. Install it once with:

```bash
gh extension install optivem/gh-optivem
```

## All languages at once — `run-all-system-tests.sh`

From the repo root:

```bash
./run-all-system-tests.sh -a monolith
```

Runs both **latest** and **legacy** suites across all three languages (.NET,
Java, TypeScript) sequentially, prints a per-language / per-phase summary, and
exits non-zero on any failure.

Useful flags:

- `-l dotnet,java` — run only a subset of languages
- `-a multitier` — run against the multitier architecture instead of monolith

This is the preferred entry point for verifying cross-language changes.

## Single language — `gh optivem test system`

From the repo root, substituting `<language>` ∈ {java, dotnet, typescript}:

```bash
# Bring up the docker-compose stacks for the chosen architecture
gh optivem run system --system docker/<language>/monolith/system.json

# Run the latest suites
gh optivem test system --system docker/<language>/monolith/system.json --tests system-test/<language>/tests-latest.json

# Or the legacy suites
gh optivem test system --system docker/<language>/monolith/system.json --tests system-test/<language>/tests-legacy.json

# Or a fast smoke (one sample per suite)
gh optivem test system --system docker/<language>/monolith/system.json --tests system-test/<language>/tests-latest.json --sample

# Stop when done
gh optivem stop system --system docker/<language>/monolith/system.json
```

Use this when iterating on a single language, or for the `--sample`
pre-commit verification described in [CLAUDE.md](../CLAUDE.md).

Substitute `docker/<language>/multitier/system.json` for the multitier architecture.

Do **not** substitute `./gradlew test`, `mvn test`, `dotnet test`, or `npm
test` — these wrappers manage Docker containers and per-suite environment
variables that the raw toolchain does not.
