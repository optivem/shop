# 2026-10-02 UTC — Order ownership, admin/customer separation, and identities in the test DSL

## TL;DR

**Why:** After adding Keycloak authentication (see `plans/20261002-0801-keycloak-auth-java-react-first.md`), any logged-in customer can see every order, and admins can place orders. The intended model is: customers see only their own orders, admins see all orders but do not place them, and the acceptance tests can express "which user does this".
**End result:** Orders have an owner; backend, UI and tests enforce and verify owner/admin rules; the test DSL has first-class identities (customer, another customer, admin) so isolation scenarios read as Given/When/Then.

## Outcomes

- Each order records its owner (the token `sub`); existing orders keep a null owner and are visible to admins only.
- Placing an order requires the CUSTOMER role; `admin1` has only ADMIN (no CUSTOMER).
- A customer's order history and order lookup return only their own orders (another customer's order => 404); an admin's history lists all customers' orders, with the customer shown.
- Customers may view/cancel their own orders; admins may view and cancel any order; delivery stays admin-only.
- The React UI hides "place order" from admins and shows the customer column in the admin order list.
- The test DSL supports identities (e.g. given a customer / another customer / an admin; when acting as X); the Playwright driver logs in as the identity the scenario needs instead of always admin1.
- New acceptance scenarios (written first): customer A cannot see customer B's order; admin sees all orders; admin cannot place an order; customer cannot see others' history.

## ▶ Next executable step (resume here)

Design only: confirm the open questions below with the author, then draft the acceptance scenarios (Step 1) in the repo's existing DSL style before any implementation. Start by reading how the latest acceptance DSL expresses givens/when (`system-test/java`, latest `acceptance` tests and the use-case DSL) and how `MyShopApiClient` / `KeycloakUiLogin` currently choose identities (`ApiIdentity`, `TestUser`).

## Steps

- [ ] Step 1: Acceptance scenarios first (ATDD): write the isolation/admin scenarios and the DSL identity vocabulary in Java latest; they fail (red) until the backend rules exist.
- [ ] Step 2: Realm: remove CUSTOMER from `admin1`; add a second customer (`customer2`) to `docker/keycloak/shop-realm.json`.
- [ ] Step 3: DB migration (shared `system/db/migrations`): add nullable `owner` column (and index) to orders; confirm dotnet/typescript backends tolerate the extra nullable column.
- [ ] Step 4: backend-java: set owner from the token on order creation; owner/admin filtering on history and lookup; CUSTOMER-only on place order; admin-or-owner on view/cancel; tests (unit, slice, component).
- [ ] Step 5: frontend-react: hide place-order for admins; show customer in admin order list; tests. (Frontend image is shared by all three stacks — check the auth-disabled mode still works and verify against the dotnet/typescript stacks before pushing.)
- [ ] Step 6: Test drivers: API client and Playwright driver take the identity from the scenario (DSL) instead of operation-based defaults / always admin1; keep opt-in behaviour (no Keycloak URL => no auth).
- [ ] Step 7: Verify locally with `gh optivem` (full latest + legacy suites), then push per milestone and watch CI; confirm dotnet/typescript pipelines unaffected.
- [ ] Step 8: Update the Keycloak plan's Phase 2 recipe to include the ownership model for .NET/TypeScript (or schedule it after Phase 2 per the sequencing decision).

## Open questions

- **Sequencing vs Keycloak Phase 2:** do this in Java before spreading auth to .NET/TypeScript, or after? Recommendation: before, so the other languages copy a finished model once.
- **Another customer's order:** 404 or 403? Recommendation: 404 (do not reveal that the order exists).
- **Can admins cancel orders?** Recommendation: yes (support use case); delivery stays admin-only.
- **Existing orders with no owner:** recommendation: leave owner null, admin-only visibility.
- **Order history for admin:** full list with customer shown; pagination/filter needed now? Recommendation: not in this plan.
- **Legacy suites:** do the legacy (mod02–mod11) suites get identity support or keep the default customer/admin choice? Recommendation: keep defaults for legacy, switch only latest to DSL identities; legacy only needs to keep passing with admin1 no longer having CUSTOMER (check mod UI tests that place orders).
- **Monoliths and other languages:** follow in the Keycloak plan's Phase 2 (Steps 8–11) once the Java model is settled.
