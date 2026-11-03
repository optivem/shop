# 2026-11-03 10:06:12 UTC — Upgrade Playwright for Ubuntu 26.04 runners

## TL;DR

**Why:** `ubuntu-latest` moved to Ubuntu 26.04 (actions/runner-images#14748). Playwright only supports `ubuntu26.04` from 1.61 onward. Our pinned versions are older: TS 1.59.1, .NET 1.57.0, Java 1.44.0. As a result, every .NET and TypeScript browser install fails with `Playwright does not support chromium on ubuntu26.04-x64`, and `meta-prerelease-stage.yml` has been red since 2026-11-02 10:33 (e.g. run 41581618722).
**End result:** All three system-test stacks run Playwright 1.64.x. Browser installs succeed on Ubuntu 26.04 runners, and `meta-prerelease-stage.yml` is green again.

## Outcomes

- TypeScript, .NET and Java system tests install chromium on `ubuntu-26.04` runners with no "does not support chromium" error and no "Host validation warning".
- Playwright versions are aligned across languages: npm and NuGet at 1.64.1, Maven at 1.64.0, which is the latest published there.
- Java no longer depends on a restored browser cache and 1.44's generic fallback to pass. Today that combination is a hidden copy of the same bug.
- `meta-prerelease-stage.yml` passes end to end. That covers the local, pipeline, drift and cross-lang jobs for every language.

## ▶ Next executable step (resume here)

Step 1: in `system-test/typescript/package.json:36`, change `"@playwright/test": "^1.55.0"` to `"^1.64.1"`. Then run `npm install` in `system-test/typescript` to regenerate `package-lock.json`, and confirm that `node_modules/playwright-core` in the lock is ≥ 1.61, ideally 1.64.1. Gate: `npx tsc --noEmit` passes in `system-test/typescript`. This unblocks Step 2.

## Steps

- [ ] Step 1 — **TypeScript:** in `system-test/typescript/package.json:36`, change `@playwright/test` to `^1.64.1`. Regenerate `package-lock.json` with `npm install`, verify that the lock resolves playwright-core ≥ 1.61, then run `npx tsc --noEmit`.
- [ ] Step 2 — **.NET:** bump `Microsoft.Playwright` to `1.64.1` in `system-test/dotnet/Common/Common.csproj:11`, `system-test/dotnet/Driver.Adapter/Driver.Adapter.csproj:12` and `system-test/dotnet/SystemTests/SystemTests.csproj:21`. Then run `dotnet build` in `system-test/dotnet`.
- [ ] Step 3 — **Java:** bump `com.microsoft.playwright:playwright` to `1.64.0` at `system-test/java/build.gradle:44`. Update the matching `npx playwright@1.44.0 install chromium` pins at `.github/workflows/monolith-java-acceptance-stage-legacy.yml:218` and `.github/workflows/multitier-java-acceptance-stage-legacy.yml:224` to `@1.64.0`. Those pins carry a "bump together with build.gradle:44" comment. Fix any API breakage from the 1.44 → 1.64 jump, then run `./gradlew build` in `system-test/java`.
- [ ] Step 4 — **Sweep for other pins:** grep the repo, excluding `node_modules`, for any remaining Playwright versions in Dockerfiles, workflows, docs and `mcr.microsoft.com/playwright` image tags, and align them. Playwright cache keys hash `build.gradle` / `package-lock.json`, so they invalidate on their own. Confirm the .NET cache key also changes, or invalidate it.
- [ ] Step 5 — **Verify locally:** run `./compile-all.sh`. Then run `gh optivem system start` → `system-test setup` → `system-test run --sample` → `system stop` for java, dotnet and typescript (`GH_OPTIVEM_CONFIG=gh-optivem-monolith-<language>.yaml`). The devcontainer runs Ubuntu 24.04, so this can't reproduce the 26.04 failure, but it guards against regressions from the upgrade.
- [ ] Step 6 — **Verify in CI:** commit and push. Dispatch `meta-prerelease-stage.yml` with `gh workflow run meta-prerelease-stage.yml`, then confirm it goes green and that no logs contain `does not support chromium on ubuntu26.04`.

## Decisions (resolved before execution)

- Java goes to 1.64.0 with no fallback to 1.61.x. Any API breakage gets fixed in Step 3.
- Pinning `runs-on: ubuntu-24.04` was rejected. It is not used as a stopgap.
- Step 6 is pre-approved: push to main and dispatch `meta-prerelease-stage.yml` without asking.
