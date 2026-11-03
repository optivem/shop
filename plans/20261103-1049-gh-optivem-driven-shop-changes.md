# 2026-11-03 10:49 UTC — Shop changes requested by the gh-optivem meta-backlog

## TL;DR

**Why:** Several gh-optivem plans need edits or pushes in shop. They were scattered across gh-optivem's plans, and shop pushes to `main` change the images the shop cloud-deploy plan deploys, so they need one place and an explicit OK.
**End result:** Every shop change that gh-optivem depends on is done, pushed with approval, and verified by the rehearsal in the owning gh-optivem plan. This file is deleted when empty.

## ▶ Next executable step (resume here)

**Item 1**: `32e90dcb` is already on `main`; run Item 1's attended runtime check and check the CI run it triggered. The other items follow in order.

## Rules

- Commit only to shop, surgically. Ask before every push to shop `main`: the shop cloud-deploy plan (`20261029-1316-deploy-shop-cloud-run.md`) is active. Don't touch its footprint (`*-cloud.yml`, `terraform/`, Keycloak cloud files).
- Only one session at a time may run a rehearsal or `gh optivem system start` (shared docker ports).
- Each item is driven from its gh-optivem child plan, which holds the verification steps. Delete the item here once shipped.

## Items

1. - [ ] **Verify shop `32e90dcb`** (gh-optivem plan `plans/backlog/p2-atdd-quality/20260620-0806-no-order-persisted-fanout-prevention.md`, scoped to place/cancel order). Already pushed to `main` on 2026-11-03 by an accidental workspace sweep, compile-checked only. Run the attended runtime check of `cannotPlaceOrderWithNonExistentCoupon` on monolith plus multitier × 3 languages. Then do that plan's Step 6 (restore #68's criterion).

2. - [ ] **Gradle `--warning-mode none`** in `system-test/java/tests*.yaml` (gh-optivem plan `plans/backlog/p2-atdd-quality/20260608-2109-failure-capture-head-window.md`, step 6). Part of the decided Option C (head+tail capture plus `--warning-mode none`). Verify via the rehearsal named in that plan.

3. - [ ] **Enumerable ERP product-list path, Java only** (gh-optivem plan `plans/backlog/p2-atdd-quality/20260613-1950-view-product-list-erp-list-gap.md`, Option A). Its Items 1–5, in order:
   - `ErpStubClient.configureGetProducts` (stub `GET /erp/api/products`);
   - `returnsProducts` / `given().products(...)` on the ERP driver (leave `given().product()` unchanged);
   - `shouldBeAbleToListProducts()` in `BaseErpContractTest`;
   - `ErpGateway.getProducts()`;
   - new `ProductApiController` with `GET /api/products` (pass-through, no migration).
   Dotnet and typescript variants are a separate follow-up plan. Rehearse #65 on `gh-optivem-monolith-java.yaml`.

4. - [ ] **JSONPlaceholder "customer directory" external system** (gh-optivem plan `plans/backlog/p3-tooling-cost/20260617-1001-rehearsal-corpus-testinstance-and-refine.md`, steps 5a onward). Register a read-only `/users`-backed external system, `real-kind: test-instance`, in all stack configs (monolith/multitier × java/dotnet/typescript). Rehearse only under `gh-optivem-monolith-java.yaml`. The refine entry point (6a) is deferred and out of scope.

## Order and conflicts

Items 1 → 2 → 3 → 4, serial: all four use the rehearsal lock, and 1–3 touch `system-test/java`. Item 3 should start only after Item 1's push is verified.
