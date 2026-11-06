# 2026-11-03 10:49 UTC — Shop changes requested by the gh-optivem meta-backlog

## TL;DR

**Why:** Several gh-optivem plans need edits or pushes in shop. They were scattered across gh-optivem's plans, and shop pushes to `main` change the images the shop cloud-deploy plan deploys, so they need one place and an explicit OK.
**End result:** Every shop change that gh-optivem depends on is done, pushed with approval, and verified by the rehearsal in the owning gh-optivem plan. This file is deleted when empty.

## ▶ Next executable step (resume here)

**Item 4**: register the JSONPlaceholder customer directory in the stack configs. Then the two rehearsals owed by Items 3 and 4 (both need gh-optivem's `implement`, see each item).

## Rules

- Commit only to shop, surgically. Ask before every push to shop `main`: the shop cloud-deploy plan (`20261029-1316-deploy-shop-cloud-run.md`) is active. Don't touch its footprint (`*-cloud.yml`, `terraform/`, Keycloak cloud files).
- Only one session at a time may run a rehearsal or `gh optivem system start` (shared docker ports).
- Each item is driven from its gh-optivem child plan, which holds the verification steps. Delete the item here once shipped.

## Items

Items 1 and 2 are done: Item 1's runtime check (`cannotPlaceOrderWithNonExistentCoupon`, monolith and multitier × 3 languages) was green on 2026-11-05 and re-run green on monolith-java on 2026-11-06; its Step 6 (restore #68's criterion) is Human-only and gated in the gh-optivem plan. Item 2 shipped as `de90fbe7` and its gh-optivem plan is closed (C1 rehearsal passed 2026-11-05).

3. - [ ] **Enumerable ERP product-list path, Java only** (gh-optivem plan `plans/backlog/p2-atdd-quality/20260613-1950-view-product-list-erp-list-gap.md`, Option A). **Shop side done (2026-11-06, see `git log -- system/monolith/java/src/main/java/com/mycompany/myshop/api/controller/ProductApiController.java`):** `ErpStubClient.configureGetProducts`, `ErpDriver.getProducts` / `returnsProducts`, `given().products().withProduct(sku, price)` and `then().products().containsProduct(sku, price)` (`given().product()` unchanged; the list replaces the default product), `shouldBeAbleToListProducts()` in `BaseErpContractTest`, `ErpGateway.getProducts()`, `ProductService` and `ProductApiController` (`GET /api/products`, authenticated, pass-through, no migration). Verified: compile-all green; monolith-java `--sample` green; full contract suites (stub and real) green, including the new list contract on both. **Remaining:** rehearse #65 on `gh-optivem-monolith-java.yaml` (`scripts/atdd-rehearsal.sh 65` in gh-optivem). Blocked on the `gh` token's missing `project` scope (`gh auth refresh -h github.com -s project`) and gh-optivem's `.env`; the rehearsal also writes to shop, so run it when no cloud run is in progress. The child plan's reasons 2 and 3 (default product in `WhenImpl.viewProductList`, exact counts on a shared WireMock) concern the DSL the agents write for #65; the new verification asserts on staged SKUs, not counts. Dotnet and typescript variants are a separate follow-up plan.

4. - [ ] **JSONPlaceholder "customer directory" external system** (gh-optivem plan `plans/backlog/p3-tooling-cost/20260617-1001-rehearsal-corpus-testinstance-and-refine.md`, steps 5a onward). Register a read-only `/users`-backed external system, `real-kind: test-instance`, in all stack configs (monolith/multitier × java/dotnet/typescript). Rehearse only under `gh-optivem-monolith-java.yaml`. The refine entry point (6a) is deferred and out of scope.

## Order and conflicts

Items 3 → 4, serial: both rehearsals use the rehearsal lock.
