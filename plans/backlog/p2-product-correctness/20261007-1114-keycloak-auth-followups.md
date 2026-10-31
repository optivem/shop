# 2026-10-07 UTC — Keycloak auth + order ownership: follow-ups

Split out of `20261007-0737-keycloak-auth-and-order-ownership-rollout` (all rollout steps complete; CI green). None of these block anything; pick up opportunistically and remove each item as it is done.

## Follow-ups

- .NET legacy UI suites failed intermittently (~1 in 9 runs, 30s Playwright timeout after Keycloak login); mitigated by `hashIterations(1)` + 90s timeout, `KeycloakUiLogin` now polls `location.href` via `WaitForFunctionAsync`. Root cause unconfirmed; the last green run held.
- .NET backend: access rules are controller attributes, not a central table; unknown routes return 404 not 401; no route-enumeration test; JWKS fetch has no retry.
- Java `SecurityConfig` (monolith + backend-java): the admin rule for coupons matches `HttpMethod.GET` only, but Spring MVC routes HEAD to GET handlers, so `HEAD /api/coupons` as CUSTOMER returns 200 and runs `browseCoupons` instead of 403. Likewise `/health` needs auth on HEAD. Found by the route-enumeration work in `SecurityConfigTest`; unfixed because it is a production change. Likely fix: drop the method from the GET coupon matcher or add HEAD, then add the HEAD case to `SecurityConfigTest`.
- Default identity is resolved two ways: `MyShopApiClient.tokenFor` uses a path-based `isAdminOnly` table that duplicates the security config's route rules, while the UI driver picks per operation. Unify (e.g. per operation in both drivers).
- Actuator and swagger endpoints on backend-java require authentication ("everything else"); revisit if they must be public.
- `npm audit` warnings after adding `oidc-client-ts` / `react-oidc-context`; not addressed.
- Should the external-system simulators need auth? Recommendation: no.

## Context to keep

- Endpoint rules: public `/health`; CUSTOMER only `POST /api/orders`, `/new-order`; ADMIN only `/api/admin/**`, `/api/coupons` GET and POST, `/deliver`, `/admin-coupons`; everything else authenticated (per Java `SecurityConfig` and `BrowseCouponsNegativeTest`).
- Do NOT run the full system-test suite (~35 min per language); compile (`./gradlew compileTestJava`, `dotnet build`, `npx tsc --noEmit`) and run targeted classes only.
- `frontend-react`, migrations and the realm file are shared across the three language stacks; check any change against all three before pushing.
