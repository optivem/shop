# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

Session 2026-11-10 did #1, #2, #3 (full latest acceptance runs per language on monolith stacks: .NET 363/363, Java 231 passed/0 failed/74 skipped, TS 250/250; afterwards each stub server held only its 18 static mappings, 0 dynamic, so no Java/TS leak), #4, #6, #7 (b) (token retry in all three languages; 0 token failures and 0 retries across the acceptance and sample runs), #8a, #8b, #9, #12, #13, #14 (system-test/typescript high 1 → 0; still deferred, needing a breaking major bump: postcss via next 16 (monolith), `@pact-foundation/pact` 17 (backend-typescript, frontend-react), vitest 5 (frontend-react, critical), and backend-typescript brace-expansion <=1.1.20, pinned by its dependent), #16 (now `gh-optivem/plans/20261110-2109-system-test-per-suite-counts.md`), #18, #19, #22–#25 and #28; #10 and #8c dropped.

**Waiting on the user:** #11 on the second machine (same fix; on Valentina_Desk it was done 2026-11-11): Git Bash's `gcloud` wrapper skips the Windows bundled Python and fell back to Python 3.13; fixed by `CLOUDSDK_PYTHON` pointing at `google-cloud-sdk/platform/bundledpython/python.exe` (user env var + `~/.bashrc`). (#27 committed in `actions` as `1139063`, 2026-11-10.)

**Next executable unit:** the shop-side ideas decided at the all-shop-plans meta-plan's G0 (2026-11-16), run as its unit U8: #55 (README note), #58 (admin `GET /api/admin/customers`), #59 (docs how-to), #61, #64, #66, #67, #68 per their recommendations. #60 and #63 done in U1 (#60: no TS breaker leak in the full TS pass; #63: docker k6 run documented in `c1000142`, local smoke 0% failed). New from U1: #69, #70 (storefront leftovers from the deleted SPEC/BUILD), #71 (gh-optivem, X1; shop side done in `db087f76`). New from U2: #72 (strict `spec-gate`, waiting on the user's waiver approval). #57 and #65 go to other repos (X1). #32–#34 and #36–#45 are done or moved (deleted 2026-11-17).

**Deferred:** #5, #15, #17, #20, #21, #26 (see their ideas below).

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
- **#27 meta dispatch matches the child run without `head_sha`:** done (`actions` `1139063`).

**Defer:** #47 (release SBOM assets), #5 (stub reset at suite start), #15 (`next start` standalone), #17 (split `SecurityConfigTest`), #20 (non-UTC CI timezone), #21 (HTML logger control character), #26 (RC tag push; revisit only if it recurs).

**Drop:** #8c (covered by `test-all.sh` legacy runs), #10 (`_stubIds` thread safety; theoretical).

**Decided 2026-11-12 (user): #32–#46, #49, #50, #51, #52, #53, #54 take their written recommendations** (#47 deferred, #48 and #35 leave as is). Routing (per `plans/20261112-0845-meta-storefront-ui-showcase-reliability.md`): #43 and #44 go with the Market UI plan; #39 and #41/#46 go with SMETA's fresh-customer unit (#41/#46: exempt UI-driver request DTOs from A1, scope it to API wire DTOs); #32, #33 and #34 are fixed in `actions`; #36/#45, #37, #38/#42 and #40 become one gh-optivem plan; #35: leave the bootstrap admin as is. Each idea is deleted here when its work lands.

**Decided 2026-11-16 (user, all-shop-plans meta-plan G0): #55–#68 take their written recommendations**, with #55 = a README note (the clean variant deliberately has no auth), #56 closed (already the rule), #58 yes (admin-only `GET /api/admin/customers` in all six backends: unpaged, A→Z-sorted JSON array of usernames that have placed an order; 401 anonymous, 403 non-admin; the four UI filters read it and `MAX_CUSTOMER_PAGES` goes away; drivers/DSL + an acceptance test), #59 a docs how-to only, #62 watch, #57 → gh-optivem and #65 → optivem-testing.

## Ideas to decide

Each idea has a recommendation. Everything up to #29 is decided (see "Resolved decisions"); #30–#54 are decided 2026-11-12 and #55–#68 on 2026-11-16 (see above).

### 5. Reset the stub server at the start of a suite run

Even with #2 fixed, a long-lived local stack accumulates any stubs a crashed or killed test run leaves behind. Option: reset dynamic mappings at suite start (`POST /__admin/mappings/reset`, which keeps the file-based static mappings), or on `gh optivem system start --restart`.
- **Recommended: defer.** #2 and #3 remove the known leaks. Revisit if leftovers still pile up.
- Note: a reset at suite start must never run while another suite is using the same stub server.

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


### 47. Releases created with `GITHUB_TOKEN` never fire `release` events — ⏳ Deferred (user, 2026-11-12): GHCR image SBOM attestations (`sbom: true`) already cover the good-practice part; release-page copies are low value. `release-sbom.yml` stays and works by `workflow_dispatch`.

Found 2026-11-12 (SHOW Step 4b). `_prod-stage.yml` creates releases through `optivem/actions/create-github-release` with the default `GITHUB_TOKEN`, and GitHub does not trigger workflows from events that token causes. So the new `release-sbom.yml` (`on: release: published`) never runs on its own; it only runs by `workflow_dispatch` with a tag.
- **Recommended:** call `release-sbom.yml` from the prod stage as a reusable workflow (`workflow_call`) after the release step, instead of switching to a PAT/app token. **Why:** no new secret to manage, and the SBOM upload stays tied to the release that produced it.

### 55. `backend-clean-java` has no authentication or authorization

Found 2026-11-12 (REL #52 coding). It has no Spring Security and no role checks, so `POST /api/orders/{n}/deliver` and the spec's admin-only `POST /api/admin/orders/sweep-deliveries` are open to anyone. No system test reaches it, because it has no system stack.
- **Recommended:** decide whether the clean variant should model auth at all. If yes, add a security adapter with the same role rules as backend-java. If not, say so in its README. **Why:** it's a teaching variant, so the omission may be deliberate, but right now it's silent.

### 56. Other sessions' commits sweep up work in progress

Found 2026-11-12 (REL #50–#54 session). Commit `a071aac8` ("Financial direction doc…") carried the Java/.NET stub-compose breaker threshold edits (`RESILIENCE_CIRCUITBREAKER_FAILURERATETHRESHOLD=100`, `Resilience__Erp__FailureRatio=1.0`), and `d858f52e` ("Workspace sweep") did the same.
- **Recommended:** parallel sessions commit with `gh optivem commit --paths` only, never `--all`. **Why:** a sweep commit mislabels another session's change and hides it from review and revert.

### 57. `--test` reruns overwrite the JUnit results `spec status` reads

Found 2026-11-12. A `--test` rerun overwrites the JUnit results that `spec status` reads, and the "never executed" check wipes the other suites' results. Extends #38/#42 (same "never executed" logic, not the overwrite).
- **Recommended:** fold into the #38/#42 gh-optivem plan: a filtered rerun merges into the existing results instead of replacing them. **Why:** one targeted rerun shouldn't erase the evidence of a full run.

### 58. React and Next.js admin customer filters scan only 50 pages of orders

Found 2026-11-12. The customer filter collects customers from the first 50 pages (2,500 orders); the local multitier DB already has 3,212, so later customers are never offered.
- **Recommended:** add a backend customers endpoint that the filter reads (an API change; needs user approval). **Why:** paging the whole order list to build a dropdown doesn't scale and silently truncates.

### 59. Stub Postgres and Keycloak data persist across `--restart`

Found 2026-11-12. Local stacks keep their data across `--restart` (over 5k orders and 2k coupons locally), which feeds #58 and slows list pages.
- **Recommended:** a documented reset (or periodic prune) for local stacks. **Why:** local runs should start from a known size, as CI does.

### 61. Java Playwright navigation hangs

Found 2026-11-12. Java Playwright waits 30 s for "scheduled navigations" after Keycloak login / Sign In, or on `navigate("/")` load. The hangs grew more frequent over the session and hit different tests each run; TS Playwright on the same stacks never hung.
- **Recommended:** investigate the Java Playwright version and the `waitUntil` settings. **Why:** it's Java-only, so it's likely the client version or its wait settings, not the stacks.
- **Seen again 2026-11-17 (U7, monolith-java, after the Playwright 1.65 Dependabot bump):** on two fresh `system start --restart` runs in a row, `--sample` failed only `E2E (real) - UI` (`shouldPlaceOrder`: `navigate("http://localhost:3111/")` load timeout, 30 s) and the new UC-002.11 test failed the same way on `navigate("/order-details?orderNumber=…")`. A Node Playwright probe of the same URLs loaded in about 1.3 s, and both Java tests passed when rerun on the warm stack. .NET and TS on the same flavor were green.

### 62. One-off "Coupon code DEFAULT-COUPON-<8hex> already exists" in the idempotency replay test

Found 2026-11-12. Seen once, unexplained.
- **Recommended: watch for it;** investigate if it recurs. **Why:** one occurrence isn't enough to diagnose.

### 64. Razor and Thymeleaf admin customer filters not checked for the `useOrders` bug

Found 2026-11-12. `6b07a16b` fixed React `useOrders` dropping the customer choices when a newer filter superseded the unfiltered request; the Razor and Thymeleaf filters weren't checked for the same bug.
- **Recommended:** check them (both already collect customers in a separate `loadCustomers`, so they're likely fine) and close this if so. **Why:** keeps the four UIs consistent. Update (.NET pass, 2026-11-12): Razor checked, it is unaffected, but capped by `MAX_CUSTOMER_PAGES` (the same limit as #58). Thymeleaf still to check.

### 65. optivem-testing `ChannelDataAttribute` runs UI-only tests in the API suites
Found 2026-11-12 (.NET pass). When the `CHANNEL` filter leaves no channels (a UI-only test in an API suite), the attribute falls back to all channels. So every UI-only test also runs in the API suites, in Dynamic channel mode. That doubles the browser load and caused the isolated-api "order had no shipping address" failures.
- **Recommended:** fix it in the optivem-testing library: an empty filtered set should run zero cases. Then release it and bump shop. **Why:** a filter that widens when it matches nothing hides failures and doubles UI cost.

### 66. Stub Keycloak memory grows during a run
Found 2026-11-12 (.NET pass). The stub Keycloak grew from 0.8 GB to 2.25 GB in one monolith acceptance run, most likely from creating and deleting a user for each test (fresh customer). Two runs were killed for low memory on a 32 GB machine.
- **Recommended:** cap the Keycloak JVM heap in the stub composes (e.g. `JAVA_OPTS_KC_HEAP`), and check whether user deletion leaves sessions or events behind. **Why:** local runs must fit alongside an IDE and a browser.

### 67. Pipeline-hiding commit-stage failures and remaining coupon-list readers
Found 2026-11-12 (pipeline-green pass).
- `multitier-backend-typescript-commit-stage` was red from `8ee90751` to `d01ea39f`, and the acceptance stage kept testing the last published image. The image was old but the run looked like a code failure (missing Retry-After). **Recommended:** make the acceptance stage fail loudly, or at least warn, when the backend image it pulls is older than the commit under test. **Why:** a red commit stage then shows up as its own cause, not as a misleading acceptance failure.
- `PublishCouponPositiveTest` (ShouldBeAbleToPublishCouponWithEmptyOptionalFields, ShouldBeAbleToCorrectlySaveCoupon, ShouldAcceptDiscountRateAboveZeroAndUpToOne) still reads the coupon back through the unpaged UI list in the parallel suites, ×3 languages. They read it straight after publishing, so the risk is lower than for the tests moved in `8d4fd72d`. **Recommended:** move them to the Isolated classes if they ever flake. **Why:** it's the same failure mode as UC-004.9.
- Note: the `8d4fd72d` message says "as Java and TS already have them". In fact that commit also carried the .NET pass agent's Java and TS moves.

### 68. .NET parallel UI suite: every in-flight browser stalls at once
Found 2026-11-13 (pipeline-green pass), twice, and in .NET UI suites only:
- meta-prerelease-stage 43299156147, multitier-dotnet docker-compose acceptance: 13 UI tests failed in one window (xUnit 01:18), each with a 30 s timeout after a `#kc-login` click that had already completed.
- meta-prerelease-stage 43406999176, cross-lang dotnet tests vs monolith java: 7 UI tests failed within 1.5 s (xUnit 01:53), each after about 90 s, with 30 s timeouts navigating to `/` and waiting for `load`.
Java and TS suites against the same stacks were green in the same runs. `81be91db` (xunit `maxParallelThreads` "0.5x") did not stop it. Every .NET UI test launches its own Playwright driver and Chromium (`MyShopUiClient.CreateAsync`), while Java and TS share a browser. The failures stay clustered, so something shared stalls (the runner CPU, the Playwright driver, or Keycloak, see #66), not the test logic.
- **Recommended:** reproduce locally with `docker stats` and runner CPU sampling during `acceptance-parallel-ui`. If it is CPU or process churn, move .NET to one shared Playwright and browser per test assembly (new context per test, like Java and TS). **Why:** that removes a Node driver process and a Chromium launch per test, the one structural difference from the languages that stay green.
- Update (U1 .NET pass, 2026-11-17, local, after `fd8ed67d` BrowserPool): the same clustered stall, now in the **API** suites too. On a fresh `--restart` stack both .NET parallel suites lost their whole first batch at xUnit 00:00:31 (monolith: 34 of 134 API, 11 of 147 UI; multitier: 1 + 1), API calls with `HttpRequestException ... forcibly closed by the remote host` after 30 s, UI calls with 30 s Playwright timeouts; multitier also logged one backend `ConnectTimeout` to the WireMock clock. Everything after that first batch passed, the isolated suites (sequential) were green, and a rerun of both parallel suites on the same stacks was green (134/134, 147/147 on both architectures). Java and TS parallel suites to compare in the same pass. So it is not (only) the browser count: something at the start of a parallel .NET run (16 cores, `0.5x` = 8 threads) stalls every in-flight connection for 30 s.
- **Recommended (updated):** sample thread-pool and socket state in the .NET test process during the first 30 s of `acceptance-parallel-api` (`dotnet-counters` for `threadpool-queue-length`, `netstat` for SYN_SENT/ESTABLISHED to 8x12), and try a one-off warm-up (a single smoke request per external port in an assembly fixture) before the parallel batch starts. **Why:** a reset of the whole first batch is a test-harness start-up effect, and a warm-up is a cheap, local fix if that is what it is.

### 69. Stale JUnit files in the results folders poison `spec status`

Found 2026-11-17 (U1 .NET pass). `system-test/dotnet/SystemTests/TestResults/junit/placeorder.xml` was a leftover from a 2026-11-07 run (78 failures, not produced by any `tests.yaml` suite). `system-test setup` doesn't clear the folder, so `spec status --config spec-dotnet.yaml` read it with the new results and reported UC-001.1–.13 and .16 as Failing although every current suite passed. Deleting it gave 110/112 verified (2 criterion-level `planned`). CI runners start clean, so only local gates are affected; the "flaky UC-007.4" of the 2026-11-12 Java pass may be the same.
- **Recommended:** have each language's `tests.yaml` setup (or a first `Clean` step) empty its JUnit results folder (`TestResults/junit`, `junit-results`, Gradle `test-results`) before a run. **Why:** a local spec gate must only read the run it gates.

### 70. Storefront leftovers carried over from SPEC/BUILD (deleted at SMETA close-out, 2026-11-17)

Moved here so nothing is lost when the storefront plans were deleted.
- Two criteria have no test and are tagged `planned` inside implemented use cases: `@UC-005.7` (no coupons: the shared coupon table is never empty) and `@UC-011.2` ("-" as the customer of an order with no recorded customer: needs DB seeding). Not covered either: UC-011.1 "newest first" (possible now with `given().order().placedAt(...)`), UC-005.3's total/empty page, and UC-010.7 doesn't assert the email.
- Cross-language behaviour differences found while writing the specs (the specs state only the common behaviour): cancelling a delivered order is refused only by `backend-clean-java`; a coupon with valid-to before valid-from gives an unmapped `IllegalArgumentException` (likely 500) on the Java monolith and backend-java and is not checked in .NET/TS; the Java monolith's order-details page shows Deliver to customers (the server then refuses), React only to admins; React's coupon list has a Status column computed from browser time instead of the Clock; `backend-clean-java`'s BrowseOrderHistory has no owner/admin scoping and no customer field (UC-011 BR-5).
- React `src/test` gaps listed by SMETA (UC-007 catalog/product, UC-008.5/.6/.8/.9/.10/.11, UC-009.2/.3/.5/.7/.8/.11/.12) belong to FT (`plans/20261112-0939-frontend-testing-component-a11y-lighthouse.md`).
- Strict mode (UC1 D4, U2): with all twelve use cases implemented, `spec check --mode strict` fails in every language only on these two `Planned` criteria (UC-005.7, UC-011.2). Java's extra UC-009.2 is fixed (#71, `db087f76`), so every language is at 110/112. Lenient passes everywhere. The strict switch itself is now #72.
- **Recommended:** triage the behaviour differences one by one (each is a small spec-or-code decision), and add the missing tests when the test kit can isolate coupons/orders (UC-005.7, UC-011.2). **Why:** they are real gaps, but none blocks the `implemented` status of a use case whose other criteria all pass.

### 71. `spec status` reports a passing parameterised UI-only Java test as Planned

Found 2026-11-17 (U1 Java pass). With every use case counted, Java shows `UC-009.2` as **Planned** although all its tests passed: the three UI runs of `shouldApplyTheTaxRateOfTheShippingAddressesCountry` are named with their arguments (`... [Channel: UI, address: ..., country: US, taxRate: 0.00]`) and become three separate test entries, while the API-suite skip (`shouldApplyTheTaxRateOfTheShippingAddressesCountry() [Skipped: API channel was not specified]`) normalises to the bare name, which then has only a skipped result and turns the criterion Planned. .NET and TS report it Verified. Lenient mode doesn't block on it; strict mode (U2) may count it as unverified.
- **Root cause (U2, 2026-11-17):** the `address` argument is the record `BaseCheckOutTest.Address`, whose default `toString()` is `Address[firstName=..., ...]`. Its brackets nest inside `[Channel: UI, address: ...]`, and gh-optivem's suffix regex (`\s*\[[^\]]*\]\s*$`, `internal/spec/junit.go`) can't strip a nested bracket, so the UI runs kept their argument lists. The API skip (`... [Skipped: ...]`) is dropped as not-applicable but still creates the bare-name entry with no results, which reads as skipped.
- **Shop side done (`db087f76`):** `Address.toString()` returns `formatted()` (no brackets). The UI runs now normalise to the bare name and UC-009.2 is Verified in Java (local isolated UI + API run).
- **Still recommended:** fix in gh-optivem (X1) as well: strip the whole trailing bracket group even when an argument nests brackets, and drop a test entry that has only not-applicable results. **Why:** any other record argument would hit the same trap, and a green test must not read as unbuilt.


### 73. `gh optivem commit` cannot push from a shared dirty checkout

Found 2026-11-17 (U10, meta-plan Wave 3). With several agents in one checkout, every `gh optivem commit --paths …` committed but stopped before pushing: its pull step runs `git pull --rebase`, which refuses while any other file is unstaged ("cannot pull with rebase: You have unstaged changes"). U10's three commits (`8cf687c3`, `5a294ef7`, `a2520492`) and an earlier B-PRG commit stayed local-only, so CI never saw them and the new `lighthouse.yml` could not be dispatched. (Numbered 73 because #72 is referenced in this file's header but its section was not in the local copy yet.)
- **Recommended:** fix in gh-optivem (X1): pull with a merge (`git pull --no-rebase --autostash`, matching the user's "plain git pull, never rebase" rule) so other sessions' dirty files don't block the push. Until then, the orchestrator pushes once per wave when the tree is clean. **Why:** a commit that silently stays local hides work from CI and from the "verify `git log origin/main`" check.

### 74. Sent outbox rows are never purged (backend-java multitier)

Found 2026-11-17 (U11 OUTBOX). The transactional-outbox worked example (`system/multitier/backend-java/.../core/outbox/`, migration `V20261117150000__add_outbox_and_order_placed_audit.sql`) keeps every sent `outbox` row and every `order_placed_audit` row forever. On a long-lived stack (prod, a student's running local stack) both tables grow by one row per order. The how-to (`docs/transactional-outbox.md`, Caveats) says so.
- **Recommended:** leave it until a real consumer lands, then add a purge of `outbox` rows with `sent_at` older than 7 days to the relay's poll (one `DELETE ... WHERE sent_at < now() - interval '7 days'` per N polls), mirroring the B-PRG idempotency-key purge. **Why:** the table is tiny at demo volumes, and the purge belongs next to the idempotency purge so the two follow one pattern.

### 75. Two `@EnableScheduling` in backend-java once B-PRG's purge lands

Found 2026-11-17 (U11 OUTBOX). U11 put `@EnableScheduling` on the new `config/OutboxConfig.java` so the outbox relay did not depend on uncommitted work. U9 (B-PRG idempotency purge) is adding `@EnableScheduling` to `BackendApplication.java` in the same checkout. Two copies are harmless (Spring imports the scheduling config once), just redundant.
- **Recommended:** after both are on `main`, remove `@EnableScheduling` from `OutboxConfig` and keep the one on `BackendApplication`. **Why:** one switch is easier to reason about.

### 76. TS monolith in production seals sessions with the committed test session secret

Found 2026-11-17 (U9, B-PRG 4.1). `system/monolith/typescript/src/lib/auth/settings.ts` defaults `AUTH_SESSION_SECRET` to `shop-monolith-test-session-secret-change-me`, and `_deploy-stage.yml` never sets it, so a production monolith-typescript deploy encrypts its session and login cookies with a key anyone can read in the repo. The 4.1 prod guard deliberately does not check it yet: adding the check today would stop the next production deploy of that image.
- **Recommended:** add a `${SYSTEM}-monolith-session-secret` Secret Manager secret (terraform, random value), map it with `--set-secrets AUTH_SESSION_SECRET=…` for the production stage in `_deploy-stage.yml`, then add `AUTH_SESSION_SECRET` to `productionConfigProblems` in `lib/production-config-guard.ts`. **Why:** a forged session cookie is a full account takeover, and the guard is the cheap way to keep it from coming back.

### 77. `gh optivem commit` appends a second, different Co-Authored-By trailer

Found 2026-11-17 (U9). Commits made with `gh optivem commit --yes --paths … "<message>"` end with the caller's `Co-Authored-By: Claude Opus 5.5` and `Claude-Session` lines, then a blank line and an extra `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>` the tool adds itself (e.g. `707e37a6`).
- **Recommended:** fix in gh-optivem (X1): skip the tool's own trailer when the message already has a `Co-Authored-By:` line. **Why:** the extra line names the wrong model and splits the trailer block.

### 78. `frontend-react` (nginx) sends no security headers

Found 2026-11-17 (U9 run 2, B-PRG 4.3). 4.3 put CSP, Referrer-Policy, HSTS, `nosniff` and `X-Frame-Options` on all seven backends, but the multitier UI is served by `system/multitier/frontend-react/nginx.conf`, which sends none of them. Its CSP is harder than the backends': the SPA talks to Keycloak (`KEYCLOAK_URL`, possibly a check-sso iframe) and optionally to a browser OTLP endpoint, so `connect-src`/`frame-src` need those runtime origins, and nginx's `add_header` is not inherited by a `location` that has its own `add_header` (all four here do).
- **Recommended:** add the non-CSP headers (`nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, HSTS) with `always` to every `location`, and a `Content-Security-Policy-Report-Only` built from `${KEYCLOAK_URL}` and `${OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT}` (add them to `NGINX_ENVSUBST_FILTER`); enforce it after a Lane D UI run shows no violations in the console. **Why:** the SPA is the user-facing surface of the multitier stack, and report-only first is the safe way to get its origins right.
