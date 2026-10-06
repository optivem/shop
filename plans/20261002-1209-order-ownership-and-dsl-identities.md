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

Confirm CI only: after the next scheduled runs on `shop` main at or after commit `72bc2e99`, check that the acceptance stages are green (`gh run list --repo optivem/shop`), especially `multitier-dotnet-acceptance-stage` (it failed on `1080c53b` with the admin home-ready timeout fixed in `72bc2e99`) and the three monolith stages (never run locally). If any fails, root-cause it; otherwise delete this plan. The ownership model for the other backends and monoliths is tracked as Step 13 of `plans/20261002-0801-keycloak-auth-java-react-first.md`.

Done and pushed: backend-java ownership rules, realm split, `owner` migration, Java DSL identities and scenarios (latest `ApiAuthorizationTest` removed), frontend changes, dotnet/typescript UI identity switch. Local verification: Java, dotnet and typescript multitier latest + legacy all green.

## Steps

- [ ] Step 7 (remaining): watch CI on `shop` main after `72bc2e99` (see the resume block above).

## Decisions (all open questions resolved 2026-10-05)

- Sequencing: Java first, then Keycloak Phase 2 copies the finished model.
- Another customer's order => 404. Admins may cancel any order; delivery stays admin-only.
- Existing orders keep a null owner (admin-only visibility). No pagination/filter in this plan.
- Legacy suites keep default identities, BUT the UI client must get DEFAULT-identity behaviour (login as customer1, re-login as admin1 for admin-only pages: coupons, deliver) because UI login is hard-wired to admin1 today and legacy UI tests mix both roles in one session. This must land before the realm change (Step 2).
- Monoliths and other languages follow in Keycloak Phase 2 (Steps 8–11).
