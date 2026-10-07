# 2026-10-07 UTC — Keycloak auth + order ownership: remaining rollout (consolidated)

Consolidates three earlier plans (deleted): `20261002-0801-keycloak-auth-java-react-first`, `20261002-1209-order-ownership-and-dsl-identities`, `20261006-1200-latest-smoke-health-without-token`.

## TL;DR

**Why:** The shop has Keycloak (OIDC, JWT-validating backends) and an order-ownership model on Java multitier + React, plus server-side OIDC login on all three monoliths. What remains is verifying the last pushes, spreading order ownership to the other backends/monoliths, tightening CI strictness, and the cloud/prod IdP.
**End result:** Every system under test (Java/.NET/TypeScript, multitier and monolith) enforces the same authentication and ownership rules, the test DSL expresses identities, CI fails loudly on missing Keycloak, and a cloud/prod IdP design exists.

## Execution style (author decision 2026-10-03)

**No `/clear` hand-offs.** Run the whole plan in ONE session and keep the main context small by delegating to subagents (default, non-worktree):

- one subagent per long wait/monitor (release publish, `meta-prerelease-stage` run, ~1h+);
- one subagent per failure diagnosis (`gh-monitor` / `general-purpose`), returning only root cause + proposed fix;
- one subagent per language/system for Step 2 (dotnet multitier, typescript multitier, three monoliths) once the Java recipe is fixed — each returns a short verdict plus the commit/push status.

The main session only dispatches, reads short verdicts, and approves fixes. Do not end with a "Next session: /clear" block unless blocked on the author. Push per verified milestone; stop if CI breaks. Release actions (`meta-release-stage`, `gh-release-stage`) are pre-approved for Step 4 once shop is green.

## Current state (what is done)

- **Keycloak Phase 1 (Java backend + React):** realm `shop` (`docker/keycloak/shop-realm.json`), clients `shop-frontend` (public, PKCE S256), `shop-system-test` (test realm only), `shop-monolith-*` (confidential), roles CUSTOMER/ADMIN, users `customer1`, `customer2`, `admin1` (admin1 = ADMIN only). Backend-java is a Spring resource server (signature, iss, aud, exp; central URL rules in `SecurityConfig`, secure by default). System-test drivers acquire tokens in the driver layer; DSL free of auth concerns.
- **Endpoint rules:** public `/health`; authenticated `/api/orders*`, `/cancel`, `/api/coupons` GET; ADMIN only `/api/admin/**`, `/api/coupons` POST, `/deliver`.
- **Monoliths (Step 11) DONE** for Java (Spring `oauth2Login` + PKCE customizer), .NET (OpenIdConnect, `ResponseMode=Query`), TypeScript (`openid-client` + `jose`, rules in `src/lib/auth/policy.ts`, enforced in `src/middleware.ts`). Browser holds a session cookie only.
- **Order ownership (Java multitier) DONE, Steps A–C committed and pushed:** `orders.owner` (token `sub`) / `owner_name` (`preferred_username`) via migration `V20261005190000__add_order_owner.sql`; place order requires CUSTOMER; customers see only their own orders (another's => 404); admins see all (history items carry `customer`), may cancel any, deliver stays admin-only; null-owner orders are admin-only; React hides place-order from admins. Test DSL has identities (customer / another customer / admin); the UI client logs in as the scenario's identity.
- **CI:** `meta-prerelease-stage` run 37142307930 (SHA 77f51479) fully GREEN. `gh-optivem` has `Component.HealthUrl` (monolith probes use `<ui>/health`).
- Verified 2026-10-07 (audit + CI): CI on shop main is green; order ownership (owner/owner_name, CUSTOMER-only placement, scoped history/lookup, 404 for others, admin sees/cancels all, deliver admin-only, matching 401/403 messages, UIs hide place-order from admins) is DONE on TS multitier, .NET multitier, and all three monoliths (commit 2c02b48a plus the .NET `OrderOwnershipComponentTests` and the TS `BrowseOrderHistory` use-case classes added 2026-10-07). The system-test kits for .NET and TypeScript have the identities, order-history use case and the 4 scenarios. Identity scenarios run in the pipeline/acceptance workflows (KEYCLOAK_URL_REAL/STUB set); the non-cloud `*-qa-stage*`, `*-prod-stage*` workflows set no KEYCLOAK_URL, so they currently skip identity scenarios (they run no system tests, so nothing to wire); cloud pipelines are out of scope. The TS scenario stages call the driver directly (no use-case layer anywhere in TS) — matches TS convention, left as is.
- Decision (2026-10-07): no anonymous-health check in the latest smoke test; existing coverage is enough (`SecurityConfigTest.healthIsPublic`, `AuthorizationComponentTest.healthIsPublic`, legacy `MyShopApiSmokeTest` and `ApiAuthorizationTest.shouldNotRequireTokenForHealth`, unauthenticated CI/compose probes).
- Decisions settled: IdP Keycloak; in-app JWT validation; SPA auth code + PKCE with tokens in memory (BFF deferred); real Keycloak in both stub and real compose variants (no token stub); monoliths use server-side OIDC; another customer's order => 404; existing orders keep a null owner; no pagination in this scope.

## Steps

- [ ] Step 1: **Verify the retired Keycloak opt-in guard in CI.** Done in code 2026-10-07 (Java/.NET/TS latest acceptance suites: guard deleted, a missing `KEYCLOAK_URL_*` is a hard configuration failure; cloud pipelines out of scope; the non-cloud QA/prod/signoff workflows run no system tests so need no wiring). Remaining: watch the acceptance-stage runs after the push (positive identity paths were only compile-verified locally) and fix any failure.
- [ ] Step 2: **Release (pre-approved by the author 2026-10-07 once shop is green; still read the workflow before running).** Run shop `meta-release-stage` (it dispatches `gh-acceptance-stage` in `optivem/gh-optivem`, ~line 302 of `meta-release-stage.yml`), then `gh-release-stage` to promote the gh-optivem RC with `healthUrl`. Use a monitor subagent; do NOT dispatch `gh-release-stage` manually before shop is green.

## Follow-ups (not blocking; pick up opportunistically)

- .NET legacy UI suites failed intermittently (~1 in 9 runs, 30s Playwright timeout after Keycloak login); mitigated by `hashIterations(1)` + 90s timeout, `KeycloakUiLogin` now polls `location.href` via `WaitForFunctionAsync`. Root cause unconfirmed; the last green run held.
- .NET backend: access rules are controller attributes, not a central table; unknown routes return 404 not 401; no route-enumeration test; JWKS fetch has no retry.
- Improve `SecurityConfigTest` to enumerate every controller route (e.g. via `RequestMappingHandlerMapping`): admin-only => 403 for CUSTOMER and 401 with no token, public => open, a new route without an expected status fails. Read the existing test first. Data-dependent rules (ownership) stay in the service layer / `@PreAuthorize`.
- Expired-token and wrong-audience cases rely on Spring's standard validators; not tested against live Keycloak.
- Actuator and swagger endpoints on backend-java require authentication ("everything else"); revisit if they must be public.
- `npm audit` warnings after adding `oidc-client-ts` / `react-oidc-context`; not addressed.
- Should the external-system simulators need auth? Recommendation: no.

## How to verify (do NOT run the full suite — ~35 min per language)

- Always compile: `./gradlew compileTestJava` (system-test/java), `dotnet build` (system-test/dotnet), `npx tsc --noEmit` (system-test/typescript).
- Local runs need `KEYCLOAK_URL_REAL` / `KEYCLOAK_URL_STUB` exported (java multitier 8191/8192, dotnet multitier 8291/8292, typescript multitier 8391/8392); without them identity scenarios skip and everything else gets 401.
- Docker images are shared by name across languages and go stale: `down -v` both stacks and rebuild (`docker compose -p my-shop-{stub,real} -f docker/<lang>/<arch>/docker-compose.local.{stub,real}.yml build`) right before each language's run. Compose project names are shared across languages (`my-shop-stub`/`my-shop-real`): `up` for one language recreates same-named services (e.g. `keycloak`) of another running stack, and a stopped-but-present multitier container can hold ports 3311/3312 — `down` the other stack first. A failed `up` leaves the container without published ports; use `up -d --force-recreate system`.
- Targeted Java latest (from the shop root): `export GH_OPTIVEM_CONFIG=gh-optivem-multitier-java.yaml; gh optivem system start && gh optivem system-test setup && gh optivem system-test run --suite acceptance --test <ClassName>,<ClassName>` (`gh-optivem-monolith-java.yaml` for the monolith). Targeted legacy: `GH_OPTIVEM_CONFIG=gh-optivem-multitier-java-legacy.yaml` with `--suite mod08-smoke,mod08-e2e-api,mod08-e2e-ui,mod09-smoke-stub,mod09-smoke-real` (only legacy suites using the scenario DSL). The runner prints "requested test(s) never executed" and exits 1 for class filters even when everything passed — read the Suite Results table. Equivalent configs exist for dotnet/typescript (`gh-optivem-*-dotnet*.yaml`, `*-typescript*.yaml`).
- Windows: killing `test-all.sh` does not stop the `gh`/gradle/test JVM chain; kill the `gh.exe` / `gh-optivem.exe` parent FIRST, then leftover gradle/test JVMs, playwright node processes and gradle daemons (`./gradlew --stop`), else a locked `build/test-results` breaks the next run.
- Admin UI flows: the UI "home ready" marker is the Order History link (admins do not see New Order).
- Keycloak imports the realm only on first start: recreate the keycloak container (`down -v`) after realm changes. The shared realm's redirect URIs/web origins list all multitier frontend ports (3111/3112/3211/3212/3311/3312/5173); a new stack port must be added there.

## Lessons to keep in mind

- `frontend-react` and its Docker image are SHARED by the Java, .NET and TypeScript multitier stacks. The image starts with empty `KEYCLOAK_*` and the app runs with auth DISABLED when `keycloakUrl` is empty; any compose file that wants login must set all three `KEYCLOAK_*` vars. Rule: check any change to a shared artifact (frontend image, migrations, realm file) against all three language stacks before pushing.
- Monolith readiness probes must hit the public `/health`, not the login-protected UI root (a 302 to Keycloak never yields 200). Done for all monoliths via `healthUrl`.
- Monolith specifics: Java needs PKCE added via `OAuth2AuthorizationRequestCustomizers.withPkce()` and a CSRF wrapper for `fetch()` POSTs (Bearer is CSRF-exempt), and redirects every non-API, non-Bearer request to login; .NET needs `client_id` on the end-session request; TypeScript cookie-authenticated state-changing API calls require `X-Requested-With`, `AUTH_SESSION_SECRET` defaults to a marked test value (set a real one outside local/pipeline), redirect base URL derived from the Host header.
- Safe defaults (author is new to auth): standard flows only (auth code + PKCE; no implicit, no browser password grant, no custom login form); never hand-write crypto/token code; backend validates every request and authorizes server-side; short-lived tokens in memory; no secrets in the repo beyond marked test-realm credentials; password grant/test users only in the test realm.
