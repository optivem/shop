# MyShop Repo Guidelines

## Documentation: No GitHub Pages

Never enable GitHub Pages on this repo or on repos scaffolded from it. Docs live as plain markdown under `docs/`; link to them from the README using relative paths (e.g. `[Architecture](docs/architecture.md)`). GitHub renders markdown and Mermaid natively on github.com, which is sufficient for the dev audience and avoids the Pages workflow, `id-token: write` permissions, and deploy-time failures.

If you find yourself proposing a `pages.yml` workflow, a `build_type=workflow` Pages API call, or any `*.github.io` URL, stop and reconsider — the answer is always README + `docs/*.md` links.

## Pre-Commit Verification

Before committing any code changes, always verify compilation locally for the affected components:

- **Java** (monolith/multitier): `./gradlew build` in the project directory
- **TypeScript** (monolith): `npx tsc --noEmit` in the project directory
- **TypeScript** (multitier frontend-react): `npx tsc --noEmit` in the project directory
- **TypeScript** (multitier backend-typescript): `npx tsc --noEmit` in the project directory
- **.NET** (monolith/multitier): `dotnet build` in the project directory

Never commit code that does not compile. If multiple components are changed, verify each one before committing.

### System Test Verification

After compilation passes, run system tests with `--sample` for each affected language before committing. From the repo root, substituting `<language>` ∈ {java, dotnet, typescript}:

```bash
gh optivem run system --system docker/<language>/monolith/system.json
gh optivem test system --system docker/<language>/monolith/system.json --tests system-test/<language>/tests-latest.json --sample
gh optivem stop system --system docker/<language>/monolith/system.json
```

This runs one sample test per suite across all test categories (smoke, acceptance, contract, e2e) to catch regressions without running the full suite. All sample tests must pass before committing.

For full-suite runs across all three languages (latest + legacy), use `./test-all.sh` from the repo root — see [docs/running-system-tests.md](docs/running-system-tests.md).

## Fixing Failing Workflows

When fixing a failing CI workflow, always follow this sequence:

1. **Reproduce locally first**: Before making any code changes, run `gh optivem test system` locally with the appropriate flags to reproduce the failure. Report whether the failure was reproduced or not.
2. **Check all languages for the same issue**: The shop repo has parallel implementations in .NET, Java, and TypeScript. When a test fails in one language, check the equivalent test in the other languages for the same problem or inconsistency. Fix all affected languages, not just the one that failed.
3. **Test locally after fixing**: After applying the fix, run `gh optivem test system` again locally to verify the fix works.
4. **Then commit**: Only commit and push after local verification passes.
