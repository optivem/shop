# 2026-10-07 UTC — Keycloak auth + order ownership: follow-ups

Split out of `20261007-0737-keycloak-auth-and-order-ownership-rollout` (all rollout steps complete; CI green). None of these block anything; pick up opportunistically and remove each item as it is done.

## Follow-ups

- .NET legacy UI suites failed intermittently (~1 in 9 runs, 30s Playwright timeout after Keycloak login); mitigated by `hashIterations(1)` + 90s timeout, `KeycloakUiLogin` now polls `location.href` via `WaitForFunctionAsync`. Root cause unconfirmed; the last green run held.
- .NET backend: access rules are controller attributes, not a central table; unknown routes return 404 not 401; no route-enumeration test; JWKS fetch has no retry.
- Improve `SecurityConfigTest` to enumerate every controller route (e.g. via `RequestMappingHandlerMapping`): admin-only => 403 for CUSTOMER and 401 with no token, public => open, a new route without an expected status fails. Read the existing test first. Data-dependent rules (ownership) stay in the service layer / `@PreAuthorize`.
- Expired-token and wrong-audience cases rely on Spring's standard validators; not tested against live Keycloak.
- Actuator and swagger endpoints on backend-java require authentication ("everything else"); revisit if they must be public.
- `npm audit` warnings after adding `oidc-client-ts` / `react-oidc-context`; not addressed.
- Should the external-system simulators need auth? Recommendation: no.

## Context to keep

- Endpoint rules: public `/health`; authenticated `/api/orders*`, `/cancel`, `/api/coupons` GET; ADMIN only `/api/admin/**`, `/api/coupons` POST, `/deliver`.
- Do NOT run the full system-test suite (~35 min per language); compile (`./gradlew compileTestJava`, `dotnet build`, `npx tsc --noEmit`) and run targeted classes only.
- `frontend-react`, migrations and the realm file are shared across the three language stacks; check any change against all three before pushing.
