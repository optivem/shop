# 2026-11-10 12:07 UTC — System-test reliability follow-ups

## TL;DR

**Why:** The storefront work turned up a set of system-test problems that are not part of building the storefront: an intermittent .NET CI failure with no root cause, failed CI runs that keep no evidence, leaked fake-server data, language gaps in the test kit, unreliable local .NET runs, and three older issues. None of them blocks the storefront, but together they make a red pipeline slow to diagnose and a local run hard to trust as a gate.
**End result:** A failed acceptance stage uploads the stub journal, mappings and container logs, so the next intermittent failure can be diagnosed from one run. No language leaks dynamic stubs, and .NET/TS verify each stub after registering it, as Java does. The deploy-stage simulators run on one instance. The cloud stage is proven to test the current frontend image, and pushing to `main` mid-run no longer breaks `meta-prerelease-stage`. Locally, `test-all.sh` rebuilds images, stops each stack (also when cut off), and its memory limits are documented. .NET runs no longer flake on Keycloak tokens or fail always-red hidden tests, and lint agrees on Windows and Linux. All backends send `application/problem+json`, and the Node projects' high/critical `npm audit` findings are fixed or noted. Six ideas stay deferred (#5, #15, #17, #20, #21, #26) and two are dropped (#8c, #10). Unchanged: no production code behaviour apart from #8b's content type, and no new infrastructure cost.

## ▶ Next executable step (resume here)

Session 2026-11-10 did #1, #2, #3 (full latest acceptance runs per language on monolith stacks: .NET 363/363, Java 231 passed/0 failed/74 skipped, TS 250/250; afterwards each stub server held only its 18 static mappings, 0 dynamic, so no Java/TS leak), #4, #6, #7 (b) (token retry in all three languages; 0 token failures and 0 retries across the acceptance and sample runs), #8a, #8b, #9, #12, #13, #14 (system-test/typescript high 1 → 0; still deferred, needing a breaking major bump: postcss via next 16 (monolith), `@pact-foundation/pact` 17 (backend-typescript, frontend-react), vitest 5 (frontend-react, critical), and backend-typescript brace-expansion <=1.1.20, pinned by its dependent), #16 (now `gh-optivem/plans/20261110-2109-system-test-per-suite-counts.md`), #18, #19, #22–#25 and #28; #10 and #8c dropped.

**Waiting on the user:** #11 on the second machine (same fix; on Valentina_Desk it was done 2026-11-11): Git Bash's `gcloud` wrapper skips the Windows bundled Python and fell back to Python 3.13; fixed by `CLOUDSDK_PYTHON` pointing at `google-cloud-sdk/platform/bundledpython/python.exe` (user env var + `~/.bashrc`). (#27 committed in `actions` as `1139063`, 2026-11-10.)

**Next executable unit:** re-run `meta-prerelease-stage` (cloud, ask) to confirm #30 and #31; then decide #32 and #33. #31 is done (`PageClient.WaitForUrlChangeAsync` in .NET now polls `location.href`, because the .NET client's `WaitForURLAsync` misses a Next.js `router.push` that lands between its URL check and its event subscription; not hydration. Local .NET vs monolith-typescript: 12/12 and suite 57/57, previously ~9/22 failed). #30 is done (`actions` `f87d6d5`: post-dispatch run read polls on 404 only, 3 s up to 60 s). #29 is done: the .NET driver fix `d437c106` and the cloud run 43004938779 are green.

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

**Decided 2026-11-12 (user): #32–#46 take their written recommendations.** Routing (per `plans/20261112-0845-meta-storefront-ui-showcase-reliability.md`): #43 and #44 go with the Market UI plan; #39 and #41/#46 go with SMETA's fresh-customer unit (#41/#46: exempt UI-driver request DTOs from A1, scope it to API wire DTOs); #32, #33 and #34 are fixed in `actions`; #36/#45, #37, #38/#42 and #40 become one gh-optivem plan; #35: leave the bootstrap admin as is. Each idea is deleted here when its work lands.

## Ideas to decide

Each idea has a recommendation. Everything up to #29 is decided (see "Resolved decisions"); #30 and #31 are done; #32 and #33 (2026-11-11) are new ideas to decide.

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


### 32. `trigger-and-wait-for-workflow/wait.sh` treats every poll failure as transient

Found 2026-11-11 while fixing #30. `actions/trigger-and-wait-for-workflow/wait.sh:30-34` warns and retries on any `gh run view` failure until `TIMEOUT_SECONDS`, including 401, 403 and 404. A bad token or a wrong repo therefore shows up only as a long timeout instead of a specific error.
- **Recommended:** keep retrying on 5xx and network errors (and on 404 for a short visibility window, as in #30), but fail at once with `::error::` on 401 and 403. **Why:** that is the "never swallow errors" rule, and it turns a misconfiguration from a timeout into an actionable message.

### 33. `trigger-and-wait-for-workflow/trigger.sh` hides a failed rate-limit probe

Found 2026-11-11 while fixing #30. `actions/trigger-and-wait-for-workflow/trigger.sh:8,10` runs the rate-limit probe with `2>/dev/null || echo "999"`, so a failed probe silently looks like plenty of quota left.
- **Recommended:** keep stderr visible and print a `::warning::` when the probe fails, but don't fail the job. **Why:** the probe is only advisory, yet hiding its error breaks the never-swallow-stderr rule and hides auth problems.

### 34. Local pipeline failed pulling images: containerd "lease does not exist"

Found 2026-11-11 in `meta-prerelease-stage` run 43052472314 (`2c235677`). Child run 43052626758 (`prerelease-pipeline-monolith-dotnet`, `verify / local`) passed its unit tests (7/7, 129/129), then failed during `docker compose pull` with `failed to commit snapshot extract-… sha256:25ca64b9…: NotFound: lease does not exist: not found`. This is a containerd/BuildKit snapshot race on the GitHub-hosted runner, not a test or code failure. Re-running the failed job (attempt 2) passed, and the run went green with `meta-v1.0.225-rc.1543`, which confirms it was transient.
- **Recommended: retry the pull once on this specific error and investigate only if it recurs.** **Why:** it is a known transient runner-side race; a single retry on that exact message keeps real pull failures (auth, missing tag) loud.

### 36. Parallel sessions on one docker host clobber each other's stacks

Found 2026-11-11 during SMETA Step 7 (UC-007). A design session cycled docker stacks while the UC-007 agent was running, replaced its running monolith-typescript stack, and the next runs failed with "system stub is not running". Images are also built from the shared working tree, so one session's uncommitted backend/compose edits (OpenTelemetry) ended up in the other's test runs.
- **Recommended:** have `gh optivem system start` refuse (or warn loudly) when a stack for a different config is already running and was started by another process, and document "one session per docker host for system tests". **Why:** silent stack replacement turns into confusing "not running" failures that look like test bugs.

### 37. `gh optivem commit` can't push from a shared checkout with someone else's unstaged changes

Found 2026-11-11 (UC-007 commit `e964c99e`). `--paths` committed fine, but the pull step (rebase) refused to run while another session had unstaged changes, so the push never happened.
- **Recommended:** when the tree is dirty only outside `--paths`, pull with `--autostash`, or fall back to a plain fast-forward push when there's nothing to pull. **Why:** `--paths` exists for exactly this shared-checkout case, and it's useless if it can't push.

### 38. `system-test run --test` filter quirks

Found 2026-11-11 (UC-007). A regex alternation passed to `--test` is treated as one test name, so the summary says "never executed" even though the tests passed. On .NET the filter matches the method name only, so a class-name filter such as `ViewProduct` matches nothing.
- **Recommended:** document the filter semantics per language in `--help`, and make the "never executed" check use the same matching the runner used. **Why:** a false "never executed" makes a green run look red.

### 40. TypeScript `--sample` isolated-UI suite ran 0 tests after `npx` fetched playwright@1.65.0

Found 2026-11-11 (UC-007). The sample run printed "playwright@1.65.0 will be installed", ran 0 tests and exited 1, while the full isolated-UI suite on the same stack had just passed. That suggests `npx playwright` resolved outside the project's own `node_modules`, possibly during the docker contention in #36.
- **Recommended: investigate if it recurs.** Make the runner call the project-local Playwright binary instead of `npx` resolution. **Why:** an unpinned Playwright download can silently change versions and drop tests.

### 42. `--suite X --test Y` also runs the other acceptance suites

Found 2026-11-11 (UC-008), extending #38. `--suite acceptance-isolated-ui --test X` ran the other acceptance suites too, then reported "requested test(s) never executed" even though X passed. On .NET a shared method name (`ShouldKeepTheQuantityBetween1And99WhenUsingTheStepper`) also pulled in UC-007's tests.
- **Recommended:** fold into #38's fix (make `--suite` restrict the run, and support class-qualified names on .NET). **Why:** the same false-red problem.

### 45. Two sessions' `system-test setup` at once corrupt `system-test/java/build`

Found 2026-11-11 (UC-009), extending #36. Concurrent clean builds in one checkout corrupted the Java build directory; fixed with a clean rebuild.
- **Recommended:** fold into #36 (one session per checkout/docker host for system tests, or a lock file in `system-test setup`). **Why:** same root cause.

### 47. Releases created with `GITHUB_TOKEN` never fire `release` events — ⏳ Deferred (user, 2026-11-12): GHCR image SBOM attestations (`sbom: true`) already cover the good-practice part; release-page copies are low value. `release-sbom.yml` stays and works by `workflow_dispatch`.

Found 2026-11-12 (SHOW Step 4b). `_prod-stage.yml` creates releases through `optivem/actions/create-github-release` with the default `GITHUB_TOKEN`, and GitHub does not trigger workflows from events that token causes. So the new `release-sbom.yml` (`on: release: published`) never runs on its own; it only runs by `workflow_dispatch` with a tag.
- **Recommended:** call `release-sbom.yml` from the prod stage as a reusable workflow (`workflow_call`) after the release step, instead of switching to a PAT/app token. **Why:** no new secret to manage, and the SBOM upload stays tied to the release that produced it.

### 49. Architecture rule A2 fails in Java and .NET on verification lookup helpers

Found 2026-11-12 (SMETA fresh-customer unit). `VerificationMethodsAreFluentOrVoid` flags `CheckOutVerification.orderNumberFor`, `orderNumbers`, `placedSkus` and `ViewOrderVerification.orderedSku` in Java and .NET; TS's A2 passes.
- **Recommended:** move these lookup helpers out of the Verification classes (e.g. onto the response/result object), so A2 can be enforced as written. **Why:** it keeps the rule meaningful instead of growing an exemption list, and matches TS, which already passes.

### 50. Monolith order-history `loadOrders` has no stale-response guard

Found 2026-11-12 (SMETA UC-011 coding). In the Thymeleaf and Razor monoliths, overlapping reloads (choosing a customer, then resetting the filters) can let an older response overwrite a newer one, so the list shows the wrong orders. The UI driver avoids double reloads, so tests don't hit it today.
- **Recommended:** add a request counter (or `AbortController`) to `loadOrders` in both monoliths so only the latest response renders. **Why:** it's a small, real UI robustness fix, and React already guards this.

### 51. Next.js order-history cells lack the `orders-cell-*` classes

Found 2026-11-12 (SMETA UC-011 coding). The customer, quantity and status cells have no `orders-cell-*` classes, unlike the other three UIs, so the driver reads cells by column header instead.
- **Recommended:** add the classes in the Next.js monolith for parity, and keep the driver's header-based reading. **Why:** a one-line markup change that keeps the four UIs consistent; the header-based read already works everywhere.
