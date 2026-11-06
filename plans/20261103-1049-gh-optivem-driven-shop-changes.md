# 2026-11-03 10:49 UTC — Shop changes requested by the gh-optivem meta-backlog

## TL;DR

**Why:** Several gh-optivem plans need edits or pushes in shop. They were scattered across gh-optivem's plans, and shop pushes to `main` change the images the shop cloud-deploy plan deploys, so they need one place and an explicit OK.
**End result:** Every shop change that gh-optivem depends on is done, pushed with approval, and verified by the rehearsal in the owning gh-optivem plan. This file is deleted when empty.

## ▶ Next executable step (resume here)

**⏳ Deferred (user, 2026-11-06).** Resume after: `gh auth refresh -h github.com -s project`, a read-only clone of `/workspaces/gh-optivem` with its `.env`, and the user's go-ahead to file the 5c ticket.

**Item 4, step 5c**: author the `crm` story ticket in `optivem/shop`. Then the two rehearsals owed by Items 3 (#65) and 4 (the new ticket). Both need gh-optivem's `implement`, which first needs the `gh` token's `project` scope (`gh auth refresh -h github.com -s project`). Steps 5d and 5f are gh-optivem edits for a gh-optivem session.

## Rules

- **Pushes to shop `main` made by the rehearsals are pre-approved (user, 2026-11-06)**: the cloud-deploy plan is finished; check GitHub status before each push. This overrides the "ask before every push" below for rehearsal pushes. Filing the 5c ticket in `optivem/shop` is still pending the user's decision (deferred 2026-11-06).
- Commit only to shop, surgically. Ask before every push to shop `main`: the shop cloud-deploy plan (`20261029-1316-deploy-shop-cloud-run.md`) is active. Don't touch its footprint (`*-cloud.yml`, `terraform/`, Keycloak cloud files).
- Only one session at a time may run a rehearsal or `gh optivem system start` (shared docker ports).
- Each item is driven from its gh-optivem child plan, which holds the verification steps. Delete the item here once shipped.

## Items

Items 1 and 2 are done: Item 1's runtime check (`cannotPlaceOrderWithNonExistentCoupon`, monolith and multitier × 3 languages) was green on 2026-11-05 and re-run green on monolith-java on 2026-11-06; its Step 6 (restore #68's criterion) is Human-only and gated in the gh-optivem plan. Item 2 shipped as `de90fbe7` and its gh-optivem plan is closed (C1 rehearsal passed 2026-11-05).

3. - [ ] **Enumerable ERP product-list path, Java only** (gh-optivem plan `plans/backlog/p2-atdd-quality/20260613-1950-view-product-list-erp-list-gap.md`, Option A). **Shop side done (2026-11-06, see `git log -- system/monolith/java/src/main/java/com/mycompany/myshop/api/controller/ProductApiController.java`):** `ErpStubClient.configureGetProducts`, `ErpDriver.getProducts` / `returnsProducts`, `given().products().withProduct(sku, price)` and `then().products().containsProduct(sku, price)` (`given().product()` unchanged; the list replaces the default product), `shouldBeAbleToListProducts()` in `BaseErpContractTest`, `ErpGateway.getProducts()`, `ProductService` and `ProductApiController` (`GET /api/products`, authenticated, pass-through, no migration). Verified: compile-all green; monolith-java `--sample` green; full contract suites (stub and real) green, including the new list contract on both. **Remaining:** rehearse #65 on `gh-optivem-monolith-java.yaml` (`scripts/atdd-rehearsal.sh 65` in gh-optivem). Blocked on the `gh` token's missing `project` scope (`gh auth refresh -h github.com -s project`) and gh-optivem's `.env`; the rehearsal also writes to shop, so run it when no cloud run is in progress. The child plan's reasons 2 and 3 (default product in `WhenImpl.viewProductList`, exact counts on a shared WireMock) concern the DSL the agents write for #65; the new verification asserts on staged SKUs, not counts. Dotnet and typescript variants are a separate follow-up plan.

4. - [ ] **JSONPlaceholder "customer directory" external system** (gh-optivem plan `plans/backlog/p3-tooling-cost/20260617-1001-rehearsal-corpus-testinstance-and-refine.md`, steps 5a onward). **Step 5a done (2026-11-06):** external system `crm` (customer directory over JSONPlaceholder `/users`), `real-kind: test-instance`, stub `external-systems/stubs`, no simulator block, in all 12 stack configs with an `external-systems:` map (monolith/multitier × java/dotnet/typescript, latest and legacy; `multitier-clean-java` has no such map). Named `crm` so the driver-port folder can match it in all three languages (`external/crm`, .NET `External/Crm`). `gh optivem config validate` passes on all 12. **Step 5b (read-only check of gh-optivem `b684625e`):** `test-instance` is wired: config validation (simulator block forbidden), preflight (no simulator path to stat), the `real-kind` gate in `internal/atdd/process/gates/bindings.go`. No cloud or deploy change is needed now: nothing calls `crm` yet. When the 5c story lands, its contract-real suite needs the JSONPlaceholder base URL in the system-test config (an external public URL, no new service). **Remaining:** 5c, author the story ticket in `optivem/shop` (Gherkin AC, e.g. show the customer's name on an order from the directory; must change `external/crm` so `external-driver-port-changed` fires); 5d and 5f are gh-optivem edits (`scripts/atdd-rehearsal-loop.sh` `DEFAULT_TICKETS`, its `CONTRIBUTING.md` "Known coverage gap" note); 5e, rehearse under `gh-optivem-monolith-java.yaml` (same `project`-scope blocker as Item 3). The refine entry point (6a) is deferred and out of scope.

## Order and conflicts

Items 3 → 4, serial: both rehearsals use the rehearsal lock.
