# 2026-10-02 UTC — Order ownership, admin/customer separation, and identities in the test DSL

> 🤖 **Picked up by agent** — `ValentinaLaptop` at `2026-10-05T18:22:24Z`

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

Step 1 (rest) + Step 6 together: design and add the DSL identity vocabulary in Java latest (given a customer / another customer / an admin; when acting as X), thread the identity through the API driver and the UI driver (UI already has `MyShopUiClient.switchUser`; API client has `ApiIdentity`, needs an OTHER_CUSTOMER / `customer2` identity and `TestUser.CUSTOMER2`), then write the isolation scenarios, move all `ApiAuthorizationTest` cases into DSL scenarios and delete that class (customer A cannot see customer B's order; admin sees all orders; customer cannot see others' history) — they should pass against the already-implemented backend-java rules. Start by reading `testkit/dsl/core/scenario/given/GivenImpl.java`, `when/WhenImpl.java`, and `MyShopApiDriver`.

Done so far (committed): UI default-identity switching (Step 1b), realm (Step 2), migration (Step 3), backend-java ownership rules with unit/component/integration/contract tests (Step 4), API-level test that an admin cannot place an order.

## Steps

- [ ] Step 1: Isolation/admin scenarios and the DSL identity vocabulary in Java latest (backend rules already exist, so these go green rather than red; the API-level "admin cannot place order" test is done). Do together with Step 6. **Delete the Java latest `ApiAuthorizationTest`** (`latest/acceptance`) once every case in it (anonymous => 401, customer => 403 on coupons/deliver, admin allowed, admin cannot place order; health stays public) is expressed as DSL scenarios — so the DSL needs an anonymous identity too. Legacy `mod04/e2e/ApiAuthorizationTest` stays (legacy keeps defaults); the dotnet/typescript latest equivalents follow in Phase 2.
- [ ] Step 5: frontend-react (also port the UI default-identity switching from Java `MyShopUiClient`/`KeycloakUiLogin` to the dotnet and typescript testkits, whose UI login is still hard-wired to admin1): hide place-order for admins; show customer in admin order list; tests. (Frontend image is shared by all three stacks — check the auth-disabled mode still works and verify against the dotnet/typescript stacks before pushing.)
- [ ] Step 6: Test drivers: API client and Playwright driver take the identity from the scenario (DSL) instead of operation-based defaults / always admin1; keep opt-in behaviour (no Keycloak URL => no auth).
- [ ] Step 7: Verify locally with `gh optivem` (full latest + legacy suites), then push per milestone and watch CI; confirm dotnet/typescript pipelines unaffected.
- [ ] Step 8: Update the Keycloak plan's Phase 2 recipe to include the ownership model for .NET/TypeScript (or schedule it after Phase 2 per the sequencing decision).

## Decisions (all open questions resolved 2026-10-05)

- Sequencing: Java first, then Keycloak Phase 2 copies the finished model.
- Another customer's order => 404. Admins may cancel any order; delivery stays admin-only.
- Existing orders keep a null owner (admin-only visibility). No pagination/filter in this plan.
- Legacy suites keep default identities, BUT the UI client must get DEFAULT-identity behaviour (login as customer1, re-login as admin1 for admin-only pages: coupons, deliver) because UI login is hard-wired to admin1 today and legacy UI tests mix both roles in one session. This must land before the realm change (Step 2).
- Monoliths and other languages follow in Keycloak Phase 2 (Steps 8–11).
