# 2026-11-06 20:30 UTC — Tag .NET and TypeScript acceptance tests with use case criterion IDs

## TL;DR

**Why:** The Place Order acceptance criteria live in one place for all three languages (`docs/use-cases/UC-001-place-order.md`). But only the Java system tests carry `UC-001.N` tags, and `spec.yaml` reads only Java JUnit XML. So the spec gate says nothing about whether .NET or TypeScript covers a criterion.
**End result:** The .NET and TypeScript acceptance tests carry the same `UC-001.N` tags as the Java tests. Their JUnit XML exposes the tags as properties the gate reads, and `spec.yaml` lists all three result globs. `results` is already a list (`StringList` in gh-optivem `internal/spec/config.go`), so this needs no gh-optivem change.

## ▶ Next executable step (resume here)

Step 1: find out how each language can put a test tag into its JUnit XML as a `<property>`. Compare with the Java output in `system-test/java/build/test-results/**/*.xml` and with gh-optivem `internal/spec/junit.go` (what it reads).

## Steps

- [ ] Step 1: Investigate. Java uses `@Tag("UC-001.N")`. Work out the .NET equivalent (e.g. xUnit `[Trait]` or NUnit `[Category]`, plus a JUnit logger that writes properties) and the TypeScript one (the test runner's tag or annotation, plus a JUnit reporter with properties). Check the exact property name `junit.go` expects.
- [ ] Step 2: Tag the .NET acceptance tests with the same IDs as the Java tests, one for one (use `system-test/java/.../latest/acceptance/PlaceOrder*Test.java` as the reference).
- [ ] Step 3: The same for TypeScript.
- [ ] Step 4: Add the .NET and TypeScript result globs to `results:` in `spec.yaml`. Decide whether the gate should report per language or pooled: pooled lets one language hide another's gap, so check how gh-optivem merges results.
- [ ] Step 5: Extend the `spec-gate` step's `if:` to all languages. Do this only once the gate is back on (see `plans/20261106-2030-spec-gate-read-gherkin-criteria.md`).

## Notes

- Depends on the gate being re-enabled for steps 4 and 5. Steps 1 to 3 can go ahead without it.
