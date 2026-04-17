# Shop Repo Guidelines

## Pre-Commit Verification

Before committing any code changes, always verify compilation locally for the affected components:

- **Java** (monolith/multitier): `./gradlew build` in the project directory
- **TypeScript** (monolith): `npx tsc --noEmit` in the project directory
- **TypeScript** (multitier frontend-react): `npx tsc --noEmit` in the project directory
- **TypeScript** (multitier backend-typescript): `npx tsc --noEmit` in the project directory
- **.NET** (monolith/multitier): `dotnet build` in the project directory

Never commit code that does not compile. If multiple components are changed, verify each one before committing.

### System Test Verification

After compilation passes, run system tests with `-Sample` for each affected language before committing. From each language's `system-test/<language>/` directory:

```powershell
./Run-SystemTests.ps1 -Architecture monolith -Sample
```

This runs one sample test per suite across all test categories (smoke, acceptance, contract, e2e) to catch regressions without running the full suite. All sample tests must pass before committing.

## Fixing Failing Workflows

When fixing a failing CI workflow, always follow this sequence:

1. **Reproduce locally first**: Before making any code changes, run `Run-SystemTests.ps1` locally with the appropriate flags to reproduce the failure. Report whether the failure was reproduced or not.
2. **Check all languages for the same issue**: The shop repo has parallel implementations in .NET, Java, and TypeScript. When a test fails in one language, check the equivalent test in the other languages for the same problem or inconsistency. Fix all affected languages, not just the one that failed.
3. **Test locally after fixing**: After applying the fix, run `Run-SystemTests.ps1` again locally to verify the fix works.
4. **Then commit**: Only commit and push after local verification passes.
