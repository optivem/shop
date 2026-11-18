# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

Session 2026-11-10 did #1, #2, #3 (full latest acceptance runs per language on monolith stacks: .NET 363/363, Java 231 passed/0 failed/74 skipped, TS 250/250; afterwards each stub server held only its 18 static mappings, 0 dynamic, so no Java/TS leak), #4, #6, #7 (b) (token retry in all three languages; 0 token failures and 0 retries across the acceptance and sample runs), #8a, #8b, #9, #12, #13, #14 (system-test/typescript high 1 → 0; still deferred, needing a breaking major bump: postcss via next 16 (monolith), `@pact-foundation/pact` 17 (backend-typescript, frontend-react), vitest 5 (frontend-react, critical), and backend-typescript brace-expansion <=1.1.20, pinned by its dependent), #16 (now `gh-optivem/plans/20261110-2109-system-test-per-suite-counts.md`), #18, #19, #22–#25 and #28; #10 and #8c dropped.

**Waiting on the user:** #11 on the second machine (same fix; on Valentina_Desk it was done 2026-11-11): Git Bash's `gcloud` wrapper skips the Windows bundled Python and fell back to Python 3.13; fixed by `CLOUDSDK_PYTHON` pointing at `google-cloud-sdk/platform/bundledpython/python.exe` (user env var + `~/.bashrc`). (#27 committed in `actions` as `1139063`, 2026-11-10.)

**Next executable unit (U8, state 2026-11-18):** U8 committed in this checkout (local; the shared dirty tree blocked `gh optivem commit`'s push, #73): #58 + #64 (`a8f8e9d6`), #66 (`85c89c25`), #84 token-client messages (`fcf43b1f`; docs half `051fde24` by Lane B), #69 (`2981f8ef`), #78 headers + report-only CSP (`6d20d8c0`, enforcement left in #78), #68 API half (`0cd89824`, xUnit `conservative`) and UI half (`f8a57fb9`: one home-page load + Keycloak sign-in before the first UI test, Chromium launched one at a time; fresh `--restart` monolith-dotnet `acceptance-parallel-ui` 151/151 twice, was 32 of 151 failed, `--sample` green); #58/#64/#66/#68/#69/#84 deleted here. #61 not reproduced (now watch). Still to do: #82 (implemented, uncommitted in the working tree; multitier gates left, see #82), #78 enforcement after a cloud multitier UI run. New: #85, #86. #75, #76, #80 done in Lane B (`8605c878`, `a94d06e1`, `ed389f13`) and deleted. #57, #65, #71, #73, #77, #79 go to other repos (X1). #70, #72, #74 not started.

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

### 57. `--test` reruns overwrite the JUnit results `spec status` reads

Found 2026-11-12. A `--test` rerun overwrites the JUnit results that `spec status` reads, and the "never executed" check wipes the other suites' results. Extends #38/#42 (same "never executed" logic, not the overwrite).
- **Recommended:** fold into the #38/#42 gh-optivem plan: a filtered rerun merges into the existing results instead of replacing them. **Why:** one targeted rerun shouldn't erase the evidence of a full run.

### 61. Java Playwright navigation hangs

Found 2026-11-12. Java Playwright waits 30 s for "scheduled navigations" after Keycloak login / Sign In, or on `navigate("/")` load. The hangs grew more frequent over the session and hit different tests each run; TS Playwright on the same stacks never hung.
- **Recommended:** investigate the Java Playwright version and the `waitUntil` settings. **Why:** it's Java-only, so it's likely the client version or its wait settings, not the stacks.
- **Seen again 2026-11-17 (U7, monolith-java, after the Playwright 1.65 Dependabot bump):** on two fresh `system start --restart` runs in a row, `--sample` failed only `E2E (real) - UI` (`shouldPlaceOrder`: `navigate("http://localhost:3111/")` load timeout, 30 s) and the new UC-002.11 test failed the same way on `navigate("/order-details?orderNumber=…")`. A Node Playwright probe of the same URLs loaded in about 1.3 s, and both Java tests passed when rerun on the warm stack. .NET and TS on the same flavor were green.
- **Not reproduced 2026-11-18 (U8, Playwright 1.65):** `--sample` on fresh `--restart` stacks of monolith-java and multitier-java, the targeted UC-011 UI tests on both, and a full `acceptance-parallel-ui` + `e2e-ui` on a fresh monolith-java stack (4 min 34 s) were all green with 0 Playwright timeouts. **Recommended now: watch;** the `WaitUntilState.DOMCONTENTLOADED` change stays a guess until a run reproduces the hang, then capture a Playwright trace of it. **Why:** a fix for a hang nobody can trigger can't be checked.

### 62. One-off "Coupon code DEFAULT-COUPON-<8hex> already exists" in the idempotency replay test

Found 2026-11-12. Seen once, unexplained.
- **Recommended: watch for it;** investigate if it recurs. **Why:** one occurrence isn't enough to diagnose.

### 65. optivem-testing `ChannelDataAttribute` runs UI-only tests in the API suites
Found 2026-11-12 (.NET pass). When the `CHANNEL` filter leaves no channels (a UI-only test in an API suite), the attribute falls back to all channels. So every UI-only test also runs in the API suites, in Dynamic channel mode. That doubles the browser load and caused the isolated-api "order had no shipping address" failures.
- **Recommended:** fix it in the optivem-testing library: an empty filtered set should run zero cases. Then release it and bump shop. **Why:** a filter that widens when it matches nothing hides failures and doubles UI cost.

### 67. Pipeline-hiding commit-stage failures and remaining coupon-list readers
Found 2026-11-12 (pipeline-green pass). First bullet done in `dc1a505e` (warning on a red commit stage).
- `PublishCouponPositiveTest` (ShouldBeAbleToPublishCouponWithEmptyOptionalFields, ShouldBeAbleToCorrectlySaveCoupon, ShouldAcceptDiscountRateAboveZeroAndUpToOne) still reads the coupon back through the unpaged UI list in the parallel suites, ×3 languages. They read it straight after publishing, so the risk is lower than for the tests moved in `8d4fd72d`. **Recommended:** move them to the Isolated classes if they ever flake. **Why:** it's the same failure mode as UC-004.9.
- Seen 2026-11-18 (U8, .NET monolith, parallel suite, fresh stack): `ShouldAcceptDiscountRateAboveZeroAndUpToOne(API, "0.01")` once, "Coupon with code 'SAVE-…' not found" in the list read back; green on the next two runs. Still a single occurrence, so no move yet.
- Note: the `8d4fd72d` message says "as Java and TS already have them". In fact that commit also carried the .NET pass agent's Java and TS moves.

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

### 77. `gh optivem commit` appends a second, different Co-Authored-By trailer

Found 2026-11-17 (U9). Commits made with `gh optivem commit --yes --paths … "<message>"` end with the caller's `Co-Authored-By: Claude Opus 5.5` and `Claude-Session` lines, then a blank line and an extra `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>` the tool adds itself (e.g. `707e37a6`).
- **Recommended:** fix in gh-optivem (X1): skip the tool's own trailer when the message already has a `Co-Authored-By:` line. **Why:** the extra line names the wrong model and splits the trailer block.

### 78. `frontend-react` (nginx) CSP is report-only

Found 2026-11-17 (U9 run 2, B-PRG 4.3). Done in U8 (`6d20d8c0`): `system/multitier/frontend-react/nginx.conf` sends `nosniff`, `X-Frame-Options: SAMEORIGIN`, `Referrer-Policy`, HSTS and a `Content-Security-Policy-Report-Only` (connect-src/frame-src/form-action from `${KEYCLOAK_URL}` and `${OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT}`), with `always`, in every location that sets its own `add_header`; `/api/` passes the backend's headers. Framing is same-origin, not `DENY`: oidc-client-ts's silent sign-in loads the app (redirect_uri) in an iframe of itself, and the first try with `DENY` / `frame-ancestors 'none'` logged a violation on every page. A local Playwright probe on multitier-typescript (anonymous pages, admin Keycloak sign-in, order history, coupons, cart) then logged 0 violations, and `--sample` was green. Not checked: a configured browser OTLP endpoint (empty locally) and the cloud stage's Keycloak origin.
- **Recommended:** switch `Content-Security-Policy-Report-Only` to `Content-Security-Policy` after one cloud multitier UI run (or a local run with `OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT` set) shows no violations in the browser console. **Why:** the two origins not yet seen are the ones an enforced policy would most likely block.

### 79. `cleanup-prereleases` deletes RC tags of another prefix whose version number was released

Found 2026-11-17 (meta-release red since 11-13). `optivem/actions/cleanup-prereleases/cleanup-prereleases.sh` keys `final_releases` and `prerelease_tags_map` by bare version (`1.0.226`), not by prefix + version. Cleanup 43548561064 (2026-11-14) logged "Version 1.0.226 (released as multitier-typescript-v1.0.226)" and deleted `meta-v1.0.226-rc.1550`, the RC that meta-release 43514773710 was promoting, so its re-run (attempt 2, 11-16) could not fetch the tag. The same pass deleted unreleased `monolith-dotnet-`, `monolith-java-`, `monolith-typescript-` and `multitier-dotnet-v1.0.238-rc.*` tags because `multitier-typescript-v1.0.238` exists. The meta version (1.0.226) collides with flavor versions, so every nightly cleanup can delete a live meta RC or flavor RC before it is promoted.
- **Recommended:** fix in optivem/actions: key both maps by `<prefix>|<version>` (`get_tag_prefix` already exists) so a release only clears its own pipeline's RCs; add a test with two prefixes that share a version. **Why:** the script deletes tags a release still needs, and the release fails much later with a "couldn't find remote ref" that hides the cause.

### 81. Cloud acceptance: one-minute us-central1 blip failed four flavors at once (2026-11-16 23:16 UTC)

Found 2026-11-17. In meta-prerelease 43910138981 (attempt 2), every cloud failure fell inside 23:15:58–23:16:40 UTC on separate Cloud Run services. The monolith-java and monolith-dotnet WireMock stubs returned GFE `503 ... Service error -27`; multitier-java (UI login on Keycloak, admin customer filter options never loaded) and monolith-dotnet (profile login) got Playwright 30–90 s timeouts. The multitier-dotnet failure was separate: one UI test that ran in the docker-compose isolated **API** suite (#65) hit a navigation timeout (#68 class). None reproduced on 5d36cd49 (2026-11-17: .NET and TS pipelines green, cloud acceptance included; Java was blocked only by the `traceId` DTO issue fixed in `0ff9ac7a`).
- **Recommended: watch.** If stub 503s recur outside a platform blip, retry WireMock admin calls on 503 in the three `JsonWireMockClient`s. **Why:** one simultaneous blip across independent services points to the platform, not the code.

### 82. Storefront aria-labels fail axe's label-in-name rule on every UI

Found 2026-11-17 (U10, FT Step 4). The first run of the new UI-channel axe check on monolith-typescript failed all four pages on `label-content-name-mismatch` (serious, WCAG 2.5.3 label in name): the header's `.store-sign-in` and `.store-cart`, the catalog's `aria-label="Product Title"` links and `aria-label="Add <sku> to Cart"` buttons, the cart's `aria-label="Title <sku>"` links and the checkout's primary button all have an accessible name that does not contain their visible text. The same test-hook aria-label pattern is in every storefront, and the system-test drivers select by those labels. FT Step 4 turned the rule off in the three drivers' axe check (`DISABLED_AXE_RULES` in `MyShopUiClient.ts`, Java and .NET `PageClient`), with the reason in a comment, so the check guards everything else.
- **Recommended:** a small cross-language unit: move the drivers' selectors for these elements to `data-testid` (or to selectors that use the visible text), drop or fix the test-hook aria-labels in the four UIs (Razor, Thymeleaf, Next.js monolith, React), then delete `DISABLED_AXE_RULES`. **Why:** voice-control users say the visible label to activate a control, so this is a real a11y bug, and once the drivers no longer depend on the labels it is a markup-only fix.
- **U8 2026-11-18: implemented, not yet committed (in the working tree).** Four UIs: the header Sign-in link and the catalog title link, add-to-cart button, cart line titles and checkout primary button carry `data-testid` (`sign-in`, `product-title`, `add-to-cart-<sku>`, `cart-line-title-<sku>`, `place-order`) instead of the test-hook aria-labels; the header Cart link's aria-label is dropped. Drivers ×3 select by those test ids, `DISABLED_AXE_RULES` / `AxeOptions` deleted in all three; legacy kits share the same page objects (mod03 raw tests use none of these hooks); React `SubmitButton.ariaLabel` optional; app tests (monolith .NET/Java `StorefrontPages*`, frontend-react unit tests) and `docs/design/shared/storefront-ux.md` §2/§3/§9 updated. Gates so far: `./compile-all.sh` 7/7, frontend-react `npm test` 121/121; monolith-dotnet `acceptance-ui` 165/165 + `--sample` green; monolith-java parallel UI 59 passed/0 failed (49 skipped), 4/4 axe tests, `--sample` green; monolith-typescript parallel UI 77/77, 4/4 axe tests, `--sample` green. **Left:** the three multitier stacks (React), same script (parallel UI, the four `*ShouldBeAccessible` isolated UI tests, `--sample`); the run was stopped by Claude Code for low host memory before a result, then commit (exact files) and delete this idea.

### 83. Keycloak 26 brute-force detection locks out concurrent successful logins

Found 2026-11-17 (monolith-dotnet acceptance 44151961920 red after `8a80fe92`). With `bruteForceProtected` on in `shop-realm.json`, the .NET parallel UI suite failed 9 tests in CI and 28-41 locally, each a 90 s timeout in `KeycloakUiLogin.LoginAsync` after submit. Keycloak events showed only `LOGIN_ERROR user_temporarily_disabled` for `customer1`/`admin1`, and the brute-force status showed `numFailures: 0`. So the lockouts came from many concurrent successful sign-ins of one account, not from failed ones (it happened again with `quickLoginCheckMilliSeconds: 0`). The fix moved the settings to `prod-realm.jq`, so they apply to production only. TS stayed green with it on (lower UI parallelism).
- **Recommended:** find out why Keycloak 26.0 locks out with no failures (check its issue tracker, or try a newer 26.x locally with the realm setting on and the .NET UI suite), and turn it back on in the test realms only once a version stops doing this. **Why:** production users can also sign in twice at once (two tabs, a double click), and the same lockout would hit them for 60 s or more.

### 85. TS legacy suites write `junit-results/results.xml`, which the spec gate also reads

Found 2026-11-18 (U8, #69). `system-test/typescript/tests.legacy.yaml` sets no `PLAYWRIGHT_JUNIT_OUTPUT_NAME`, so a legacy run writes Playwright's default `junit-results/results.xml`, and `spec-typescript.yaml` reads `junit-results/*.xml`. A legacy run after the latest suites (as `test-all.sh` does) adds the legacy results to the next `spec status` read. #69 clears the folder in the latest setup only.
- **Recommended:** give the legacy suites their own output folder (`PLAYWRIGHT_JUNIT_OUTPUT_DIR`, or a reporter option in a legacy config), so `junit-results/` holds only latest results. **Why:** the spec gate must read only the latest suites it gates.

### 86. backend-java `contractTest` fails locally on the three `*RealParityContractTest`s

Found 2026-11-18 (U8, #58 gates). `./gradlew contractTest` in `system/multitier/backend-java` ran 45 tests with 5 failures, all in `ClockRealParityContractTest`, `ErpRealParityContractTest` and `TaxRealParityContractTest`: `ExceptionInInitializerError` / `NoClassDefFoundError: Could not initialize class ...ExternalSystemReal`, without a running real external-systems stack. The pact verifications (latest and legacy) passed. Not caused by #58.
- **Recommended:** check what `ExternalSystemReal` needs (URL env or a running real stack), and have it fail with a message that names the missing setting, or skip when it is absent. **Why:** a static-initializer crash hides the cause and makes the whole contract task look broken.
