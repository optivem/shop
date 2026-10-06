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

**Step A — Java monolith ownership model.** `monolith-java-acceptance-stage` is red in CI on `dff947e9` and later: the Java monolith has no ownership rules, so four Java latest scenarios fail against it (customer can view another customer's order, admin can place an order, customer cancels another's order, customer sees another's order in history). The Java test kit is shared by multitier-java and monolith-java. Implement the same rules as `system/multitier/backend-java` (owner/owner_name set from the token `sub`/`preferred_username`; POST place-order requires CUSTOMER; history and lookup/cancel filtered to the owner with 404 for others and for null-owner orders; admins see all, may cancel any, history items carry `customer`; deliver stays admin-only) in `system/monolith/java`, including its UI pages (hide place-order from admins, customer column for admins) and its unit/integration tests. The monolith uses server-side OIDC, so read its security config and how it exposes roles before copying. Then verify with the targeted commands below and check `monolith-java-acceptance-stage` on CI.

Then **Step B** (below): mirror the tests in .NET and TypeScript FIRST and stop for the author's review of the syntax before any backend work.

## Steps

- [ ] Step A: Java monolith ownership model (see the resume block). Run via a subagent; do not commit until the targeted verification below is green; then `/commit` limited to `shop`.
- [ ] Step B: .NET and TypeScript test mirroring, tests first. (1) Mirror into `system-test/dotnet` and `system-test/typescript` the Java test DSL as it stands after `3670d2c8`: Given-side identity (`loggedInAsCustomer()` / `loggedInAsCustomer("A")` / `loggedInAsAdmin()` / `notLoggedIn()`), aliased named customers (`placedByCustomer("B")`; the no-arg default customer is customer1 and is reserved when used; aliases are assigned from the remaining users in order of first use; clear error when a scenario needs more customers than exist, currently 2), the order-history use case (API only), `errorMessage(...)` assertions with shared constants for "Authentication is required" and "You do not have permission to perform this action" (no HTTP status codes in the DSL), and a skip-without-Keycloak marker per scenario. Reference implementation: Java, commits `463402b8`, `dff947e9`, `4e964fdf`, `3670d2c8` (read `CustomerAliases`, `UserIdentity`, `GivenImpl`, `GivenOrderImpl`, `ErrorMessages`, `KeycloakRequirementExtension`). The UI default-identity switch is already ported in both kits. (2) Scenarios: fold into the use-case test classes exactly like the Java ones (ViewOrderPositive/Negative, PlaceOrderNegative, CancelOrderPositive/Negative, BrowseOrderHistoryPositive/Negative, DeliverOrderNegative, BrowseCouponsNegative, PublishCouponNegative, plus the alias-stability scenario). Delete the language's `ApiAuthorizationTest` equivalent once its cases are covered (the legacy mod04 ones stay). (3) Leave the new tests UNCOMMITTED and **stop: show the author the test syntax in each language for review.** They will be red against the current backends — do not push red tests (pipelines would turn red).
- [ ] Step C: after the author approves the syntax: implement the ownership model in the .NET multitier backend, TypeScript multitier backend, .NET monolith and TypeScript monolith (same rules as Step A; Keycloak plan Step 13 has the recipe). The backends must return the same 401/403 messages as backend-java. Frontend is shared and already done. One subagent per implementation; verify each with the targeted commands; commit and push only when green.
- [ ] Step D: check CI on `shop` main for all acceptance stages after the last push (`gh optivem actions status` or `gh run list --repo optivem/shop`), then delete this plan. Also decide the parked questions: `plans/20261006-1200-latest-smoke-health-without-token.md` and Keycloak plan Step 14 (retire the opt-in guard).

## How to verify (learned the hard way — do NOT run the full suite, it takes ~35 min per language)

- Always: `./gradlew compileTestJava` (system-test/java), `dotnet build` (system-test/dotnet), `npx tsc --noEmit` (system-test/typescript).
- Local runs need `KEYCLOAK_URL_REAL` / `KEYCLOAK_URL_STUB` exported (java multitier 8191/8192, dotnet multitier 8291/8292, typescript multitier 8391/8392); without them identity scenarios skip and everything else gets 401.
- Docker images are shared by name across languages and go stale: always `down -v` both stacks and rebuild (`docker compose -p my-shop-{stub,real} -f docker/<lang>/<arch>/docker-compose.local.{stub,real}.yml build`) before a run, and rebuild right before each language's run.
- Targeted Java latest (from the shop root): `export GH_OPTIVEM_CONFIG=gh-optivem-multitier-java.yaml; gh optivem system start && gh optivem system-test setup && gh optivem system-test run --suite acceptance --test <ClassName>,<ClassName>` (use `gh-optivem-monolith-java.yaml` for the monolith). Targeted legacy: `GH_OPTIVEM_CONFIG=gh-optivem-multitier-java-legacy.yaml` with `--suite mod08-smoke,mod08-e2e-api,mod08-e2e-ui,mod09-smoke-stub,mod09-smoke-real` (mod08/mod09 are the only legacy suites using the scenario DSL). The runner prints "requested test(s) never executed" and exits 1 for class filters even when everything passed — read the Suite Results table. Equivalent configs exist for dotnet and typescript (`gh-optivem-*-dotnet*.yaml`, `*-typescript*.yaml`).
- Windows: killing `test-all.sh` does not stop the `gh`/gradle/test JVM chain; kill the `gh.exe` / `gh-optivem.exe` parent FIRST, then leftover gradle/test JVMs, playwright node processes and gradle daemons (`./gradlew --stop`), else a locked `build/test-results` breaks the next run.
- Admin UI flows: the UI "home ready" marker is the Order History link (admins do not see New Order).
- CI schedules re-run the acceptance stages hourly after a push; `gh run list --repo optivem/shop`.

## Decisions (all open questions resolved 2026-10-05)

- Sequencing: Java first, then Keycloak Phase 2 copies the finished model.
- Another customer's order => 404. Admins may cancel any order; delivery stays admin-only.
- Existing orders keep a null owner (admin-only visibility). No pagination/filter in this plan.
- Legacy suites keep default identities, BUT the UI client must get DEFAULT-identity behaviour (login as customer1, re-login as admin1 for admin-only pages: coupons, deliver) because UI login is hard-wired to admin1 today and legacy UI tests mix both roles in one session. This must land before the realm change (Step 2).
- Monoliths and other languages follow in Keycloak Phase 2 (Steps 8–11).
