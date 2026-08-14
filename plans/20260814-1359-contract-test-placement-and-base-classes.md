# Contract-test placement, "contract" vocabulary, and test base classes

**Status:** Decision 1 must be settled before any file moves. Items 1–3 are safe to execute
regardless of how Decision 1 lands.

## Context

Question that started this: does `system/multitier/backend-java/src/integrationTest/java/.../integration/contract/erp/`
belong under `integrationTest` or `contractTest`?

Tracing it surfaced that **"contract" currently names three different things across four paths**:

| Path | What it means there | Binding mechanism |
|---|---|---|
| `contractTest/.../backend/contract/{latest,legacy}/` | Pact provider verification — we are the provider to the frontend | Derivation: the consumer's stub *is* the pact |
| `componentTest/.../component/latest/contract/` | Stub consumability — does `ErpStubDriver`'s stub parse through the production gateway | Boot-and-read-back |
| `integrationTest/.../integration/contract/erp/` | Stub-vs-real parity — does the gateway's parse agree with the simulator | Twin tests, shared assertions |
| `system-test/.../systemtest/latest/contract/` | The ATDD CT — pinned as `ct-test:` in `gh-optivem-multitier-java.yaml` | Deployed system, STUB vs REAL mode |

All four are legitimately contract tests under the general definition (*do the two sides still
agree about the interface between them?*). They differ in the **binding mechanism**, which is
forced by whether the far side will run your verification — Pact where we control both ends
(frontend), stub-vs-real parity where we do not (ERP, tax, clock).

## Decision 1 — regroup by counterparty, or keep the scope-based taxonomy?

`docs/atdd/test-taxonomy.md` defines the current model and states one discriminator:

> Does the test boot or render the real component? Yes → component (layer 3). Calls one adapter
> directly, no boot / render → narrow integration (layer 2).
>
> The mocking stack — Pact mock server, WireMock, Testcontainers — is **orthogonal** to this line.

Under that doctrine every one of the four locations above is **correctly placed today**, and
line 178 documents `integration/contract/erp/ErpRealContractTest`'s home deliberately.

**Option A — keep scope-based layering, fix the vocabulary (recommended).**
No files move. Disambiguate the word "contract" so the three meanings are distinguishable by
name, and record the counterparty/binding-mechanism model in the taxonomy doc as a second,
*descriptive* axis that does not drive directory placement. Recommended because the scope axis
is already documented, already consistent across three languages and both architectures, and is
the axis that determines a test's cost and its CI stage — which is what suite membership needs
to key on.

**Option B — regroup by counterparty into `contractTest/{latest,legacy}/{internal,external}/`.**
Target tree:

```
src/contractTest/java/.../backend/contract/
  latest/
    internal/frontend/     BackendPactVerificationTest
    external/{clock,erp,tax}/   parity pairs + stub-consumability tests
  legacy/
    internal/frontend/
```

This makes the binding mechanism the primary organising principle. Cost: it overrides the
"orthogonal to mocking stack" rule, co-locates no-boot and full-boot tests in one folder, gives
the gateway-level tests `componentTest`'s heavier classpath (inherited via `build.gradle:43`),
and requires `docs/atdd/test-taxonomy.md` to be rewritten around a different discriminator.
`internal/` is preferred over `components/` because `component` already names a test *level*
here (`componentTest`, `component/latest/`), so `contractTest/latest/components/` reads alike
with the existing `componentTest/.../component/latest/contract/`.

Scope if Option B is chosen: `backend-java` only in the first pass; port to `backend-dotnet` and
`backend-typescript` once proven.

---

## Items

Safe under either option.

1. **Delete `AbstractIntegrationTest`.**
   `integrationTest/java/com/mycompany/myshop/backend/AbstractIntegrationTest.java` is an empty
   class body carrying one annotation, `@Import(TestcontainersConfiguration.class)`, with a
   single subclass. Move that annotation onto `OrderRepositoryIntegrationTest`, which already
   declares `@DataJpaTest`, `@AutoConfigureTestDatabase` and `@ActiveProfiles` inline — putting
   the fourth alongside them removes the split between inline and inherited configuration.
   Delete the file. `OrderControllerIntegrationTest` is `@WebMvcTest` with a mocked service and
   needs no DB, so no second subclass is coming.

2. **Rename the two surviving `Abstract*` bases to `Base*`.**
   - `AbstractComponentTest` → `BaseComponentTest` (extended by every component test and the
     Pact verifier)
   - `AbstractGatewayIntegrationTest` → `BaseGatewayIntegrationTest` (4 subclasses)

   Rationale: backend-java already holds 10 `Base*` classes against 3 `Abstract*`, one of which
   Item 1 deletes; system-test is 100% `Base*`; and the newest backend test base
   (`BaseErpProductContractIntegrationTest`) already uses `Base*`. This is two stragglers, not
   two competing conventions. Do this as its own commit — it touches many files mechanically and
   should not be entangled with any move.

3. **Disambiguate the "contract" vocabulary.**
   Give the three backend meanings distinguishable names so the word alone stops being
   ambiguous, without moving any file. Coordinate with the already-in-flight `contract` →
   `provider-verification` suite-id rename noted in `docs/atdd/test-taxonomy.md` lines 208–211 —
   extend that effort rather than opening a parallel one. Record in `docs/atdd/test-taxonomy.md`
   the counterparty/binding-mechanism model from the Context table above, explicitly as a
   descriptive axis that does not determine directory placement.

## Conditional items — only if Decision 1 lands on Option B

4. Move `contractTest/.../contract/{latest,legacy}/BackendPactVerificationTest` →
   `contract/{latest,legacy}/internal/frontend/`.
5. Move `integrationTest/.../integration/contract/erp/` → `contract/latest/external/erp/`.
6. Decide and then apply: do the three `componentTest/.../component/latest/contract/*StubContractComponentTest`
   move to `contract/latest/external/{clock,erp,tax}/`? They need the full SUT boot, which
   `contractTest` already supports via its `componentTest.output` classpath, so the move is
   mechanically possible. Leaving them behind means keeping two contract locations and the
   regrouping only half-pays off.
7. Update Java package declarations for every moved file.
8. Update `component-tests.yaml` suite filters (`:20`, `:25`) and add whatever new suite entries
   the moved tests need so nothing silently stops running.
9. Rewrite `docs/atdd/test-taxonomy.md` around the changed discriminator, including the
   "Backend: four-suite instantiation" section and the line-178 paragraph describing
   `integration/contract/erp/`.
10. Update the snippet in `plans/20260812-1600-erp-real-contract-ci-wiring.md`: its proposed
    suite runs `.\gradlew.bat integrationTest --tests '*ErpRealContractTest'`, and the Gradle
    task changes to `contractTest` if item 5 lands. The class-name filter itself survives a
    package move.

## Findings surfaced but not scheduled

Neither is a defect; both are recorded so the reasoning is not re-derived later.

- **The static stub mappings are not loaded by any test.** `external-systems/stubs/mappings/erp-products-*.json`
  is bind-mounted into the WireMock container by all twelve `docker-compose.*.stub.yml` files, so
  it is what every stub-mode deployment serves. All three ERP stub definitions (the static
  mappings, `ErpStubDriver`'s runtime registration, and the inline stub in
  `ErpStubContractIntegrationTest`) currently agree — same `{id, price}` shape, same 404-on-unknown
  — but the agreement is hand-maintained. Renaming `price` in the JSON turns nothing red. This is
  drift risk, not a live defect. Closing it would mean having the parity test arrange through
  `ErpStubDriver` so one artifact serves both roles, the way Pact does structurally on the
  frontend side.
- **Parity covers products only.** Promotion and error responses have no real-side twin, because
  the simulator's promotion endpoint is hardcoded and its errors cannot be provoked on demand.
  Already ruled out in `plans/20260717-1015-component-stub-contract-beyond.md` item 4.

## Verification

- `.\gradlew.bat compileJava compileTestJava compileIntegrationTestJava compileComponentTestJava compileContractTestJava`
  passes after Items 1 and 2.
- Every suite in `component-tests.yaml` reports the same test count before and after — the
  failure mode for both the rename and any move is tests silently dropping out of a wildcard
  filter rather than failing.

## Cross-references

- `docs/atdd/test-taxonomy.md` — the governing doctrine; Decision 1 is a question about whether
  to change it
- `plans/20260812-1600-erp-real-contract-ci-wiring.md` — deferred CI wiring for
  `ErpRealContractTest`; item 10 above depends on it
- `plans/20260717-1015-component-stub-contract-beyond.md` item 4 — why promotion and error
  real-mode twins were ruled out
