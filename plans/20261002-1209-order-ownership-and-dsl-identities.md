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

**Step D:** Steps A–C are committed and pushed. Check CI on `shop` main for all acceptance stages after the push (`gh optivem actions status` or `gh run list --repo optivem/shop`; schedules re-run hourly), then delete this plan. Also decide the parked questions: `plans/20261006-1200-latest-smoke-health-without-token.md` and Keycloak plan Step 14.

## Steps

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
