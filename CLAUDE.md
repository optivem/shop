# MyShop Repo Guidelines

## Git hooks (one-time setup per clone)

After cloning shop, run this once to enable the tracked pre-commit hook:

```bash
git config core.hooksPath .githooks
```

The hook at `.githooks/pre-commit` rejects commits matching the accidental-rehearsal-leak shape (generic "Sync changes" message + touching `system/**` or `system-test/**` + Claude co-author trailer). It exists because of incident `d325a797` (reverted in `7ce32f0e`).

## Documentation: No GitHub Pages

Never enable GitHub Pages on this repo or on repos scaffolded from it. Docs live as plain markdown under `docs/`; link to them from the README using relative paths (e.g. `[Architecture](docs/architecture.md)`). GitHub renders markdown and Mermaid natively on github.com, which is sufficient for the dev audience and avoids the Pages workflow, `id-token: write` permissions, and deploy-time failures.

If you find yourself proposing a `pages.yml` workflow, a `build_type=workflow` Pages API call, or any `*.github.io` URL, stop and reconsider — the answer is always README + `docs/*.md` links.

## Pre-Commit Verification

Before committing any code changes, always verify compilation locally. The default is to run `./compile-all.sh` from the repo root — it compiles every system and system-test project across all three languages and prints a pass/fail summary. Never commit code that does not compile.

If you only touched a single project, you can run that project's compile command directly instead of the full sweep:

- **Java** (monolith/multitier): `./gradlew build` in the project directory
- **TypeScript** (monolith / multitier backend / frontend-react): `npx tsc --noEmit` in the project directory
- **.NET** (monolith/multitier): `dotnet build` in the project directory

Note that `./gradlew build` and `dotnet build` run the unit tests, not just the compile — that test run is the gate, so never substitute a compile-only task for it (a rename can compile cleanly and still leave a stale test assertion behind).

### System Test Verification

After compilation passes, run system tests with `--sample` for each affected language before committing. From the repo root, substituting `<language>` ∈ {java, dotnet, typescript}:

```pwsh
$env:GH_OPTIVEM_CONFIG = "gh-optivem-monolith-<language>.yaml"
gh optivem system start
gh optivem system-test setup
gh optivem system-test run --sample
gh optivem system stop
```

This runs one sample test per suite across all test categories (smoke, acceptance, contract, e2e) to catch regressions without running the full suite. All sample tests must pass before committing.

For full-suite runs across all three languages (latest + legacy), use `./test-all.sh` from the repo root — see [CONTRIBUTING.md](CONTRIBUTING.md#running-system-tests).

## Fixing Failing Workflows

When fixing a failing CI workflow, always follow this sequence:

1. **Reproduce locally first**: Before making any code changes, run `gh optivem system-test run` locally with the appropriate flags to reproduce the failure. Report whether the failure was reproduced or not.
2. **Check all languages for the same issue**: The shop repo has parallel implementations in .NET, Java, and TypeScript. When a test fails in one language, check the equivalent test in the other languages for the same problem or inconsistency. Fix all affected languages, not just the one that failed.
3. **Test locally after fixing**: After applying the fix, run `gh optivem system-test run` again locally to verify the fix works.
4. **Then commit**: Only commit and push after local verification passes.
