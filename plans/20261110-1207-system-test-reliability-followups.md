# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

Session 2026-11-10 did #1, #2, #3 (full latest acceptance runs per language on monolith stacks: .NET 363/363, Java 231 passed/0 failed/74 skipped, TS 250/250; afterwards each stub server held only its 18 static mappings, 0 dynamic, so no Java/TS leak), #4, #6, #7 (b) (token retry in all three languages; 0 token failures and 0 retries across the acceptance and sample runs), #8a, #8b, #9, #12, #13, #14 (system-test/typescript high 1 → 0; still deferred, needing a breaking major bump: postcss via next 16 (monolith), `@pact-foundation/pact` 17 (backend-typescript, frontend-react), vitest 5 (frontend-react, critical), and backend-typescript brace-expansion <=1.1.20, pinned by its dependent), #16 (now `gh-optivem/plans/20261110-2109-system-test-per-suite-counts.md`), #18, #19, #22–#25 and #28; #10 and #8c dropped.

**Waiting on the user:** #11 is user-side. (#27 committed in `actions` as `1139063`, 2026-11-10.)

**Next executable unit:** #30: the `trigger-and-wait-for-workflow` 404 retry in `optivem/actions` (small, no local stack needed). Then #31: the .NET browse-catalog timeout against the TS frontend (local repro, needs the user's yes). Then re-run `meta-prerelease-stage` (cloud, ask). #29 is done: the .NET driver fix `d437c106` and the cloud run 43004938779 are green.

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

**Defer:** #5 (stub reset at suite start), #15 (`next start` standalone), #17 (split `SecurityConfigTest`), #20 (non-UTC CI timezone), #21 (HTML logger control character), #26 (RC tag push; revisit only if it recurs).

**Drop:** #8c (covered by `test-all.sh` legacy runs), #10 (`_stubIds` thread safety; theoretical).

## Ideas to decide

Each idea has a recommendation. Everything up to #29 is decided (see "Resolved decisions"); #30 and #31 (2026-11-11) are next, with their recommendations.

### 5. Reset the stub server at the start of a suite run

Even with #2 fixed, a long-lived local stack accumulates any stubs a crashed or killed test run leaves behind. Option: reset dynamic mappings at suite start (`POST /__admin/mappings/reset`, which keeps the file-based static mappings), or on `gh optivem system start --restart`.
- **Recommended: defer.** #2 and #3 remove the known leaks. Revisit if leftovers still pile up.
- Note: a reset at suite start must never run while another suite is using the same stub server.

### 11. Fix local `gcloud`

Local `gcloud` fails because it doesn't support Python 3.13. It needs `CLOUDSDK_PYTHON` pointing at Python 3.8–3.12. That blocked checking the Cloud Run instance counts directly.
- **Recommended: do, once per machine.** Set `CLOUDSDK_PYTHON` in your shell profile on both machines. Note it in `CONTRIBUTING.md` if other contributors use `gcloud`.

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


### 30. `trigger-and-wait-for-workflow` fails on a 404 when it reads a run it has just dispatched

Found 2026-11-11 in `meta-prerelease-stage` 43009532540: `pipeline (multitier-java)` and `pipeline (monolith-dotnet)` failed with `Could not read dispatched run <id> of <workflow>: gh: Not Found (HTTP 404)`. The monolith-dotnet failure came from its child legacy acceptance step (run 43019313596). The child runs themselves were fine: the multitier-java pipeline run 43017020444 finished **green**.
- **Cause:** `actions/trigger-and-wait-for-workflow/trigger.sh:138-141`. Straight after dispatching, the "did `$REF` move" diagnostic does `GET /runs/{id}` for the id the dispatch API returned. GitHub hasn't made the new run visible yet, so the read gets a 404. `retry_run` doesn't retry 404s, so the action fails hard. This looks like it came in with #27's change (`1139063`), which switched to the run id the dispatch API returns.
- **Recommended fix:** in `optivem/actions`, poll that read on a 404 only, every 3 s for up to 60 s. If the run still isn't visible after that, fail loudly with the run id and workflow. **Why:** a 404 straight after a dispatch with a known run id is GitHub's API lag, not a missing run, and the timeout keeps the rule that probes never swallow errors. Verify with `bash -n` and `shellcheck` on `trigger.sh`, commit in `actions`, then re-run `meta-prerelease-stage` (cloud, ask about cost).

### 31. Cross-lang: .NET browse-catalog UI test times out against the monolith-typescript frontend

Found 2026-11-11 in `meta-prerelease-stage` 43009532540, job `cross-lang / dotnet tests vs monolith typescript`. `BrowseCatalogPositiveIsolatedTest.ShouldShowOnlyTheProductsInTheChosenCategory(UI)` failed both times with `Timeout 30000ms exceeded`, waiting for navigation until "Load". The stack is `CatalogPage.SelectCategoryAsync` → `ChangeViewAsync` → `PageClient.WaitForUrlChangeAsync`, which means the URL never changed after the category label was clicked.
- The markup matches: `fieldset[aria-label='Category Filter']` exists in `system/monolith/typescript/src/app/CatalogView.tsx:81`. The .NET `ChangeViewAsync` (`system-test/dotnet/Driver.Adapter/Ui/Client/Pages/CatalogPage.cs:125-131`) does the same as the TS `changeView` (`system-test/typescript/src/testkit/driver/adapter/ui/client/pages/CatalogPage.ts:108-113`). The TS driver passes against this frontend, so the failure only happens with the .NET driver on Next.js. The frontend changes category with client-side `router.push` (`CatalogView.tsx:141`).
- **Hypothesis (not verified):** a hydration race. The .NET driver clicks the radio before React has hydrated, so the browser checks the radio natively and `onChange` → `router.push` never fires. Another possibility: the .NET driver opens the catalog page differently, so it doesn't wait for the same ready state the TS driver does.
- **Recommended:** reproduce locally by running the .NET latest acceptance tests against the monolith-typescript stack, the same combination as the cross-lang job (see how `meta-prerelease-stage` / `_cross-lang*` wires it, needs the user's yes for the local stack). Then fix in the .NET catalog page object, e.g. wait for the page to be hydrated/ready before the first filter click, or assert the option is checked and retry the click. Check the Java driver against the TS frontend for the same race. This is storefront test-kit code (`1607bbec`).
