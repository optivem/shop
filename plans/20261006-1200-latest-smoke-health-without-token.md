# 2026-10-06 UTC — Should the latest smoke test prove that /health needs no token?

## TL;DR

**Why:** The Java latest acceptance suite has no check that `/health` works without a token. `MyShopSmokeTest` (`scenario.assume().myShop().shouldBeRunning()`) goes through the DSL with the default identity, so it sends a customer token and would pass even if `/health` demanded one. The standalone `HealthTest` that covered this was deleted in the order-ownership work (it duplicated backend and legacy coverage and did not fit the one-class-per-use-case layout), so the idea of proving it in the smoke test was parked here.
**End result:** A decision, recorded, on whether the latest smoke test should assert unauthenticated health; if yes, the smoke test (or a sibling) does it without breaking UI-channel smoke or systems that run without Keycloak.

## Outcomes

- A recorded decision: keep relying on the existing coverage, or make the latest smoke check anonymous.
- If the answer is yes: a smoke scenario that calls health as anonymous on the API channel only, running correctly both with and without a Keycloak URL.

## ▶ Next executable step (resume here)

Design only: answer the open questions below with the author, then (if yes) implement in `system-test/java`. Start by reading `latest/smoke/system/MyShopSmokeTest.java` (channels `UI` and `API`), `WhenImpl`/`AssumeImpl.actingAsAnonymous()`, `MyShopApiDriver.actAs`, and how `MyShopApiClient` behaves when no Keycloak URL is set (no token source is installed, so anonymous and default behave the same).

## Steps

- [ ] Step 1: Decide (see open questions). If "keep as is", delete this plan.
- [ ] Step 2 (only if yes): add an API-only smoke scenario that asserts health as anonymous. Do not change the UI-channel smoke: `MyShopUiDriver.actAs(ANONYMOUS)` throws `UnsupportedOperationException` because the UI requires a logged-in user.
- [ ] Step 3 (only if yes): confirm it passes locally with and without `KEYCLOAK_URL_*` set (rebuild the docker images first), then commit.

## Existing coverage of "health needs no token" (for the decision)

- Backend: `SecurityConfigTest.healthIsPublic` (slice) and `AuthorizationComponentTest.healthIsPublic` (component, real filter chain, no token).
- Legacy Java: `mod02` `MyShopApiSmokeTest` (raw HTTP, no token) and `mod04` `ApiAuthorizationTest.shouldNotRequireTokenForHealth` (explicit anonymous).
- Pipeline: the CI and compose readiness probes call the public `/health` unauthenticated (verify the individual probe files if relying on this).
- Not covered: the latest-cycle system tests.

## Open questions

- **Is the existing coverage enough?** Recommendation: yes — the backend tests pin the security rule at the layer that owns it, and the probes would fail loudly if `/health` became protected. If the author agrees, close the plan.
- **If more is wanted, where does it go?** Recommendation: a separate API-only smoke test next to `MyShopSmokeTest` (not changing it), so the UI smoke and the no-Keycloak run are unaffected.
- **Should anonymous health also be checked on a system with Keycloak disabled?** Recommendation: no — without Keycloak no token is ever sent, so the check proves nothing there; mark it `@RequiresKeycloak` like the other identity scenarios.
- **Same question for the .NET and TypeScript latest suites?** Recommendation: decide Java first and copy the outcome in the Keycloak plan's Step 13 work.
