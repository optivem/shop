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

Step 6: Steps 1–5 are committed. Step 5 is green: compile-all 7/7, and `--sample` 11/11 for monolith java/dotnet/typescript and multitier typescript. The TS blocker was a stale shared docker image, fixed in 724b989d. Push to main, dispatch `meta-prerelease-stage.yml`, and confirm it's green with no `does not support chromium on ubuntu26.04` in its logs.

## Steps

- [ ] Step 6 — **Verify in CI:** commit and push. Dispatch `meta-prerelease-stage.yml` with `gh workflow run meta-prerelease-stage.yml`, then confirm it goes green and that no logs contain `does not support chromium on ubuntu26.04`.

## Decisions (resolved before execution)

- Java goes to 1.64.0 with no fallback to 1.61.x. Any API breakage gets fixed in Step 3.
- Pinning `runs-on: ubuntu-24.04` was rejected. It is not used as a stopgap.
- Step 6 is pre-approved: push to main and dispatch `meta-prerelease-stage.yml` without asking.
