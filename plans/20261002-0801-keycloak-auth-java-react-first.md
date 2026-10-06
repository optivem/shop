# 2026-10-02 08:01:00 UTC — Keycloak authentication: Java backend + React frontend first, then spread


## TL;DR

**Why:** The shop has no authentication. We want a realistic, industry-standard setup (dockerised Keycloak as IdP, OIDC, backends validating JWTs) that also serves as teaching material and an ATDD example.
**End result:** The multitier shop (backend-java + frontend-react) logs users in via Keycloak and enforces JWT-based authorization, with system tests and CI green. The pattern is then replicated to the .NET and TypeScript backends and the monolith variants.

## Outcomes

- Keycloak runs as a container in the multitier Java compose files, with a realm-import JSON (realm, public client for the React app, confidential/test client for system tests, test users with roles e.g. CUSTOMER / ADMIN).
- React frontend performs OIDC authorization-code + PKCE login/logout and attaches the access token to backend calls.
- backend-java is an OAuth2 resource server (Spring Security): validates signature (JWKS), issuer, audience, expiry; maps roles to authorization rules; unauthenticated calls get 401, forbidden calls 403.
- System-test drivers (Java first) acquire tokens programmatically against the test realm in the driver layer; the DSL stays free of auth concerns.
- The cross-lang-system-verification matrix stays green throughout the rollout (no silent breakage of dotnet/typescript test langs).
- A documented, repeatable recipe for spreading to backend-dotnet, backend-typescript, then the monolith variants.
- Monolith end state (Step 11): opening a monolith UI redirects to Keycloak; after login the server holds a session cookie (tokens never reach browser JS); UI pages and API routes enforce the same role rules as multitier (public `/health`, authenticated orders, ADMIN-only admin/coupon-create/deliver; 401/403 semantics for API). Monolith system tests acquire tokens in the driver layer, UI tests log in via the Keycloak page. Unchanged: multitier setup, shared realm (plus new monolith clients), DSL free of auth concerns, cross-lang matrix green.

## Decisions settled (2026-10-02)

- IdP: Keycloak. Backend: in-app JWT validation (resource server). SPA login: authorization code + PKCE (BFF deferred, can be adopted later without backend changes). Test tokens: dedicated test realm, fetched in the driver layer. Browser token storage: in memory.
- Rollout: Java backend + React frontend first (multitier), then spread to .NET/TypeScript and monoliths.

## Author answers (2026-10-02) — design CONFIRMED

- Phase 1 is role-based only; order owner-scoping is a separate follow-up plan.
- Endpoint rules as drafted below.
- Push per verified milestone; stop if CI breaks.
- Real Keycloak in both stub and real compose variants (no token stub).
- Execution: batch mode, subagents for backend-java / frontend-react / system-test-java after the contract is fixed; stop only for the final manual browser login or unexpected problems.

## Step 1 — design note (confirmed)

Grounded in `system/multitier/backend-java` controllers and `docker/java/multitier/docker-compose.local.real.yml` (frontend :3111, backend :8111, postgres, flyway, simulators).

**Keycloak realm `shop`** (new compose service `keycloak`, `start-dev --import-realm`, realm JSON mounted from a shared dir, e.g. `docker/keycloak/shop-realm.json`)
- Clients: `shop-frontend` (public, PKCE S256 required, redirect URIs `http://localhost:3111/*`, web origin `http://localhost:3111`); `shop-system-test` (direct-access/password grant enabled, test realm only).
- Realm roles: `CUSTOMER`, `ADMIN`. Test users: `customer1` (CUSTOMER), `admin1` (ADMIN) with clearly-marked test passwords.
- Audience mapper so access tokens carry `aud` = `shop-backend`; roles exposed via `realm_access.roles`.

**Backend endpoint protection** (current endpoints: `/health`, `/api/orders` GET/POST, `/api/orders/{n}` GET, `/cancel`, `/deliver`, `/api/coupons` GET/POST, `/api/admin/recall/{sku}`)
- Public: `/health`.
- Authenticated (any role): `/api/orders` and `/api/orders/{n}` (read/create), `/cancel`.
- ADMIN only: `/api/admin/**`, `/api/coupons` POST, `/deliver`.
- `/api/coupons` GET: authenticated.

**Open design points to confirm**
- Orders currently have no owner. The "customer sees only their own orders" rule needs an `owner` (token `sub`) column + Flyway migration — larger scope. Recommendation: Phase 1 ships role-based rules only; owner-scoping becomes its own follow-up plan.
- Whether the external-system simulators need any auth (recommendation: no).

## Best-practice guardrails (author is new to auth — default to the safe, standard path)

- Use standard flows only: authorization code + PKCE for the SPA. No implicit flow, no password grant in the browser, no custom login form.
- Never write crypto/token code by hand: use Spring Security's resource-server support and a maintained OIDC client library in React (e.g. `oidc-client-ts` / `react-oidc-context`).
- Backend validates every request: signature, `iss`, `aud`, `exp`; authorize by role/claim server-side — never trust the frontend to hide things.
- Short-lived access tokens; keep tokens in memory in the SPA (not localStorage); rely on the IdP for refresh/session.
- No secrets in the repo beyond clearly-marked test-realm credentials; real config via env vars.
- Password grant / test users exist only in the test realm; a prod-like realm must not have them.
- Have the realm-import and config reviewed against the Keycloak and Spring Security docs before the pipeline depends on them.

## ▶ Next executable step (resume here)

**Execution style (author decision 2026-10-03): no `/clear` hand-offs.** Run the rest of this plan in ONE session and keep the main context small by delegating to subagents (default, non-worktree): one for each long wait/monitor (release publish, `meta-prerelease-stage` run, ~1h+), one for each failure diagnosis (`gh-monitor` / `general-purpose`, returning only the root cause + proposed fix). The main session only dispatches, reads the short verdicts, and applies/approves fixes. Do not end the session with a "Next session: /clear" block unless the plan is truly blocked on the author.

Step 11 is DONE for all three monoliths; TS monolith latest+legacy acceptance green (run 37124202413). `cross-lang-system-verification` is now a reusable workflow only (no `workflow_dispatch`); verify it via `gh workflow run meta-prerelease-stage.yml --ref main` (~1h+) after the probe fixes below are pushed (wait for the commit-stage runs first).

Last scheduled meta-prerelease-stage (SHA 64bc2ad, before TS monolith) failed on: cross-lang `* tests vs monolith java` and drift `monolith — Java → TS schema interop` (readiness probe of the login-protected UI root `/` never gets 200), plus `prerelease-pipeline-monolith-java [level:local]` (gh-optivem `WaitForURL` in `internal/build/runner/health.go` requires 200 on `systems.yaml` components[0] = UI root; fails with "Monolith ... not ready after 30 attempts"). Fixed in shop (uncommitted→committed this session): cross-lang endpoint list skips the monolith UI root; drift.yml monolith probes use only the API `/health`.

Done this session (uncommitted until review): gh-optivem `Component.HealthUrl` (optional, probes prefer it over `url`; tested) and `healthUrl: <ui>/health` on the `Monolith` UI entry in the java/dotnet/typescript monolith `systems.yaml`.

Run 37126180862 (meta-prerelease-stage, on released gh-optivem v1.6.86) findings, fixed in shop: (a) Java monolith local stage — the probe's GET `/` has no Accept header so Spring returned 401 instead of the login redirect (.NET/TS redirect unconditionally); `SecurityConfig` now redirects every non-API/non-Bearer request to login (+ test). (b) .NET UI timeouts (`KeycloakUiLogin.LoginAsync` `WaitUntil=Commit` race, flaky ~3 of 4 runs) — dropped `Commit`. Both unverified until the next meta-prerelease-stage run. Order matters: shop must be fully green first; the shop meta pipeline then triggers gh-optivem itself (do NOT dispatch gh-release-stage manually). `healthUrl` stays committed in gh-optivem/shop (harmless, keeps `url` as the UI base).

Run 37133721795 (after 6c2af459): Java monolith local stage + dotnet-vs-multitier-java now green. Still failing: .NET UI timeout, now inside `WaitForURLAsync`'s internal `WaitForLoadStateAsync` (dotnet vs multitier typescript cross-lang; `multitier-dotnet-acceptance-stage-legacy` mod03-e2e). Fix applied: `KeycloakUiLogin.LoginAsync` polls `location.href` via `WaitForFunctionAsync` instead of `WaitForURLAsync` (no load-state wait). Unverified.

RESULT: meta-prerelease-stage run 37142307930 (SHA 77f51479) is fully GREEN (all local/cross-lang/pipeline jobs, tag-meta-rc ran). The .NET `KeycloakUiLogin` poll fix and the Java monolith login-redirect fix both held. The gh-acceptance-stage dispatch into optivem/gh-optivem lives in `meta-release-stage.yml` (line ~302), NOT in the prerelease pipeline.

Next: author decides when to run shop `meta-release-stage` (release action — ask first; read the workflow before running), which dispatches gh-acceptance-stage in gh-optivem; then `gh-release-stage` promotes the gh-optivem RC with `healthUrl` (release action — ask first). After that, Step 12 (cloud/prod IdP) is design work: draft with `/create-plan`.

(Superseded) First action next session: (1) once gh-optivem is committed/released and the shop commit-stage runs finish, dispatch `gh workflow run meta-prerelease-stage.yml --ref main` (~1h+) and confirm green, including `prerelease-pipeline-monolith-java [level:local]` and the .NET/TS monolith local stages; (2) if any monolith still fails readiness, diagnose that stage specifically.

Next unit after that: Step 12 (cloud/prod-stage IdP) is design work — draft it with `/create-plan`. Order-ownership plan `plans/20261002-1209-order-ownership-and-dsl-identities.md` is still pending.

## Steps

Phase 1 — get it working end-to-end on Java backend + React frontend (multitier)


Phase 2 — spread

- [ ] Step 13: Order ownership model for the remaining backends (the Java multitier model is done, see the 2026-10-02 order-ownership work): .NET and TypeScript multitier backends and the three monoliths copy it. Recipe: set `orders.owner` (token `sub`) and `owner_name` (`preferred_username`) on placement (columns already exist in the shared migration `V20261005190000__add_order_owner.sql`); place order requires CUSTOMER (`admin1` now holds only ADMIN, `customer2` is a second customer); customers' history and lookup return only their own orders and another customer's order is 404 (no existence leak); admins see all orders (history items carry `customer`) and may cancel any order; deliver stays admin-only; orders with a null owner are admin-only. Test drivers for each language need the DSL identities and the UI default-identity switch used in Java (the UI switch is already ported to the dotnet and typescript multitier test kits; the DSL identities and order-history use case are not). Monolith UIs must hide place-order from admins like the shared React frontend does.
- [ ] Step 14: Retire the Keycloak opt-in guard once every system under test always has Keycloak (after Step 13 and the monoliths): delete `@RequiresKeycloak`, `KeycloakRequirementExtension` and `isKeycloakConfigured()` from the Java latest acceptance base, and make a missing `KEYCLOAK_URL` a hard failure in CI instead of a silent skip (a skipped identity scenario hides a misconfigured pipeline). Decide first whether runs without auth (student exercises, template projects, deliberately unauthenticated legacy suites) are still wanted; if so keep the guard and only make CI strict. Also: the .NET and TypeScript backends must return the same 401/403 messages as backend-java ("Authentication is required", "You do not have permission to perform this action") because the Java DSL asserts on them, and `UserIdentity.CUSTOMER_COUNT` must stay in sync with `TestUser` when customers are added.
- [ ] Step 12: Cloud/prod-stage: managed or hardened IdP, secrets, TLS (separate from local/pipeline Keycloak).

## Phase 1 follow-ups noted during verification

- (Mitigated by hashIterations(1) + 90s timeout; root cause unconfirmed) .NET legacy UI suites (mod02 smoke, mod06/07 e2e-ui) failed intermittently (~1 in 9 runner runs) with a 30s Playwright timeout after the Keycloak login; not reproducible when looping the tests directly. Needs diagnosis (suspect first login on a cold Keycloak or load under the runner).
- .NET backend: access rules are controller attributes, not a central table, and unknown routes return 404 rather than 401; no route-enumeration test yet (see the SecurityConfig follow-up above). JWKS fetch has no retry.
- Shared realm `docker/keycloak/shop-realm.json` redirect URIs/web origins now list all multitier frontend ports (3111/3112/3211/3212/3311/3312/5173); a new stack port must be added there. Keycloak only imports the realm on first start, so recreate the keycloak container after changing it.
- Local system-test runs of dotnet/typescript need KEYCLOAK_URL_REAL / KEYCLOAK_URL_STUB exported (8291/8292, 8391/8392).
- Actuator and swagger endpoints on backend-java now require authentication ("everything else"); revisit if they must be public.
- `npm audit` reported warnings after adding oidc-client-ts / react-oidc-context; not addressed.
- Cloud stage workflows (`*-cloud.yml`) and QA/prod stages for java multitier not yet reviewed for Keycloak (Step 12).
- Access rules live centrally in `SecurityConfig` (URL rules + `anyRequest().authenticated()`), kept deliberately: secure-by-default, single audit point. Risk: a renamed/added route can silently stop matching a rule (fails closed, but unnoticed). Improvement: make `SecurityConfigTest` verify every controller route — admin-only endpoints return 403 for CUSTOMER and 401 with no token, public ones are open, and a new route without an expected status fails the test (e.g. enumerate routes from `RequestMappingHandlerMapping`). First read the existing test to see what it already covers. Data-dependent rules (order ownership) go in service layer / `@PreAuthorize` in the ownership plan, not in URL rules.
- Expired-token and wrong-audience cases rely on Spring's standard validators; not tested against live Keycloak.

- Java monolith: Spring does not send PKCE for confidential clients by default; the realm client requires S256, so `SecurityConfig` adds it via `OAuth2AuthorizationRequestCustomizers.withPkce()` (without it Keycloak returns `Missing parameter: code_challenge_method`). Pages' `fetch()` POSTs get the CSRF token from the XSRF-TOKEN cookie via a wrapper in `layout.html`; Bearer requests are CSRF-exempt.
- CI lesson: the workflows' "MyShop UI" readiness probe (`http://localhost:311x/`) expects 200, but a login-protected `/` returns 302 to Keycloak and the wait times out. Point the monolith UI probe at the public `/health` in every acceptance/qa/prod workflow of each monolith (done for Java). Acceptance stages are on an hourly schedule: use `gh workflow run <file> --ref main` to verify sooner.
- .NET monolith: OIDC uses `ResponseMode=Query` plus Lax correlation/nonce cookies (default `form_post` needs SameSite=None+Secure, which breaks over plain HTTP); without saved tokens Keycloak needs `client_id` on the end-session request (added in `OnRedirectToIdentityProviderForSignOut`). Local compose projects share names (`my-shop-stub`/`my-shop-real`) across languages: `up` for one language recreates same-named services (e.g. `keycloak`) of another running stack, so `down` the other stack first.
- TypeScript monolith (Next.js): no Auth.js; uses `openid-client` (code flow + PKCE S256, state, nonce, explicit endpoints because browser/internal URLs differ) and `jose` (encrypted httpOnly session cookie; Bearer JWT validation via JWKS, issuer, audience). All rules live in `src/lib/auth/policy.ts` and are enforced by `src/middleware.ts` (Node runtime, secure-by-default matcher). Cookie-authenticated state-changing API calls require an `X-Requested-With` header (added by a fetch wrapper in `layout.tsx`); Bearer calls are exempt. The redirect base URL is derived from the Host header (port 3311/8311 both map to the container). `AUTH_SESSION_SECRET` defaults to a marked test value; set a real one outside local/pipeline. Unit tests cover the policy only; middleware/login are covered by the system tests.
- Local compose projects share names across stacks: a stopped-but-present `my-shop-real-frontend-1` etc. from the multitier stack holds ports 3311/3312 — stop it before starting a monolith stack. A failed `up` leaves the container without published ports; use `up -d --force-recreate system`.
- Keycloak only imports the realm on first start: recreate the keycloak container (`down -v`) after realm changes such as the new `shop-monolith` client.

## Lesson from CI (2026-10-02)

- `frontend-react` and its Docker image are SHARED by the Java, .NET and TypeScript multitier stacks. Phase 1 initially made Keycloak config mandatory, which broke the .NET/TypeScript pipelines (nginx `unknown "keycloak_url" variable`). Fixed: the image starts with empty KEYCLOAK_* (defaults in the Dockerfile) and the app runs with auth DISABLED when keycloakUrl is empty. Any compose file that wants login must set all three KEYCLOAK_* vars (Phase 2 sets them for .NET/TypeScript when their backends validate tokens).
- Rule: any change to a shared artifact (frontend image, db migrations, realm file) must be checked against all three language stacks before pushing.

## Resolved decisions

- **Monolith UI login (Step 11), resolved 2026-10-02:** server-side OIDC. Each monolith (Java/Thymeleaf, .NET/Razor Pages, TypeScript/Next.js) is a confidential Keycloak client using authorization code + PKCE server-side with a session cookie (Spring `oauth2Login` / ASP.NET OpenIdConnect / Auth.js). UI pages use the session; API routes also accept Bearer JWTs (same role rules as multitier). Rationale: standard pattern for server-rendered apps, tokens stay off the browser, realm reused with one new client per monolith. Alternatives rejected: in-browser oidc-client-ts on server-rendered pages (awkward, tokens in JS); auth proxy/BFF in front (hides in-app security teaching material, extra component).
  - Consequences: add `shop-monolith-*` confidential client(s) + redirect URIs to `docker/keycloak/shop-realm.json` (client secret = clearly-marked test value, env-supplied); monolith system-test drivers acquire tokens as in multitier; UI tests drive the Keycloak login page. Check every change against all three language stacks before pushing.

## Open questions

None for Phase 1 — all resolved (2026-10-02):
- Cross-lang matrix: gate the java-multitier (auth) system out of cross-lang-system-verification until Step 10 re-enables it.
- Legacy and latest system-test suites both get token acquisition in Phase 1 (logic in the shared driver/client layer).
- Frontend-react is shared across backends (Phase 2 only touches backends + test drivers).
- Keycloak startup time in CI: verify during Step 6 (health-wait step); not a blocker.
- Monolith UI login flow (Step 11): resolved — see Resolved decisions. Deferred to Phase 2: prod IdP (Step 12).
