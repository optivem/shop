# 2026-08-18 14:53:00 UTC — Fix stale ProductTest guard-message assertion (backend-clean-java)

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-08-18T15:00:24Z`

## TL;DR

**Why:** `main` is red. Run [32150581071](https://github.com/optivem/shop/actions/runs/32150581071) of `multitier-backend-clean-java-commit-stage` fails at **Run Unit Tests** (`./gradlew test`): `ProductTest > rejectsConstructionWithoutAnIdentifier()` expects `"id cannot be null"` but the guard now says `"sku cannot be null"`. Commit `e351e75b` (Chunk R) renamed `Product.id` → `Product.sku` and updated only the accessor call site in the test, leaving the assertion string and two test method names stale.
**End result:** `ProductTest` asserts the real guard message and its method names read in `sku` terms; `./gradlew test` is green (14/14 domain tests) and the `multitier-backend-clean-java-commit-stage` workflow goes green on `main`.

## Outcomes

- `ProductTest` matches the post-rename `Product` contract — no stale `id` vocabulary left in the clean-java domain tests.
- `multitier-backend-clean-java-commit-stage` is green on `main`, unblocking the downstream steps that never ran (Component / Integration / Contract / Real-Mode Contract tests, Linter).
- The verification gap that let this through is recorded: a Java-only change must be gated on `./gradlew build` (or `./gradlew test`), not compile alone.

## Diagnosis (settled — do not re-derive)

- **Failing step:** `Run Unit Tests` → `./gradlew test` in `system/multitier/backend-clean-java`.
- **Assertion:** `org.opentest4j.AssertionFailedError: Expecting message to be: "id cannot be null" but was: "sku cannot be null"`.
- **Correct side:** `system/multitier/backend-clean-java/src/main/java/com/mycompany/myshop/backend/domain/entities/Product.java:13` — `Guard.notNull(sku, "sku")`. `sku` is the right name; it matches how `Order` guards the same concept. **Do not change `Product.java`.**
- **Stale side:** `system/multitier/backend-clean-java/src/test/java/com/mycompany/myshop/backend/domain/entities/ProductTest.java:24` — `.hasMessage("id cannot be null")`, plus the method names `carriesItsIdentifierAndPrice` and `rejectsConstructionWithoutAnIdentifier`.
- **Classification:** test-authoring error (incomplete rename in `e351e75b`), not a product bug.
- **Reproduced locally:** `./gradlew test` at `HEAD` (`3a904ab1`) fails with the same assertion; with the working-tree edit applied it passes 14/14.
- **No cross-language twin:** `backend-java`, `backend-dotnet`, and `backend-typescript` have no `Product` domain entity — only `ProductDetailsResponse` DTOs. Nothing to mirror.
- **No other stale assertions:** every `hasMessage(...)` in the clean-java domain tests was checked against its guard; `ProductTest` was the only mismatch.

## ▶ Next executable step (resume here)

**Step 5 only — confirm CI.** Steps 1–4 and 6 are done and committed: `ProductTest` matches the
guard, `./gradlew build` is green (96 unit tests, 0 failures), the repo-root sweep ran, and the
`CLAUDE.md` sentence is in. What is left is watching
`multitier-backend-clean-java-commit-stage` on `main` — in particular the steps that never ran in
the red run (Component, Integration, Contract, Real-Mode Contract tests, Linter).

## Steps

- [ ] Step 5: Confirm `multitier-backend-clean-java-commit-stage` goes green on `main`, including the steps that were skipped in the failing run (Component, Integration, Contract, Real-Mode Contract tests, Linter).

## Found during execution (not part of this plan)

- The commit also carried the uncommitted Chunk B / value-object work (`Sku`, `OrderNumber`, the
  sales report). That pass had missed the `contractTest` source set entirely — four compile errors
  in `BaseErpProductParityContractTest` and both `BackendPactVerificationTest`s — and left three
  assertions comparing a value object to a raw `String` (`getSku()).isEqualTo("BOOK-123")`), which
  compile but fail at runtime. All seven were fixed before committing.
- `./compile-all.sh` reports `gh-optivem-multitier-clean-java.yaml FAILED` in 00:00 with
  `field kind not found in type projectconfig.Config`. This is **not** a compile failure: the
  locally installed `gh optivem` binary predates the `kind:`/`component:` schema that `3a904ab1`
  introduced. Upgrading the local `gh-optivem` install is the fix — it also blocks plan
  `20260818-1659-component-tests-yaml-for-backend-clean-java.md`, whose every verification step
  shells out to `gh optivem component-test`.
