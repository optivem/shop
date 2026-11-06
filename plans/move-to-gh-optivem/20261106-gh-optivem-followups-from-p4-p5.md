# 2026-11-06 — gh-optivem follow-ups found while executing shop P4 and P5

## TL;DR

**Why:** Shop's P4 (gh-optivem-driven shop changes) and P5 (permanent acc/e2e environments) landed their shop side, but some steps need gh-optivem edits, a GitHub/human action or a rehearsal run through `gh optivem implement`. gh-optivem is read-only for shop sessions.
**End result:** A gh-optivem session records the shop commits in its plans and does the gh-optivem-side steps below. This file is deleted once each item is moved or done.

## Items (for a gh-optivem session)

1. **Plan `20260613-1950` (view-product-list, Option A):** record that shop `d777ecde` implements Option A Items 1–5 (ERP stub `GET /erp/api/products`, `returnsProducts`/`given().products()`, `shouldBeAbleToListProducts`, `ErpGateway.getProducts`, `ProductApiController` `GET /api/products`, Java monolith only). Drop its "waiting for the user to confirm the option" header. Then run the #65 rehearsal (its Item 6) on `gh-optivem-monolith-java.yaml`; later decide the .NET/TypeScript follow-up.
2. **Plan `20260617-1001` (rehearsal corpus, test-instance):** record that shop `a00053a0` registers external system `crm` (JSONPlaceholder `/users`, `real-kind: test-instance`) in all 12 shop configs with an `external-systems:` map, and that 5b (gh-optivem supports `test-instance`) is confirmed by reading the code. Remaining: 5c story ticket in optivem/shop, 5d add it to `DEFAULT_TICKETS` in `scripts/atdd-rehearsal-loop.sh` + CONTRIBUTING subsection, 5e rehearsal, 5f update the "Known coverage gap" note (`CONTRIBUTING.md:141`).
3. **Plan `20260620-0806` (no-order-persisted), human-only:** Step 6 (restore #68's criterion) and the attended run checking the fixer runs with the writer's scope.
4. **Bug (side finding):** `nameExecuted` in `internal/build/runner/tests.go` matches test names case-sensitively, so .NET camelCase `--test` names are reported "never executed" and exit 1.
5. **Scaffolder (from P5):** shop gained `setup-custom-domains.sh` (repo root; called by `setup-gcp.sh` when `ACC_DOMAIN`/`E2E_DOMAIN` are set) and opt-in `acc-domain`/`e2e-domain` inputs in `_acceptance-stage.yml`. Decide whether scaffolded repos should get the script (only matters for repos that opt in).

## Rehearsal prerequisites

`gh optivem implement` preflight needs the `gh` token's `project` / `read:project` scope (`gh auth refresh -h github.com -s project`) and gh-optivem's `.env`. Rehearsals write to shop, so run them when no shop cloud run is in progress.
