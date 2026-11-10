# 2026-11-10 12:07 UTC — System-test reliability follow-ups

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-10T21:02:26Z`

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

All 27 ideas are decided (see "Resolved decisions"). Start with **#1, keep evidence on failure**: add one shared on-failure step to `.github/workflows/_acceptance-stage-*.yml`, checking the composite actions in the sibling `optivem/actions` repo first. It should upload `/__admin/requests`, `/__admin/mappings` and `docker compose logs` as artifacts. Then #2 + #3, then the rest of the Do list in order. #11 is user-side, and #16 becomes a gh-optivem plan.

Don't run it alongside the storefront meta-plan's (`plans/20261107-1705-meta-storefront-specs-build.md`) Steps 5–7 if an idea touches `system-test/**` and a port subagent is running system tests at the same time: they share docker ports and the test kit.

## Background: the .NET "does not exist" failures (found 2026-11-10, root cause fixed)

- **Symptom:** from `9da17ffc` onwards, the cloud .NET acceptance stages failed on every attempt (4 of 4). Several API tests per run failed with `Product does not exist for SKU: DEFAULT-SKU-…` or `Country does not exist: UK-…`. Java and TypeScript passed, and so did the .NET docker-compose stages.
- **Root cause:** `.github/workflows/_acceptance-stage.yml` deployed the external-systems Cloud Run service (WireMock stubs, json-server simulator) with `--max-instances 2`. Both keep their state in memory, separately on each instance. Under parallel test load Cloud Run scaled out, so a stub a test registered on one instance was missing when the backend's lookup landed on the other. `f1ca8b7d` added many .NET tests, which pushed the load over the scale-out threshold.
- **Fix:** capped at `--max-instances 1` (commit `c068ab47`, 2026-11-10), following the precedent of the monolith deploy, which is already capped at 1 for its in-memory session state. Cloud Run instance counts were not checked directly, because local `gcloud` is broken (see #11).
- **Ruled out on the way:** shared-profile leaks (the backend takes the country from the request, `system/monolith/dotnet/Core/Services/OrderService.cs:105`), DSL ordering, cross-test stub deletion, static stub files, and WireMock itself.
- **Lesson:** a failure that shows only in the cloud stage and never in docker-compose points at the cloud infrastructure. #1 (keep evidence) would have shown the request landing on a stub server with no matching mapping.

## Resolved decisions

Decided by the user on 2026-11-10: #1–#3 one at a time, then "I automatically approve of all of your suggestions" for #4–#27. Each one takes the recommendation written in its idea below.

**Do (21):**
- **#1 keep failure evidence:** first.
- **#2 .NET tax-stub leak.**
- **#3 Java/TS leak check:** with #2.
- **#4 stub read-back (.NET, TS):** after #1.
- **#6 local .NET restore:** a small check. If it's a setup gap, raise a gh-optivem plan.
- **#7 Windows parallel runs:** option (b), retry Keycloak token acquisition, then measure again.
- **#8a `DefaultGivensTest` null channel:** fix the test.
- **#8b problem+json content type:** fix .NET, and check the other five backends.
- **#9 deploy-stage simulators:** cap at `--max-instances 1`.
- **#11 local gcloud:** set `CLOUDSDK_PYTHON` on both machines (user-side).
- **#12 stale image, or the UI case missing from the cloud stage:** soon.
- **#13 anonymous cancel/deliver parity:** with #12.
- **#14 `npm audit` triage:** across every Node project.
- **#16 per-suite summary line:** in gh-optivem, as its own plan there.
- **#18 `.editorconfig` LF for C#.**
- **#19 `ShopWebApplicationFactory`:** ignore `TransactionIgnoredWarning`.
- **#22 document the low-memory workaround:** in CONTRIBUTING.md.
- **#23 `test-all.sh` stops stacks:** after each language.
- **#24 `test-all.sh --restart` passthrough.**
- **#25 `test-all.sh` EXIT trap:** with #23.
- **#27 meta dispatch matches the child run without `head_sha`.**

**Defer:** #5 (stub reset at suite start), #15 (`next start` standalone), #17 (split `SecurityConfigTest`), #20 (non-UTC CI timezone), #21 (HTML logger control character), #26 (RC tag push; revisit only if it recurs).

**Drop:** #8c (covered by `test-all.sh` legacy runs), #10 (`_stubIds` thread safety; theoretical).

## Ideas to decide

Each idea has a recommendation, and every one is decided (see "Resolved decisions").

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

### 17. `SecurityConfigTest` (Java monolith) is at the checkstyle file-length limit

`system/monolith/java` `SecurityConfigTest` is 496 lines, against checkstyle's 500-line limit, so the Thymeleaf port had to put new security tests in `StorefrontPagesTest`.
- **Recommended: defer.** Splitting it by area (public routes, customer routes, admin routes) is a refactor with no behaviour change. Do it the next time someone has to add a security test.

### 18. `dotnet format --verify-no-changes` flags an untouched file on Windows

`system/monolith/dotnet` `Api/Security/AuthenticationExtensions.cs` lines 62–68 fail the whitespace rule on Windows (it wants CRLF before the chained `.AddPolicyScheme` calls). CI runs on Linux. Found in the Razor port, 2026-11-10.
- **Recommended: do, small.** Pin `end_of_line = lf` in `.editorconfig` for C# files, so local and CI lint agree on Windows and Linux.

### 19. `ShopWebApplicationFactory` (.NET monolith tests) fails any HTTP test that places an order

The in-memory database raises `TransactionIgnoredWarning` as an error when `OrderService` opens a transaction. The Razor port's page tests work around it locally.
- **Recommended: do.** Configure the factory to ignore that warning (`ConfigureWarnings(w => w.Ignore(InMemoryEventId.TransactionIgnoredWarning))`) and remove the workaround.

### 20. Local runs depend on the machine's timezone

The .NET admin-coupons bug (fixed in storefront Step 6) was only caught because this machine is set to CET, while CI runners are UTC. Other timezone bugs would also pass CI unnoticed.
- **Recommended: defer, but consider.** One option is to run one CI UI job with a non-UTC browser timezone (Playwright `timezoneId`, e.g. `Europe/Berlin`), so timezone bugs show up in CI.

### 21. .NET test runs print an HTML logger error

`dotnet test` prints `Html Logger Error : '', hexadecimal value 0x1A, is an invalid character.` The run still passes, but the HTML report may be incomplete. Something in the test output (a log line or test name) contains a control character.
- **Recommended: defer.** It is cosmetic. Look into it if anyone relies on the HTML report.

### 26. Cloud acceptance stage failed to push the RC tag

`meta-prerelease-stage` run 42660796447 (2026-11-09 17:03, `ac04be12`) failed all six pipelines in `verify / cloud-acceptance-stage` with `Failed to push tag monolith-java-v1.0.237-rc.3363 (remote: , local: ac04be12…)`. The tests weren't the cause. The next runs on 2026-11-10 pushed tags for monolith without a problem, so it looks transient (a race between parallel tag pushes, or a GitHub hiccup), but the empty `remote:` field hides the real error.
- **Recommended: investigate if it recurs.** Make the tag-push step print git's stderr and retry once on a non-fast-forward or a network error.

### 27. A push to `main` during `meta-prerelease-stage` fails the run
In run 42885354673 (2026-11-10, `3f4840cc`), a plan-only commit was pushed to `main` while the run was starting. The `multitier-frontend-react-commit-stage.yml` it dispatched ran on the new head (`e8e471a1`, run 42885508265, green), and the parent failed with `Failed to locate dispatched run … (sha=3f4840cc …) within 120s`. Because of that, the pipeline, cross-language and tag jobs were skipped. Any push during the run's dispatch window, even a docs-only one, wastes a ~2.5 h run.
- **Recommended: do.** Make the dispatch-and-wait step find the child run by a unique run name or correlation input, not by `head_sha`. Or pin the dispatch to the resolved sha. Until then, don't push to `main` while a meta run is dispatching.
