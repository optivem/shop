# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

Session 2026-11-10 did #1, #6, #8b, #9, #12, #16 (now `gh-optivem/plans/20261110-2109-system-test-per-suite-counts.md`), #18, #19, #22–#25, #28, and parts of #13/#14; #10 and #8c dropped.

**Waiting on the user:** #27's fix is written but **uncommitted** in `../actions/trigger-and-wait-for-workflow/trigger.sh` (+ README). Committing it auto-pushes and moves the floating `v1` tag for every consumer, and on a moved `main` it now warns and continues instead of failing (the child may test a newer sha than the parent tags). Review, then commit via `/commit`. #11 is user-side.

**Blocked until the storefront `system-test/**` work is committed** (the tree had ~40 uncommitted storefront files there): #2 (await `_tax` dispose in `system-test/dotnet/Dsl.Core/UseCase/UseCaseDsl.cs:135`) is the next unit, then #3 (needs local acceptance runs — ask first), #4, #7 (b), #8a, #13 fix, #14's system-test/typescript `npm audit fix`.

Don't run alongside the storefront meta-plan's (`plans/20261107-1705-meta-storefront-specs-build.md`) Steps 5–7 if an idea touches `system-test/**` and a port subagent is running system tests at the same time: they share docker ports and the test kit.

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

### 7. Make local parallel runs reliable on Windows

6 of the 13 local .NET runs failed for other reasons: 30 s timeouts with "An existing connection was forcibly closed by the remote host", or "Failed to acquire token for admin1" from Keycloak. That looks like Windows Docker networking under parallel load. Until it's fixed, a local run can't confirm a fix, which breaks CLAUDE.md's "reproduce locally first" rule.
- Options:
  - (a) Lower parallelism for local runs only.
  - (b) Retry Keycloak token acquisition a few times in the test kit.
  - (c) Both.
- **Recommended: (b) first.** A token retry is small and contained in the test kit, while lower parallelism slows every local run. Measure again after (b).

### 8. Older pre-existing issues (moved here from the storefront meta-plan)

- **8a.** `system-test/dotnet` `DefaultGivensTest.OrderWithoutDeclaredProductShouldCreateDefaultProductAndTaxRateExactlyOnce` throws a NullReferenceException because it passes a null channel. It fails on older commits too. It is not in the CI filter (`.Latest.AcceptanceTests`), which is why CI doesn't show it. **Recommended: do**, by fixing the test's channel setup. A test that always fails locally and is hidden from CI is worse than no test.

### 11. Fix local `gcloud`

Local `gcloud` fails because it doesn't support Python 3.13. It needs `CLOUDSDK_PYTHON` pointing at Python 3.8–3.12. That blocked checking the Cloud Run instance counts directly.
- **Recommended: do, once per machine.** Set `CLOUDSDK_PYTHON` in your shell profile on both machines. Note it in `CONTRIBUTING.md` if other contributors use `gcloud`.

### 13. Anonymous cancel and deliver order: language parity

Checked 2026-11-10. The URL is fine in .NET and Java: both go through `ViewOrder`'s anonymous branch, which falls back to the React route `/order-details/{n}`. But both call `RunAs(Customer/Admin)` **before** that branch, so an anonymous cancel/deliver first signs a real user in. The TypeScript driver (`my-shop-ui-driver.ts:142-144, 159-161`) checks `anonymous` first, which is the correct shape. No acceptance test covers anonymous cancel or deliver in any language.
- .NET: `system-test/dotnet/Driver.Adapter/Ui/MyShopUiDriver.cs:117` (cancel), `:154` (deliver), anonymous branch `:191-195`.
- Java: `system-test/java/src/main/java/com/mycompany/myshop/testkit/driver/adapter/ui/MyShopUiDriver.java:134` (cancel), `:154` (deliver), anonymous branch `:179-183`.
- **Fix (blocked until the storefront `system-test/**` work is committed):** extract a `LoginRequiredForOrder(orderNumber)` helper from `ViewOrder`'s anonymous branch (mirroring TS `loginRequiredForOrder`), call it at the very top of `CancelOrder` / `DeliverOrder` before `RunAs`, and have `ViewOrder` use it too.

### 14. `npm audit` vulnerabilities in the Node projects

Triaged 2026-11-10: `npm audit fix` (no `--force`) plus an in-range `@nestjs/common`/`@nestjs/testing` 11.2.9 bump took high/critical from 39 to 9 across the monolith, backend-typescript, frontend-react and simulators (lockfiles only). What's left:
- **system-test/typescript (blocked on storefront work):** 1 high (brace-expansion), non-breaking. Run `npm audit fix` there once the storefront `system-test/**` changes are committed.
- **Deferred, needs a breaking major bump:** postcss via next 16 (monolith); `@pact-foundation/pact` 17 (backend-typescript, frontend-react); vitest 5 (frontend-react, critical). backend-typescript brace-expansion <=1.1.20 was not applied by `audit fix` (pinned by its dependent).

### 15. `next start` doesn't work with `output: "standalone"` locally

With `output: "standalone"`, `next start` warns that it doesn't work, and the standalone server serves no `/_next/static` unless that folder is copied in. Docker is fine, because the Dockerfile copies it. It only affects running the monolith outside Docker.
- **Recommended: defer.** No documented workflow runs it outside Docker. If one appears, add an npm script that copies the static folder or runs `node .next/standalone/server.js`.

### 17. `SecurityConfigTest` (Java monolith) is at the checkstyle file-length limit

`system/monolith/java` `SecurityConfigTest` is 496 lines, against checkstyle's 500-line limit, so the Thymeleaf port had to put new security tests in `StorefrontPagesTest`.
- **Recommended: defer.** Splitting it by area (public routes, customer routes, admin routes) is a refactor with no behaviour change. Do it the next time someone has to add a security test.

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
