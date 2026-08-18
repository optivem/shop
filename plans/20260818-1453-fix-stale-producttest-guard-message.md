# 2026-08-18 14:53:00 UTC — Fix stale ProductTest guard-message assertion (backend-clean-java)

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

Apply Step 1: in `system/multitier/backend-clean-java/src/test/java/com/mycompany/myshop/backend/domain/entities/ProductTest.java`, rename `carriesItsIdentifierAndPrice` → `carriesItsSkuAndPrice`, rename `rejectsConstructionWithoutAnIdentifier` → `rejectsConstructionWithoutASku`, and change the expected message on line 24 from `"id cannot be null"` to `"sku cannot be null"`. **This exact 3-line change may already be sitting uncommitted in the working tree** — check `git diff` on that file first and, if it is there, verify it matches this description rather than re-authoring it. Then run `./gradlew test` in that project (Step 2) before committing.

## Steps

- [ ] Step 1: Bring `ProductTest.java` in line with the renamed guard — `carriesItsIdentifierAndPrice` → `carriesItsSkuAndPrice`, `rejectsConstructionWithoutAnIdentifier` → `rejectsConstructionWithoutASku`, and `.hasMessage("id cannot be null")` → `.hasMessage("sku cannot be null")`. If the working tree already carries this diff, verify it and move on. Touch no other file — `Product.java` is correct.
- [ ] Step 2: Run `./gradlew test` in `system/multitier/backend-clean-java`. Expect `BUILD SUCCESSFUL`, 14/14 domain tests.
- [ ] Step 3: Run `./compile-all.sh` from the repo root — confirm no other project regressed.
- [ ] Step 4: Commit the fix (ask before committing, per the standing rule) and push.
- [ ] Step 5: Confirm `multitier-backend-clean-java-commit-stage` goes green on `main`, including the steps that were skipped in the failing run (Component, Integration, Contract, Real-Mode Contract tests, Linter).
- [ ] Step 6 (prevention): Reinforce in `CLAUDE.md` → **Pre-Commit Verification** that a Java-only change runs `./gradlew build` (which runs tests), not just a compile — `e351e75b` passed a compile-only check and shipped a failing test. Keep the edit to a sentence; do not restructure the section.

## Open questions

- Step 6 is a `CLAUDE.md` wording tweak; the section already says `./gradlew build`, so this may be a no-op on reading. Confirm at execution time whether the existing wording is already sufficient and drop the step if so.
