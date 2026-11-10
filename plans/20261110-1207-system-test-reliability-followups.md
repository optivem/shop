# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** Each idea below has a decision (do, defer or drop). The accepted ones are done, and the pipeline keeps enough evidence that the next intermittent failure can be diagnosed from one run.

## ▶ Next executable step (resume here)

Review and decision first: the user goes through the ideas below and marks each one **do**, **defer** or **drop** (run `/refine-plan plans/20261110-1207-system-test-reliability-followups.md` to go through them one at a time with recommendations). Then `/execute-plan` this file.

Don't run it alongside the storefront meta-plan's (`plans/20261107-1705-meta-storefront-specs-build.md`) Steps 5–7 if an idea touches `system-test/**` and a port subagent is running system tests at the same time: they share docker ports and the test kit.

## Background: the .NET "does not exist" failures (found 2026-11-10, root cause fixed)

- **Symptom:** from `9da17ffc` onwards, the cloud .NET acceptance stages failed on every attempt (4 of 4). Several API tests per run failed with `Product does not exist for SKU: DEFAULT-SKU-…` or `Country does not exist: UK-…`. Java and TypeScript passed, and so did the .NET docker-compose stages.
- **Root cause:** `.github/workflows/_acceptance-stage.yml` deployed the external-systems Cloud Run service (WireMock stubs, json-server simulator) with `--max-instances 2`. Both keep their state in memory, separately on each instance. Under parallel test load Cloud Run scaled out, so a stub a test registered on one instance was missing when the backend's lookup landed on the other. `f1ca8b7d` added many .NET tests, which pushed the load over the scale-out threshold.
- **Fix:** capped at `--max-instances 1` (commit `c068ab47`, 2026-11-10), following the precedent of the monolith deploy, which is already capped at 1 for its in-memory session state. Cloud Run instance counts were not checked directly, because local `gcloud` is broken (see #11).
- **Ruled out on the way:** shared-profile leaks (the backend takes the country from the request, `system/monolith/dotnet/Core/Services/OrderService.cs:105`), DSL ordering, cross-test stub deletion, static stub files, and WireMock itself.
- **Lesson:** a failure that shows only in the cloud stage and never in docker-compose points at the cloud infrastructure. #1 (keep evidence) would have shown the request landing on a stub server with no matching mapping.

## Ideas to decide

Each idea has a recommendation. Mark it **do / defer / drop**.

### 1. Keep evidence when an acceptance stage fails

On failure, have the acceptance-stage workflows upload as artifacts the stub server's request journal (`/__admin/requests`), its current mappings (`/__admin/mappings`), and the stub and system container logs (`docker compose logs`). Today a failed run has no artifacts, which is why the .NET failure couldn't be diagnosed.
- **Recommended: do, first.** The .NET failure was found without it, but only after two long investigations. Evidence on failure helps every future red run in every language.
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
- **Recommended: do, after #1.** It brings the languages to parity and turns a missing stub into a clear error at registration. Note: it would **not** have caught the instance split, because the read-back can land on the same instance as the registration.

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

### 9. Cap the deploy-stage simulators at one instance too

`.github/workflows/_deploy-stage.yml:270` deploys the simulators (json-server, with products held in memory) with `--max-instances 2`. Any QA or prod-stage test that creates ERP products has the same split-state risk that broke the .NET acceptance stage.
- **Recommended: do.** It is the same one-line fix with the same reasoning, at no added cost (`min-instances 0`).

### 10. Make `JsonWireMockClient._stubIds` thread-safe (.NET)

`system-test/dotnet/Driver.Adapter/Shared/Client/WireMock/JsonWireMockClient.cs:16` is a plain `List<string>`. It is safe today because each test registers stubs one after another, but it would break if a driver ever registered stubs concurrently.
- **Recommended: drop**, unless concurrent registration is planned. Today the risk is only theoretical.

### 11. Fix local `gcloud`

Local `gcloud` fails because it doesn't support Python 3.13. It needs `CLOUDSDK_PYTHON` pointing at Python 3.8–3.12. That blocked checking the Cloud Run instance counts directly.
- **Recommended: do, once per machine.** Set `CLOUDSDK_PYTHON` in your shell profile on both machines. Note it in `CONTRIBUTING.md` if other contributors use `gcloud`.

### 12. Why did multitier Java and TypeScript pass with the broken anonymous view-order URL?

After `28b1aaf9` (React per-route auth), the anonymous "view order" UI check opened `/order-details?orderNumber=…`, which matches no React route, so no login redirect happened. .NET timed out on it (run 42816408496). The test kit was fixed in all three languages on 2026-11-10. But the multitier Java (42805210051) and TypeScript (42803565635) acceptance stages **passed** on commits that already contained `28b1aaf9`. Either they ran an older frontend image, or the UI case didn't run there.
- **Recommended: do, soon.** If the cloud stage can run a stale frontend image, every UI result there is suspect. It is the CI twin of the local stale-image trap (`gh-optivem/plans/20261110-1115-system-start-stale-images.md`). Check which frontend image digest each run deployed, and whether `AnonymousShouldNotBeAbleToViewOrder(UI)` appears in their test results.

### 13. Anonymous cancel and deliver order: language parity

The TypeScript driver also used the order-details URL for anonymous cancel and deliver order, so it was fixed there. .NET and Java weren't checked for those two paths.
- **Recommended: do, with #12.** It's a quick read of the three drivers to confirm the anonymous cancel and deliver checks open a URL React actually routes.

### 14. `npm audit` vulnerabilities in the Next.js monolith

`npm ci` in `system/monolith/typescript` reports `npm audit` vulnerabilities. They are pre-existing, found during the Next.js port on 2026-11-10.
- **Recommended: do, as a triage.** Run `npm audit` in every Node project (the monolith, frontend-react, the multitier TS backend, system-test/typescript, external-systems). Fix the high and critical findings that have a non-breaking upgrade, and defer the rest with a note.

### 15. `next start` doesn't work with `output: "standalone"` locally

With `output: "standalone"`, `next start` warns that it doesn't work, and the standalone server serves no `/_next/static` unless that folder is copied in. Docker is fine, because the Dockerfile copies it. It only affects running the monolith outside Docker.
- **Recommended: defer.** No documented workflow runs it outside Docker. If one appears, add an npm script that copies the static folder or runs `node .next/standalone/server.js`.

### 16. `system-test run --sample` output lacks per-suite counts for some suites

In TypeScript runs, only some suites print a JSON summary (the `expected` and `unexpected` counts). The acceptance suites print a Playwright list instead, so there is no uniform pass/fail line per suite to read off. This is a gh-optivem change.
- **Recommended: do, in gh-optivem.** Have `system-test run` print one summary line per suite (`<suite>: passed N, failed M, skipped K`) for every language, and a final table. That makes local gates and subagent reports reliable to read.

