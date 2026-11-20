# 2026-11-20 17:30 UTC — Visitors can sign up (UC-013 Create Customer)

## TL;DR

**Why:** The substack series now names two shop use cases that the shop doesn't document: **UC-013 Create Customer** (actor: Visitor) and **UC-014 Create Admin** (actor: Admin). A visitor can't sign up today: `docker/keycloak/shop-realm.json` has `"registrationAllowed": false`, and customers only exist because the system tests create them through `KeycloakAdminClient`. The substack articles also have Place Order read the country from the signed-in customer's profile (`CustomerProfile.country`), so a new customer needs a profile with a country.
**End result:** A visitor can sign up from the frontend, gets the `CUSTOMER` role and a customer profile with a country, and can place an order straight away. UC-013 and UC-014 are in the use case README, and UC-013 has acceptance tests.
**Unchanged:** the app's role checks (`SecurityConfig` ADMIN/CUSTOMER), how admins are created (that's `20261120-1500-per-test-admin.md`), and Place Order's country source (a separate change, see the SHOP WORK note in the substack Part 3 draft).

Origin: substack articles "Minimal Pipeline & Use Case Diagram in Legacy Code with AI" (published) and "Core Use Case Narrative & Business Rules in Legacy Code with AI" (draft): both list Create Customer as a use case started by a Visitor.

## Items

1. **Use case README:** add `UC-013 Create Customer` (Visitor) and `UC-014 Create Admin` (Admin) to `docs/shared/specs/use-cases/README.md` (diagram and table), each with a `use-case-narrative.md`. UC-014 can stay `not implemented` until the per-test-admin plan lands.
2. **Realm:** set `"registrationAllowed": true` in `docker/keycloak/shop-realm.json` and check every overlay (`acceptance-realm.jq`, `stage-realm.jq`, `prod-realm.jq`, `prod-realm-check.jq`) keeps it on, or decide explicitly that prod stays off for now. New users get the `CUSTOMER` realm role by default (default roles), never `ADMIN`.
3. **Profile with a country:** on first sign-in, a new customer has no `CustomerProfile`. Decide where the country comes from (a required field on the Keycloak registration form mapped to a user attribute, or a "complete your profile" step via UC-010 Manage Profile before the first order) and make Place Order fail clearly when the profile has no country.
4. **Frontend:** a "Sign up" link next to "Sign in" that opens the Keycloak registration page, in every frontend variant.
5. **Acceptance tests:** UC-013 Create Customer through the UI (sign up, then place an order), in Java, .NET and TypeScript. Existing tests keep creating customers through `KeycloakAdminClient` (the API path stays the fast setup).
6. **Verify:** compile all three languages, run the UC-013 tests, then the full `test-all` run.
