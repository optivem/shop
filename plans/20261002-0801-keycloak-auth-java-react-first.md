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

Phase 1 is implemented and verified locally (Keycloak in the 4 Java multitier compose files, backend-java resource server, React OIDC login, Java system-test token + Playwright UI login for latest and legacy suites, CI env wiring, cross-lang exclusion). Remaining: (a) confirm the pushed pipeline is green for multitier-java (acceptance, acceptance-legacy, qa, prod stages) and the cross-lang matrix; (b) manual browser login by the author; (c) then start Phase 2 with Step 8 (backend-dotnet resource server + system-test/dotnet token acquisition + Playwright login), using the Phase 1 diff as the recipe.

## Steps

Phase 1 — get it working end-to-end on Java backend + React frontend (multitier)


Phase 2 — spread

- [ ] Step 8: backend-dotnet resource server + system-test/dotnet token acquisition.
- [ ] Step 9: backend-typescript resource server + system-test/typescript token acquisition.
- [ ] Step 10: Re-enable/complete the cross-lang matrix for all combinations; remove any temporary gating.
- [ ] Step 11: Monolith variants (java, dotnet, typescript) — decide how the monolith's bundled UI does login (see open questions).
- [ ] Step 12: Cloud/prod-stage: managed or hardened IdP, secrets, TLS (separate from local/pipeline Keycloak).

## Phase 1 follow-ups noted during verification

- Actuator and swagger endpoints on backend-java now require authentication ("everything else"); revisit if they must be public.
- `npm audit` reported warnings after adding oidc-client-ts / react-oidc-context; not addressed.
- Cloud stage workflows (`*-cloud.yml`) and QA/prod stages for java multitier not yet reviewed for Keycloak (Step 12).
- Expired-token and wrong-audience cases rely on Spring's standard validators; not tested against live Keycloak.

## Open questions

None for Phase 1 — all resolved (2026-10-02):
- Cross-lang matrix: gate the java-multitier (auth) system out of cross-lang-system-verification until Step 10 re-enables it.
- Legacy and latest system-test suites both get token acquisition in Phase 1 (logic in the shared driver/client layer).
- Frontend-react is shared across backends (Phase 2 only touches backends + test drivers).
- Keycloak startup time in CI: verify during Step 6 (health-wait step); not a blocker.
- Deferred to Phase 2: monolith UI login flow (Step 11), prod IdP (Step 12).
