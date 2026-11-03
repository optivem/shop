# 2026-10-07 UTC — Keycloak auth + order ownership: follow-ups

Split out of `20261007-0737-keycloak-auth-and-order-ownership-rollout` (all rollout steps complete; CI green). None of these block anything; pick up opportunistically and remove each item as it is done.

▶ Next (2026-11-03, meta U9): fix the Java/TypeScript HEAD bypass, then run the non-breaking `npm audit fix`. The .NET route-table item stays deferred.

## Decisions (Q-F, 2026-11-03): resolved

1. Actuator/swagger on backend-java: **not public**; leave them authenticated. Item closed.
2. Simulators need auth: **no**. Item closed.
3. `npm audit` warnings: **`npm audit fix` without `--force`** (non-breaking only) in the npm projects that pull in `oidc-client-ts` / `react-oidc-context`; ignore the rest.
4. .NET intermittent UI flake: **treated as mitigated**. Bullet closed.
5. .NET central route table / JWKS retry: **deferred**; not needed for a teaching template.

## Follow-ups

- ⏳ Deferred: not needed for a teaching template (2026-11-03). .NET backend: access rules are controller attributes, not a central table; unknown routes return 404 not 401; no route-enumeration test; JWKS fetch has no retry.
- Java `SecurityConfig` (monolith + backend-java): the admin rule for coupons matches `HttpMethod.GET` only, but Spring MVC routes HEAD to GET handlers, so `HEAD /api/coupons` as CUSTOMER returns 200 and runs `browseCoupons` instead of 403. Likewise `/health` needs auth on HEAD. Found by the route-enumeration work in `SecurityConfigTest`; unfixed because it is a production change. Likely fix: drop the method from the GET coupon matcher or add HEAD, then add the HEAD case to `SecurityConfigTest`.
- `npm audit` warnings after adding `oidc-client-ts` / `react-oidc-context`; not addressed.

## Context to keep

- Endpoint rules: public `/health`; CUSTOMER only `POST /api/orders`, `/new-order`; ADMIN only `/api/admin/**`, `/api/coupons` GET and POST, `/deliver`, `/admin-coupons`; everything else authenticated (per Java `SecurityConfig` and `BrowseCouponsNegativeTest`).
- Do NOT run the full system-test suite (~35 min per language); compile (`./gradlew compileTestJava`, `dotnet build`, `npx tsc --noEmit`) and run targeted classes only.
- `frontend-react`, migrations and the realm file are shared across the three language stacks; check any change against all three before pushing.
