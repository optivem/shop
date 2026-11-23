# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

Session 2026-11-10 did #1, #2, #3 (full latest acceptance runs per language on monolith stacks: .NET 363/363, Java 231 passed/0 failed/74 skipped, TS 250/250; afterwards each stub server held only its 18 static mappings, 0 dynamic, so no Java/TS leak), #4, #6, #7 (b) (token retry in all three languages; 0 token failures and 0 retries across the acceptance and sample runs), #8a, #8b, #9, #12, #13, #14 (system-test/typescript high 1 → 0; still deferred, needing a breaking major bump: postcss via next 16 (monolith), `@pact-foundation/pact` 17 (backend-typescript, frontend-react), vitest 5 (frontend-react, critical), and backend-typescript brace-expansion <=1.1.20, pinned by its dependent), #16 (now `gh-optivem/plans/20261110-2109-system-test-per-suite-counts.md`), #18, #19, #22–#25 and #28; #10 and #8c dropped.

**Waiting on the user:** #11 on the second machine (same fix; on Valentina_Desk it was done 2026-11-11): Git Bash's `gcloud` wrapper skips the Windows bundled Python and fell back to Python 3.13; fixed by `CLOUDSDK_PYTHON` pointing at `google-cloud-sdk/platform/bundledpython/python.exe` (user env var + `~/.bashrc`). (#27 committed in `actions` as `1139063`, 2026-11-10.)

**Next executable unit (U8, state 2026-11-18):** U8 committed in this checkout (local; the shared dirty tree blocked `gh optivem commit`'s push, #73): #58 + #64 (`a8f8e9d6`), #66 (`85c89c25`), #84 token-client messages (`fcf43b1f`; docs half `051fde24` by Lane B), #69 (`2981f8ef`), #78 headers + report-only CSP (`6d20d8c0`, enforcement left in #78), #68 API half (`0cd89824`, xUnit `conservative`) and UI half (`f8a57fb9`: one home-page load + Keycloak sign-in before the first UI test, Chromium launched one at a time; fresh `--restart` monolith-dotnet `acceptance-parallel-ui` 151/151 twice, was 32 of 151 failed, `--sample` green); #82 (`a57c88f7`: storefront test hooks moved from aria-labels to `data-testid` in all four UIs, `DISABLED_AXE_RULES` deleted; all six stacks green: parallel UI, the four axe tests, `--sample`; multitier java 59 passed/0 failed/49 skipped, dotnet 151/151, typescript 77/77); #58/#64/#66/#68/#69/#82/#84 deleted here. #61 not reproduced (now watch). Still to do: #78 enforcement after a cloud multitier UI run. New: #85, #86. #75, #76, #80 done in Lane B (`8605c878`, `a94d06e1`, `ed389f13`) and deleted. #79 done in optivem/actions (`53fc86f`: cleanup maps keyed by prefix|version, `cleanup-prereleases/_test-cleanup-prereleases.sh` added) and deleted. #57, #65, #71, #73, #77 go to other repos (X1). #70, #72, #74 not started.

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
- React `src/test` gaps listed by SMETA (UC-007, UC-008.5/.6/.8/.9/.10/.11, UC-009.2/.3/.5/.7/.8/.11/.12), rechecked 2026-11-18 (FT follow-up, U10) against `system/multitier/frontend-react/src/test/unit/`. Most were already covered; files are `storefront.interaction` (SI), `cart.unit` (CU), `checkout.interaction` (CI), `auth.unit` (AU), `roles.unit` (RU):
  - **Covered:** UC-007.1 details + breadcrumbs (SI "shows the product details and breadcrumbs"); .2 and .3 (SI "keeps the stepper between 1 and 99 and adds the chosen quantity", CU "adds a product, and raises the quantity…"); .5 (SI "shows \"Product not found\"…"); .6 (SI "tells the visitor when the ERP is unavailable and offers no add to cart"); .7 (SI `it.each` "does not add the quantity %s", all five values). UC-008.5 (SI "shows each line at current catalog prices, with the subtotal", incl. the checkout hint); .8 across visits (CU "keeps the cart in localStorage across visits"); .9 (SI "removes a line, and says the cart is empty after the last one", incl. "Start shopping" link); .10 unavailable half (SI "keeps the cart when the ERP is unavailable"); .11 now all five values (SI `it.each` "keeps the previous quantity when the new one is %s", `e73bde72`; was `100` only). UC-009.2 estimated tax DE→GB (CI "preselects the saved address and reviews the cart with the estimated tax"); .3 preselect (same test); .7 (AU "redirects an unauthenticated visitor to login…", "remembers the page to come back to after the login" with `/checkout`); .8 (RU "blocks an admin-only user from the new order route"; `/checkout` uses the same `RequireCanPlaceOrder` guard in `App.tsx`); .12 (CI "keeps the rejected lines in the cart and shows why on the confirmation", `placeCheckoutOrders` "…keeps going after a rejection" for the dropped product).
  - **Real gaps (all small):** UC-007.1 open from the catalog (navigation) and UC-007.4 anonymous product view (product tests run as a customer); UC-008.6 stepper bounds (only one increase is checked, not decrease-at-1 or the 99 cap); UC-008.8 cart kept on sign-in; UC-008.10 cart shown again once the ERP is back; UC-009.3 placing to a different address (order address + profile untouched); UC-009.5 review quantity of each line and the "worked out per order" notice; UC-009.11 the UI rejects only an empty street (others via `validateAddress`: city, country, FR; first name, last name, postal code unchecked). UC-008.8 sign-in, UC-009.7 return-with-cart and UC-007.6 "rest of the site works" are cross-page flows that the UI-channel system tests already own.
  - **Recommended:** add the UC-008.6, UC-009.5 and UC-009.11 assertions (one `it.each` over the six address fields) as one small test-only commit in a later unit, and leave the cross-page flows to the system tests. **Why:** those three are one-file, mock-only additions that close the last per-criterion holes; the flows would only duplicate the system tests.
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

### 78. `frontend-react` (nginx) CSP is report-only — ✅ Done (`f02a79a1`)

Enforced 2026-11-20 (U9b): the three `add_header` lines now send `Content-Security-Policy` (same value). A local Playwright probe on multitier-typescript with `OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT=http://localhost:4318` set (anonymous catalog, product, cart; admin Keycloak sign-in; order history, an order-details page, coupons, account) logged 0 CSP violations under report-only and again under the enforced policy, with every page working, and `--sample` passed 11/11 on multitier-typescript, multitier-java and multitier-dotnet. Still not checked: the cloud stage's Keycloak origin.

Found 2026-11-17 (U9 run 2, B-PRG 4.3). Done in U8 (`6d20d8c0`): `system/multitier/frontend-react/nginx.conf` sends `nosniff`, `X-Frame-Options: SAMEORIGIN`, `Referrer-Policy`, HSTS and a `Content-Security-Policy-Report-Only` (connect-src/frame-src/form-action from `${KEYCLOAK_URL}` and `${OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT}`), with `always`, in every location that sets its own `add_header`; `/api/` passes the backend's headers. Framing is same-origin, not `DENY`: oidc-client-ts's silent sign-in loads the app (redirect_uri) in an iframe of itself, and the first try with `DENY` / `frame-ancestors 'none'` logged a violation on every page. A local Playwright probe on multitier-typescript (anonymous pages, admin Keycloak sign-in, order history, coupons, cart) then logged 0 violations, and `--sample` was green. Not checked: a configured browser OTLP endpoint (empty locally) and the cloud stage's Keycloak origin.
- **Recommended:** switch `Content-Security-Policy-Report-Only` to `Content-Security-Policy` after one cloud multitier UI run (or a local run with `OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT` set) shows no violations in the browser console. **Why:** the two origins not yet seen are the ones an enforced policy would most likely block.

### 81. Cloud acceptance: one-minute us-central1 blip failed four flavors at once (2026-11-16 23:16 UTC) — ✅ Done (`2d5cb10c`)

Recurred 2026-11-18 in multitier-java acceptance 44376493715 (sha 475ea05c), cloud "latest - Acceptance Isolated (stub) - UI", both attempts: a 502 on `POST /__admin/mappings` (attempt 1) and a read timeout on the same call (attempt 2). Cloud Run's request log for `shop-multitier-java-stub-stubs` has no 5xx and no slow admin request in either window (every admin call 2–10 ms, one instance start per attempt, no restarts), so the failing requests never reached WireMock: the front end lost them. No service-side root cause; `149cc519` only pinned `wiremock/wiremock:3.10.0` by digest (same image, no JVM settings). Fixed in `2d5cb10c`: all three `JsonWireMockClient`s retry stub registration (POST and the read-back/list) up to 4 attempts with 1/2/4 s backoff on 502/503/504 and transport failures (timeouts, resets), logging each failed attempt and failing with the URL and status once retries run out. Each mapping gets a client-chosen id, and a retry first checks whether the lost attempt landed, so a mapping is never added twice. .NET and TS admin requests now time out after 30 s (Java keeps the WireMock client's own timeout). `--sample` green on monolith java, dotnet and typescript.

Found 2026-11-17. In meta-prerelease 43910138981 (attempt 2), every cloud failure fell inside 23:15:58–23:16:40 UTC on separate Cloud Run services. The monolith-java and monolith-dotnet WireMock stubs returned GFE `503 ... Service error -27`; multitier-java (UI login on Keycloak, admin customer filter options never loaded) and monolith-dotnet (profile login) got Playwright 30–90 s timeouts. The multitier-dotnet failure was separate: one UI test that ran in the docker-compose isolated **API** suite (#65) hit a navigation timeout (#68 class). None reproduced on 5d36cd49 (2026-11-17: .NET and TS pipelines green, cloud acceptance included; Java was blocked only by the `traceId` DTO issue fixed in `0ff9ac7a`).
- **Recommended: watch.** If stub 503s recur outside a platform blip, retry WireMock admin calls on 503 in the three `JsonWireMockClient`s. **Why:** one simultaneous blip across independent services points to the platform, not the code.

### 83. Keycloak 26 brute-force detection locks out concurrent successful logins

Found 2026-11-17 (monolith-dotnet acceptance 44151961920 red after `8a80fe92`). With `bruteForceProtected` on in `shop-realm.json`, the .NET parallel UI suite failed 9 tests in CI and 28-41 locally, each a 90 s timeout in `KeycloakUiLogin.LoginAsync` after submit. Keycloak events showed only `LOGIN_ERROR user_temporarily_disabled` for `customer1`/`admin1`, and the brute-force status showed `numFailures: 0`. So the lockouts came from many concurrent successful sign-ins of one account, not from failed ones (it happened again with `quickLoginCheckMilliSeconds: 0`). The fix moved the settings to `prod-realm.jq`, so they apply to production only. TS stayed green with it on (lower UI parallelism).
- **Recommended:** find out why Keycloak 26.0 locks out with no failures (check its issue tracker, or try a newer 26.x locally with the realm setting on and the .NET UI suite), and turn it back on in the test realms only once a version stops doing this. **Why:** production users can also sign in twice at once (two tabs, a double click), and the same lockout would hit them for 60 s or more.

### 85. TS legacy suites write `junit-results/results.xml`, which the spec gate also reads

Found 2026-11-18 (U8, #69). `system-test/typescript/tests.legacy.yaml` sets no `PLAYWRIGHT_JUNIT_OUTPUT_NAME`, so a legacy run writes Playwright's default `junit-results/results.xml`, and `spec-typescript.yaml` reads `junit-results/*.xml`. A legacy run after the latest suites (as `test-all.sh` does) adds the legacy results to the next `spec status` read. #69 clears the folder in the latest setup only.
- **Recommended:** give the legacy suites their own output folder (`PLAYWRIGHT_JUNIT_OUTPUT_DIR`, or a reporter option in a legacy config), so `junit-results/` holds only latest results. **Why:** the spec gate must read only the latest suites it gates.

### 86. backend-java `contractTest` fails locally on the three `*RealParityContractTest`s

Found 2026-11-18 (U8, #58 gates). `./gradlew contractTest` in `system/multitier/backend-java` ran 45 tests with 5 failures, all in `ClockRealParityContractTest`, `ErpRealParityContractTest` and `TaxRealParityContractTest`: `ExceptionInInitializerError` / `NoClassDefFoundError: Could not initialize class ...ExternalSystemReal`, without a running real external-systems stack. The pact verifications (latest and legacy) passed. Not caused by #58.
- **Recommended:** check what `ExternalSystemReal` needs (URL env or a running real stack), and have it fail with a message that names the missing setting, or skip when it is absent. **Why:** a static-initializer crash hides the cause and makes the whole contract task look broken.

### 87. First DAST run: cookie flags, Permissions-Policy, CSP fallbacks, stale ZAP pin

Found 2026-11-18 (U9, B-PRG 6.7, `dast.yml` run 44333808708 on monolith-java, stub mode). 0 FAIL, 11 WARN, 56 PASS. Worth acting on: Cookie No HttpOnly Flag (10010, x3), Cookie without SameSite (10054, x5), Permissions Policy Header Not Set (10063, x11), CSP: Failure to Define Directive with No Fallback (10055, x9; `form-action` is left out on purpose, B-PRG 4.3), Insufficient Site Isolation / Spectre (90004, x12: no COOP/COEP/CORP). Also "ZAP is Out of Date" (10116): the pinned ZAP image is behind, and the job warns that `actions/upload-artifact@v4` runs on deprecated Node 20. The rest (suspicious comments, user-controllable attribute, non-storable content, modern web app, session management) are informational. Not checked yet: dotnet and typescript monoliths.
- **Recommended:** one small security-headers pass in all seven backends: `Permissions-Policy` with the features off, `Cross-Origin-Opener-Policy: same-origin`, and `HttpOnly` + `SameSite=Lax` on the cookies the report names (check which cookie first: Keycloak's own cookies are out of scope). Bump the ZAP pin and `upload-artifact` to v5 in the same change, then dispatch `dast.yml` for all three languages and compare. **Why:** these are cheap header and cookie settings, and scanning the other two languages shows whether the gap is shared.

### 88. Next.js order history rendered a stale unfiltered response over the admin's filter — ✅ Done (`0ffc2c5a`)

Found 2026-11-18 (meta-prerelease 44352569945, `monolith-typescript-acceptance-stage` 44373666475, cloud "latest - Acceptance Parallel (stub) - UI"). `adminShouldSeeNoOrdersWhenNoOrderMatchesFilters` and `adminShouldSeeOrderOfChosenCustomer` [UI] read the 50 unfiltered orders of the long-lived cloud DB. REL #50 (`d13b4a79`) made the Thymeleaf and Razor order histories drop superseded `loadOrders` responses, and frontend-react's `useOrders` already ignored them, but the Next.js page had no guard. It never mattered until REL #58 (`a8f8e9d6`): the customer options used to appear only after up to 50 paged order fetches, so the initial load was always finished by the time the driver chose a customer. With `/api/admin/customers` they appear at once, the choice lands while the unfiltered load is in flight, and that load's response (and its `setLoading(false)`) rendered over the filtered one. Reproduced locally with a forced 1.5 s unfiltered / 4 s filtered delay (2 failed, 50 orders received); not reproduced on plain local latency even with 207k orders. Fix: a `useRef` request counter in `system/monolith/typescript/src/app/order-history/page.tsx`; only the latest request sets orders, error or loading. Gates: tsc + eslint, the delay repro green, `acceptance-parallel-ui` 77/77 with 207k orders, `--sample` green. No test-driver change was needed.

### 89. .NET stub-mode Clock/Tax breakers kept the 10 s production break; a slow burst at the stubs instance failed a whole cloud suite

Found 2026-11-19 (meta-prerelease 44402085567 on `8abcd9c2`; `multitier-dotnet-acceptance-stage` 44424935506, cloud stub stack: about 15 API order tests failed with `SystemError: An unexpected error occurred`). Cloud Run logs: at 00:02:15.6 several backend `GET /clock/api/time` calls never reached `shop-multitier-dotnet-stub-stubs` (it logged no clock request until 00:02:18.4), because the single stubs instance (`--max-instances 1`, concurrency 80, 1 CPU) was taking about 200 parallel `__admin/mappings` calls a second from test setup. Every call hit the 2 s attempt timeout, so the Clock breaker opened. The stub-mode overrides (`STUB_BREAKER_ENV` in `_acceptance-stage.yml` and `docker/dotnet/*/docker-compose.*.stub.yml`, added in `4d0db38d`) covered only `Resilience__Erp__*`. The Java (`RESILIENCE_CIRCUITBREAKER_*`) and TS (`EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS`) overrides apply to every gateway, so only .NET kept Clock/Tax at a 10 s break with a 0.5 ratio, and every order in the next 10 s failed fast with `BrokenCircuitException`. Fix (shop side, done): `Resilience__Clock__*` and `Resilience__Tax__*` stub overrides (500 ms, ratio 1.0) next to the Erp ones in the workflow and in the four .NET stub composes.
- **Open, recommended:** stop the stubs instance from queueing traffic at the Cloud Run front end. Turn off WireMock's verbose admin-request logging in the stubs image (every admin POST logs its headers and body) and raise the stubs service's `--concurrency` (for example 250). It has to stay at one instance, because WireMock mappings live in memory. **Why:** #81 (lost admin POSTs) and this failure share one cause, a saturated single stubs instance. The breaker fix only shortens the damage, and a 2 s queue can still fail a test that lands inside it.

### 90. .NET gateways never retried a connect timeout; WireMock admin retry missed front-end 500s

Found 2026-11-19 (meta-prerelease 44510183439 on `b399cf71`, after #89's fix). (1) `multitier-dotnet-acceptance-stage` 44533203073 again failed its API order tests with `SystemError`. With the #89 overrides deployed, the breaker no longer stayed open, but the clock calls failed with `TaskCanceledException` → `TimeoutException: A connection could not be established within the configured ConnectTimeout` (1 s, during a burst of new TLS connections to the stubs service). `SocketsHttpHandler` reports a connect timeout that way, not as an `HttpRequestException`, so `IsFailure` in both .NET `ResilienceExtensions` didn't match it, and it was never retried. Java retries it (`HttpConnectTimeoutException` is an `IOException`), and so does TS. Fix: `IsFailure` also matches `TaskCanceledException { InnerException: TimeoutException }`, with a `RetriesAConnectTimeoutThenSucceeds` unit test in both .NET backends. (2) `monolith-java-acceptance-stage` 44533377344 failed one UI test on `__admin/mappings` → 500, which the stubs container never logged (it logged only 201s), so it came from the Cloud Run front end like #81's 502s. Fix: the admin retry in all three `JsonWireMockClient`s also covers 500. This is safe because the mapping id is client-chosen and checked before each retry (#81).

### 91. Prerelease waits time out under runner contention although the waited run passes

Found 2026-11-19 (meta-prerelease 44510183439). Three of six inner pipelines failed with `exceeded timeout of 7200s` on `*-acceptance-stage-docker-compose.yml`, but each waited run ended green (e.g. multitier-java 44514540514: dispatched 11:24, `preflight` started 11:55, `run` 12:13–12:42, `sonar` waited until 13:20, done 13:22:59; the wait gave up at 13:24:56). The time went to job queueing while the mutation nightlies, Lighthouse and two extra .NET cloud acceptance dispatches shared the runner pool.
- **Recommended:** keep 7200 s, and don't dispatch extra runs or let the mutation nightlies overlap with a meta-prerelease. If it happens again on a quiet pool, measure queue vs run time per job and either stagger the six pipelines or raise the docker-compose waits to 3 h together with the meta's 14400 s outer wait. **Why:** the timeout catches hung runs, and a longer one only hides contention.
- **Recurred 2026-11-20 on a quiet pool (meta-prerelease 44724359792, `f1bb9b34`), now fixed by staggering.** monolith-typescript, monolith-dotnet and multitier-typescript timed out again, and all five waited runs ended green. Only the meta's own jobs, two schedules and one push were in the pool. Measured on monolith-typescript 44726494812: dispatched 11:18, `preflight` 12:00 (42 min queued), `run` 12:10–12:17, `sonar` queued until 13:23 (66 min). The cause is the meta itself: 6 meta waiters plus 12 inner acceptance waiters (latest and legacy) held ~18 runners idle for 2 h, alongside 12 cross-lang jobs. **Fix:** `max-parallel: 3` on the `pipeline` matrix in `_meta-prerelease-pipeline.yml`, keeping 7200 s. Watch the next meta run: if three at a time still times out, drop to 2.

### 92. Local cross-lang UI runs stall 30 s on page loads when free RAM drops below 5 GB

Found 2026-11-20 (U12, local cross-lang runs of the order UI tests). On one stack at a time, with free RAM between 3.8 and 4.8 GB, parallel UI suites hit `Timeout 30000ms exceeded` on plain page loads (`navigating to "/"`, order history), and once on `PublishCoupon` with `connection forcibly closed`. It always happened in the first or last parallel burst and never in the isolated suites. The .NET harness was hit hardest (8 to 12 of 25 against the dotnet and TS monoliths, 0 against the java monolith right after a daemon cleanup). Running the old .NET driver on the same SUT timed out the same way, so the order-details route change isn't the cause. Gradle daemons and `dotnet build-server` processes kept from earlier runs held 1 to 2 GB, and Claude Code finally killed the multitier run for low memory.
- **Recommended:** make Lane D's local verification stop the Gradle daemons and `dotnet build-server` after each test-language run (`./gradlew --stop`, `dotnet build-server shutdown`), and run the cross-lang directions in CI (`cross-lang-system-verification.yml`) rather than all locally. A 16 GB machine can't hold a stack, three harnesses and an IDE above the 5 GB floor. **Why:** these failures are host contention and say nothing about the code, and CI runners have a clean memory budget per job.

### 93. Released images are signed but nothing verifies the signature

Found 2026-11-20 (U9 release check, meta-release 44784247292). Every prod-stage run signs its released image digests with cosign keyless ("Sign Released Images": `tlog entry created`, `Pushing signature to: ghcr.io/optivem/shop/<image>`), but no workflow runs `cosign verify` on them. The only verify in the logs is the installer checking its own binary (`cosign verify-blob ... cosign_v2.5.2`), and `SECURITY.md` documents a manual command. Signing that nothing checks proves little: a broken signing identity would only show up when someone verifies by hand.
- **Recommended:** add a `cosign verify --certificate-identity-regexp '^https://github.com/optivem/shop/' --certificate-oidc-issuer https://token.actions.githubusercontent.com <digest>` step right after signing in `_prod-stage.yml`'s `release` job, failing loud. It costs $0 and adds seconds. **Why:** it turns "we sign" into "we sign and the signature is valid for our identity", and it catches a broken signing setup on the same run.

### 94. CSP violations are invisible to every automated run

Found 2026-11-20 (U9 release check, REL #78). The cloud multitier prod and QA smoke runs (prod 44784657146 and its siblings; QA 44776538213, 44782828071, 44774715904) load only the home page, the system-test UI drivers never capture the browser console, and the nginx policy has no `report-uri`/`report-to`. So "no CSP violations in the logs" can't confirm #78.
- **Recommended:** have the Playwright UI drivers (all three languages) collect `console` messages of type error whose text contains `Content Security Policy`, and fail the test on any of them (or at least log them). **Why:** an enforced CSP that blocks a script fails silently in the browser. A console check turns it into a red test, locally and in cloud runs.

### 95. Monolith TypeScript can't move to ESLint 10 yet (Dependabot #141, #109)

Found 2026-11-20 (U15). `eslint-config-next@15.5.14` caps eslint at ^9, and `eslint-config-next@16.4.6` doesn't help: it pulls eslint-plugin-react 7.37.5, eslint-plugin-import 2.32.0 and eslint-plugin-jsx-a11y 6.10.2, whose latest releases still cap at ^9. `@eslint/js@10` needs eslint ^10, so #109 is blocked the same way. The other three TS projects moved to ESLint 10 in `97266b2f`.
- **Recommended:** keep the monolith on ESLint 9 and leave #141/#109 open. Re-check when those three plugins publish ESLint 10 peer support (`npm view eslint-plugin-react peerDependencies`). **Why:** the alternatives are forcing peers (`--legacy-peer-deps`, which the meta-plan rules out) or replacing eslint-config-next with a hand-built config, which would lose the React, import and a11y rules.

### 97. CLAUDE.md says `dotnet build` runs the unit tests; it doesn't

Found 2026-11-23 (U17). Repo `CLAUDE.md` → Pre-Commit Verification says "`./gradlew build` and `dotnet build` run the unit tests". True for Gradle, false for .NET: no project hooks a test target into build, so `dotnet build` only compiles. A .NET change can pass that gate with a red test, which is exactly what the note warns against. (U17 ran `dotnet test` explicitly: 139/139 in `backend-dotnet/Tests`.)
- **Recommended:** change the .NET bullet to `dotnet build && dotnet test` (test project dirs: `system/monolith/dotnet/Tests`, `system/multitier/backend-dotnet/Tests`) and fix the "Note that…" sentence to name Gradle only. Also check whether `compile-all.sh`'s .NET step runs `dotnet test`. Same section, Java side (found U18): in `system-test/java`, `./gradlew build` runs the whole system suite (497/509 fail without a booted stack: `KEYCLOAK_URL_STUB` not set), so name that project's gate explicitly as `./gradlew testClasses` + `--sample`. **Why:** a doc-only fix, it closes a real gap in the commit gate, and it's cheaper than adding an MSBuild hook that would make every build slower.

### 98. Gradle 10 deprecations in the four Java builds

Found 2026-11-23 (U18, Gradle 9.9.1). `--warning-mode all` prints 21 deprecations: `afterSuite` / `afterTest` / `beforeTest(Closure)` on test tasks (e.g. `backend-clean-java/build.gradle:147–151`), `Configuration.setVisible(boolean)` ×4, and Groovy space-assignment in `settings.gradle:7` (at least `backend-clean-java` and `system-test/java`).
- **Recommended:** one cleanup pass across `system/monolith/java`, `system/multitier/backend-java`, `backend-clean-java` and `system-test/java` (`addTestListener`, drop `setVisible`, `url = ...`), gated by `./gradlew build --warning-mode fail` in each. **Why:** Gradle 10 turns these into errors, and a Dependabot major is a bad moment to find out.

### 99. `compile-all.sh` + lingering Gradle daemons trip the host memory reaper

Found 2026-11-23 (U18). With no docker stack up and VS Code open (~4 GB), `./compile-all.sh` was killed by Claude Code's memory-pressure reaper; daemons left by earlier configs (Gradle, dotnet build servers) stay resident across the sweep. Same family as #66 / #92.
- **Recommended:** run the Java configs in `compile-all.sh` with `--no-daemon` (or `./gradlew --stop` after each) and `dotnet build-server shutdown` at the end. **Why:** the sweep is a one-shot gate, so daemon warm-up buys nothing and costs ~1–2 GB per project.

### 100. Java backends sit on the deprecated Jackson 2 bridge after Spring Boot 4.1

Found 2026-11-23 (U19, `e467f3f6`). To keep `MoneyJsonModule`, the gateways, the ProblemDetail handlers and `IdempotencyKeyStore` / `OutboxStore` unchanged, U19 added `spring-boot-jackson2` and moved config to `spring.jackson2.*` + `spring.http.converters.preferred-json-mapper: jackson2` in monolith, backend-java and backend-clean-java. Both are deprecated since Boot 4.0.
- **Recommended:** a dedicated unit that migrates the three Java backends to Jackson 3 (`tools.jackson`), updates clean-java's `ArchitectureTest` package ban (`com.fasterxml.jackson..`), drops the bridge, and re-syncs the hq/temp article snippets that quote this code (`[[project_hq_temp_docs_mirror_shop]]`). **Why:** the bridge will be removed in a later Boot major; doing it as its own unit keeps the money wire format (cross-language) under one focused gate instead of riding a dependency bump.

### 101. Stale OpenTelemetry version comment in the Java build files

Found 2026-11-23 (U19). The build files say the instrumentation BOM 2.33.1-alpha is pinned to "Boot's OTel SDK 1.49.0", but it imports opentelemetry-bom 1.67.0 while Boot 4.1 manages 1.62.0.
- **Recommended:** `./gradlew dependencyInsight --dependency opentelemetry-sdk` in each backend, then fix the comment (and the pin if the two BOMs disagree at runtime). **Why:** a wrong comment will mislead the next upgrade.

### 102. Deprecated API use left in Java backends after Boot 4.1

Found 2026-11-23 (U19). Deprecation warnings remain in the three `GlobalExceptionHandler`s, monolith `AbstractIntegrationTest` and backend-java `OrderControllerIntegrationTest`.
- **Recommended:** one `-Xlint:deprecation` build per backend and a cleanup commit; combine with #98 (Gradle 10 deprecations) if convenient. **Why:** cheap now, forced later.

### 103. `default-timezone: UTC` in the Java `application.yml` jackson block is not a Boot property

Found 2026-11-23 (U19); predates the upgrade. Boot ignores it silently.
- **Recommended:** delete it (UTC is already enforced elsewhere; confirm with the clock tests). **Why:** dead config suggests a guarantee that isn't there.

### 104. clean-java `GET /api/admin/customers` returns `[]` for its own orders; README counts stale

Found 2026-11-23 while fixing the clean-java commit stage (red since 2026-11-18: REL #58 added the endpoint to "all six backends" and the frontend pact, but not to `backend-clean-java`, so pact `browse-customers` got a 404). Fix `b8ce6935`: open endpoint (clean-java has no auth by design), reading `SELECT DISTINCT owner_name FROM orders` natively. Clean-java's domain `Order` has no customer and its PlaceOrder never writes `owner_name`, so against its own data the list is always empty. The README line from #55 saying the variant has no such endpoint was rewritten. README test counts are also stale (106/51/43 unit/integration/contract vs 116/58/45; "62 component" now 64).
- **Recommended:** accept the empty-list behaviour (no auth means no customer, consistent with the variant's design) and add one README sentence saying so; refresh the counts in the same commit. Also add clean-java to whatever checklist produced "all six backends", so the seventh backend isn't missed again. **Why:** the pact contract is satisfied, and modelling a customer in clean-java's domain would undo its deliberate no-auth simplification.

### 105. Compose image tags drift from the Dockerfile pins (Dependabot only bumps the Dockerfiles)

Found 2026-11-23 (U20, `0c417732`). `dependabot.yml` excludes `docker/**/docker-compose*.yml` on purpose, and `compose-drift.yml` only checks that the 24 compose files agree with **each other**. Nothing checks them against the digest-pinned `FROM` lines in `system/db/Dockerfile`, `docker/keycloak/Dockerfile` and `external-systems/stubs/Dockerfile`. #91 and #95 would have left all 24 compose files on flyway 10.21 / keycloak 26.0 / wiremock 3.10 (U20 bumped them by hand), and local `--sample` would have tested the old images.
- **Recommended:** extend `compose-drift.yml` with one assertion: the flyway, keycloak and wiremock tags in the compose files equal the tag part of the matching Dockerfile `FROM`. **Why:** it keeps the existing "no Dependabot on compose" decision and turns silent drift into a red check on the Dependabot PR itself.

### 106. `gh optivem commit --paths` silently treats a comma-separated list as one path

Found 2026-11-23 (U20). `--paths "a,b"` fails with a `git add` pathspec error (exit 128); only space-separated works, and the help text doesn't say so. → gh-optivem.
- **Recommended:** goes to X1 as a gh-optivem plan item (accept commas or document the separator). **Why:** shop doesn't edit gh-optivem (read-only, 2026-11-06).

### 107. Keycloak minor bump took the shared cloud Keycloak (incl. `shop-prod`) down for ~40 min

Incident 2026-11-23, caused by U20 (`0c417732`, keycloak 26.0 → 26.8 in `docker/keycloak/Dockerfile`). The multitier-dotnet QA stage (run 45165378553, tested `e52df37b`) built and deployed revision `shop-keycloak-00014-dbn`; on first boot Keycloak 26.8.2 ran its Liquibase + model migration (26.0 → 26.1+) against the shared DB and returned 503 ("Request received during bootstrapping") for all realms. The deploy's `wait-for-endpoints` (900 s) timed out at 11:43 (exit 124) and failed meta-prerelease 45136998785. After that nothing hit the service, so Cloud Run (1 vCPU, request-based CPU) starved the migration; it only progressed once the orchestrator polled the realms every 5 s, and all three realms returned 200 at 12:08 UTC. Outage window ~11:28–12:08 UTC for shop, shop-qa **and shop-prod**. Not rolled back: Keycloak doesn't support DB downgrade once the migration has started. Local `--sample` couldn't catch it: local stacks use a fresh DB and the plain upstream image, not `docker/keycloak/Dockerfile`.
- **Recommended:** (1) treat any `quay.io/keycloak/keycloak` minor/major bump as its own unit, not part of a Dependabot group: back up the Keycloak DB first, deploy during a quiet window, and keep the service warm (or temporarily `--no-cpu-throttling`) until the realms answer; (2) raise the `deploy-keycloak` wait for upgrades and keep polling during it, so the poll itself keeps CPU allocated; (3) add Keycloak to the `dependabot.yml` docker group's ignore list for `version-update:semver-minor` so it can't ride a group PR again. **Why:** the shared Keycloak is a prod dependency, and its upgrade path is a one-way DB migration that the local gates never exercise.

