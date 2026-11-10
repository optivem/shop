# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** Each idea below has a decision (do, defer or drop). The accepted ones are done, and the pipeline keeps enough evidence that the next intermittent failure can be diagnosed from one run.

## ▶ Next executable step (resume here)

Review and decision first: the user goes through the ideas below and marks each one **do**, **defer** or **drop** (run `/refine-plan plans/20261110-1207-system-test-reliability-followups.md` to go through them one at a time with recommendations). Then `/execute-plan` this file.

Don't run it alongside the storefront meta-plan's (`plans/20261107-1705-meta-storefront-specs-build.md`) Steps 5–7 if an idea touches `system-test/**` and a port subagent is running system tests at the same time: they share docker ports and the test kit.

## Background: the intermittent .NET failure (found 2026-11-10)

- **Symptom:** only in CI, and only sometimes, 1–3 of the 119 .NET API acceptance tests fail with `Product does not exist for SKU: DEFAULT-SKU-…`. The ERP stub returns 404 for a SKU the test had just registered successfully. The set of failing tests differs from run to run: run 42803428175 (multitier) failed 3, run 42805123156 (monolith) failed 1. Java and TypeScript pass.
- **First seen** on `9da17ffc`, the first build that includes `f1ca8b7d` (profile, shipping address, `GET /api/orders?customer=`). The earlier `2e1eec96` runs failed only at `publish-tag` (the token problem), not in the tests.
- **Did not reproduce** in 13 local monolith-dotnet runs of the same suite.
- **Ruled out:**
  - DSL ordering: the default product is registered and awaited before the order is placed (`DefaultGivens.EnsureAsync`, `GivenStage.SetupGiven`, `WhenStage.EnsureGiven`).
  - Cross-test deletion: each test removes only its own stub ids, and there is no reset or delete-all anywhere.
  - Static stub files: all exact-path, with no catch-all 404.
  - The backend's ERP lookup: it returns "does not exist" only on a 404, and it is unchanged in the range.
  - WireMock 3.10.0 itself: its add/remove source was read, and a stress test (60 threads, 2,700 cycles) lost no stubs.
  - The compose stub service: identical in the dotnet and java pipeline compose files.
- **Circumstantial:** in 42803428175 the failures came 10–11 s into the run, when most parallel tests are finishing and removing their stubs.

## Ideas to decide

Each idea has a recommendation. Mark it **do / defer / drop**.

### 1. Keep evidence when an acceptance stage fails

On failure, have the acceptance-stage workflows upload as artifacts the stub server's request journal (`/__admin/requests`), its current mappings (`/__admin/mappings`), and the stub and system container logs (`docker compose logs`). Today a failed run has no artifacts, which is why the .NET failure couldn't be diagnosed.
- **Recommended: do, first.** It is the only idea that can find the real cause of the .NET failure, and it helps every future red run in every language.
- Files: `.github/workflows/_acceptance-stage-*.yml` (one shared step if possible; check the shared composite actions in the sibling `optivem/actions` repo first).

### 2. Fix the .NET tax-stub leak

`system-test/dotnet/Dsl.Core/UseCase/UseCaseDsl.cs:135` calls `_tax?.Dispose()` synchronously, while the shop, ERP and clock disposals above and below it are awaited. The tax stubs are never removed, and about 1,100 leftover `US-*` tax stubs were counted on the local stub server after the investigation's runs.
- **Recommended: do.** It is a small, certain bug fix: await an async dispose, as for ERP.
- It can't cause the product 404 (the tax names are unique per test), but a stub server that keeps growing slows every lookup.

### 3. Check Java and TypeScript for the same kind of leak

After a full acceptance run in each language, list the stub server's mappings. Only the static mappings should remain.
- **Recommended: do, together with #2.** The check is cheap, and parity between the languages is the shop's standing rule.

### 4. Read back each stub after registering it (.NET and TypeScript)

Java's `JsonWireMockClient` re-lists the stubs after registering one and fails if the new stub isn't visible. The .NET and TypeScript clients don't.
- **Recommended: do, after #1.** It brings the languages to parity, and if the .NET failure is a registration race it would surface as a clear error instead of a misleading "product does not exist". Not a proven fix, so don't close the .NET failure on it alone.

### 5. Reset the stub server at the start of a suite run

Even with #2 fixed, a long-lived local stack accumulates any stubs a crashed or killed test run leaves behind. Option: reset dynamic mappings at suite start (`POST /__admin/mappings/reset`, which keeps the file-based static mappings), or on `gh optivem system start --restart`.
- **Recommended: defer.** #2 and #3 remove the known leaks. Revisit if leftovers still pile up.
- Note: a reset at suite start must never run while another suite is using the same stub server.

### 6. Make the local .NET setup reliable

`gh optivem system-test setup` for dotnet failed with NETSDK1064 (GraphQL.Analyzers 8.2.1 missing) until `dotnet restore --force` was run.
- **Recommended: do, as a small check.** Find out whether it is a stale local package cache (fix once on this machine) or something setup should handle (a gh-optivem change, so raise a gh-optivem plan).

### 7. Make local parallel runs reliable on Windows

6 of the 13 local .NET runs failed for other reasons: 30 s timeouts with "An existing connection was forcibly closed by the remote host", or "Failed to acquire token for admin1" from Keycloak. That looks like Windows Docker networking under parallel load. Until it's fixed, a local run can't confirm a fix, which breaks CLAUDE.md's "reproduce locally first" rule.
- Options:
  - (a) Lower parallelism for local runs only.
  - (b) Retry Keycloak token acquisition a few times in the test kit.
  - (c) Both.
- **Recommended: (b) first.** A token retry is small and contained in the test kit, while lower parallelism slows every local run. Measure again after (b).

### 8. Older pre-existing issues (moved here from the storefront meta-plan)

- **8a.** `system-test/dotnet` `DefaultGivensTest.OrderWithoutDeclaredProductShouldCreateDefaultProductAndTaxRateExactlyOnce` throws a NullReferenceException because it passes a null channel. It fails on older commits too. It is not in the CI filter (`.Latest.AcceptanceTests`), which is why CI doesn't show it. **Recommended: do**, by fixing the test's channel setup. A test that always fails locally and is hidden from CI is worse than no test.
- **8b.** The .NET monolith's `GlobalExceptionHandler` sends `application/json` instead of `application/problem+json`. **Recommended: do**, and check the other five backends for the same content type.
- **8c.** The storefront Step 1 run never ran the monolith-java **legacy** `--sample`. Its latest sample and contract runs passed, and legacy compiles. **Recommended: drop.** The storefront meta-plan's Step 6 runs `./test-all.sh`, which covers legacy.
