# 2026-10-07 UTC — Keycloak auth + order ownership: follow-ups

Split out of `20261007-0737-keycloak-auth-and-order-ownership-rollout` (all rollout steps complete; CI green). None of these block anything; pick up opportunistically and remove each item as it is done.

## Follow-ups

- .NET legacy UI suites failed intermittently (~1 in 9 runs, 30s Playwright timeout after Keycloak login); mitigated by `hashIterations(1)` + 90s timeout, `KeycloakUiLogin` now polls `location.href` via `WaitForFunctionAsync`. Root cause unconfirmed; the last green run held.
- .NET backend: access rules are controller attributes, not a central table; unknown routes return 404 not 401; no route-enumeration test; JWKS fetch has no retry.
- Improve `SecurityConfigTest` to enumerate every controller route (e.g. via `RequestMappingHandlerMapping`): admin-only => 403 for CUSTOMER and 401 with no token, public => open, a new route without an expected status fails. Read the existing test first. Data-dependent rules (ownership) stay in the service layer / `@PreAuthorize`.
- Expired-token and wrong-audience cases rely on Spring's standard validators; not tested against live Keycloak. Add them to `SecurityConfigTest` (component level), not to acceptance tests.
- UI ownership check is weak: on the UI channel, "Order X does not exist." is synthesized by the driver (`ensureOnOrderDetailsPage`) when the order isn't found in A's history search; it never navigates directly to B's order-details URL. A UI route leaking an order via direct URL would go uncaught. Make the UI driver open the order-details URL directly.
- Access-denial acceptance tests are API-only: `MyShopUiDriver.actAs` throws for ANONYMOUS; the UI driver can't report a denied admin page as `PERMISSION_DENIED` (publish coupon, browse coupons, deliver order, admin placing an order); `MyShopUiDriver.browseOrderHistory` throws, so history ownership tests are API-only. Add UI support, then widen the `@Channel` on those tests.
- Default identity is resolved two ways: `MyShopApiClient.tokenFor` uses a path-based `isAdminOnly` table that duplicates the security config's route rules, while the UI driver picks per operation. Unify (e.g. per operation in both drivers).
- Actuator and swagger endpoints on backend-java require authentication ("everything else"); revisit if they must be public.
- `npm audit` warnings after adding `oidc-client-ts` / `react-oidc-context`; not addressed.
- Should the external-system simulators need auth? Recommendation: no.

## Context to keep

- Endpoint rules: public `/health`; CUSTOMER only `POST /api/orders`, `/new-order`; ADMIN only `/api/admin/**`, `/api/coupons` GET and POST, `/deliver`, `/admin-coupons`; everything else authenticated (per Java `SecurityConfig` and `BrowseCouponsNegativeTest`).
- Do NOT run the full system-test suite (~35 min per language); compile (`./gradlew compileTestJava`, `dotnet build`, `npx tsc --noEmit`) and run targeted classes only.
- `frontend-react`, migrations and the realm file are shared across the three language stacks; check any change against all three before pushing.
