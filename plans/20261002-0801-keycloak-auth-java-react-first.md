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

## Best-practice guardrails (author is new to auth — default to the safe, standard path)

- Use standard flows only: authorization code + PKCE for the SPA. No implicit flow, no password grant in the browser, no custom login form.
- Never write crypto/token code by hand: use Spring Security's resource-server support and a maintained OIDC client library in React (e.g. `oidc-client-ts` / `react-oidc-context`).
- Backend validates every request: signature, `iss`, `aud`, `exp`; authorize by role/claim server-side — never trust the frontend to hide things.
- Short-lived access tokens; keep tokens in memory in the SPA (not localStorage); rely on the IdP for refresh/session.
- No secrets in the repo beyond clearly-marked test-realm credentials; real config via env vars.
- Password grant / test users exist only in the test realm; a prod-like realm must not have them.
- Have the realm-import and config reviewed against the Keycloak and Spring Security docs before the pipeline depends on them.

## ▶ Next executable step (resume here)

Step 1: write a short design note (inside this plan or `docs/`) pinning the realm layout (realm name, clients, roles, users), token claims the backend relies on, and the CORS/redirect-URI settings for the React dev + compose setups. Design only; no code. After it settles, move to Step 2 (Keycloak in compose + realm import).

## Steps

Phase 1 — get it working end-to-end on Java backend + React frontend (multitier)

- [ ] Step 1: Design note — realm/clients/roles/users, claims used, redirect URIs, which endpoints become protected and with what roles.
- [ ] Step 2: Add Keycloak service + realm-import JSON to `docker/java/multitier/docker-compose.*.yml` (local + pipeline; real variant). Decide stub variant (see open questions).
- [ ] Step 3: backend-java — add `spring-boot-starter-oauth2-resource-server`, issuer/JWKS config via env vars, security filter chain, role mapping; unit/slice tests with mock JWTs.
- [ ] Step 4: frontend-react — OIDC login (authorization code + PKCE), token handling, logout, attach bearer token to API client; component tests with a mocked auth provider.
- [ ] Step 5: system-test/java — token-acquisition in the driver layer (test realm, password or client-credentials grant); keep DSL auth-agnostic; add acceptance scenarios for 401/403 and an owner-only rule.
- [ ] Step 6: CI — wire Keycloak startup/health-wait into the Java multitier acceptance/QA/prod-stage workflows; handle the cross-lang matrix (see open questions).
- [ ] Step 7: Verify end-to-end locally and in the pipeline; write the "spread recipe" (what changed, per layer).

Phase 2 — spread

- [ ] Step 8: backend-dotnet resource server + system-test/dotnet token acquisition.
- [ ] Step 9: backend-typescript resource server + system-test/typescript token acquisition.
- [ ] Step 10: Re-enable/complete the cross-lang matrix for all combinations; remove any temporary gating.
- [ ] Step 11: Monolith variants (java, dotnet, typescript) — decide how the monolith's bundled UI does login (see open questions).
- [ ] Step 12: Cloud/prod-stage: managed or hardened IdP, secrets, TLS (separate from local/pipeline Keycloak).

## Open questions

- **Frontend sharing (inferred):** `system/multitier/frontend-react` appears to be a single frontend shared by all backend languages. Recommendation: yes — do the React work once in Phase 1; Phase 2 then only touches backends and test drivers.
- **Cross-lang matrix (test-lang × system-lang):** a Java auth-protected system breaks dotnet/typescript test drivers until they gain token acquisition. Options: (a) gate/exclude the Java-auth multitier system from the matrix until Step 10; (b) add token acquisition to all test langs early. Recommendation: (a) — it keeps Phase 1 small; make the exclusion explicit and tracked by Step 10 so it can't linger silently.
- **Stub variant:** what replaces Keycloak in `stub` compose files? Recommendation: run real Keycloak in both variants at first (it is cheap and avoids a second implementation); revisit only if startup time hurts the pipeline.
- **Token acquisition grant for tests:** password grant vs client-credentials with a service-account user. Recommendation: password grant on a dedicated test realm (needs real user roles); never enabled in prod.
- **Keycloak startup time in CI:** needs a health-wait step and realm import in `start-dev` mode; confirm the budget in the acceptance stage.
- **Authorization rules to demonstrate:** which concrete requirement drives the ATDD example? Recommendation: "a customer can only see their own orders; admin can see all".
- **Monolith UI login:** server-rendered/bundled UI may need a different flow (OIDC login via the backend vs SPA). Defer to Step 11.
- **Legacy vs latest test suites:** do both get auth, or only latest? Recommendation: confirm in Step 1.
