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

Step 7: verify locally with `gh optivem` (full latest + legacy suites; this is also the first real run of the new Java identity scenarios `AccessControlTest` / `OrderOwnershipTest`, and of the dotnet/typescript UI default-identity switch against the Keycloak stack), then push per milestone and watch CI; confirm dotnet/typescript pipelines unaffected. Then Step 8.

Done so far (committed): UI default-identity switching (Java), realm, migration, backend-java ownership rules with tests. Done, not yet committed or executed: Java latest DSL identities (`actingAsCustomer/AnotherCustomer/Admin/Anonymous`, `placedByCustomer/AnotherCustomer`, `browseOrderHistory`, `deliverOrder`, `statusCode`), `UserIdentity` threaded through the API and UI drivers, `OrderOwnershipTest` + `AccessControlTest`, `ApiAuthorizationTest` (latest) deleted. Step 5 (compiled/unit-tested only, not committed, not run against the stack): frontend-react hides place-order from admins without CUSTOMER (`canPlaceOrder` in `useRoles`, `RequireCanPlaceOrder` route guard, Home card), admin-only Customer column in the order history table (shown only when an order carries `customer`), optional `customer` in the API type and guard; dotnet/typescript UI default-identity switching (customer1 default, fresh session as admin1 for deliver/publish/browse coupons). Once the frontend hides place-order for admins, consider enabling the UI channel on the admin scenarios in `AccessControlTest`.

## Steps

- [ ] Step 7: Verify locally with `gh optivem` (full latest + legacy suites), then push per milestone and watch CI; confirm dotnet/typescript pipelines unaffected.
- [ ] Step 8: Update the Keycloak plan's Phase 2 recipe to include the ownership model for .NET/TypeScript (or schedule it after Phase 2 per the sequencing decision).

## Decisions (all open questions resolved 2026-10-05)

- Sequencing: Java first, then Keycloak Phase 2 copies the finished model.
- Another customer's order => 404. Admins may cancel any order; delivery stays admin-only.
- Existing orders keep a null owner (admin-only visibility). No pagination/filter in this plan.
- Legacy suites keep default identities, BUT the UI client must get DEFAULT-identity behaviour (login as customer1, re-login as admin1 for admin-only pages: coupons, deliver) because UI login is hard-wired to admin1 today and legacy UI tests mix both roles in one session. This must land before the realm change (Step 2).
- Monoliths and other languages follow in Keycloak Phase 2 (Steps 8–11).
