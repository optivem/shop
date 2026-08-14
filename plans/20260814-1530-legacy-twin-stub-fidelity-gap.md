# Legacy twins' inline stub JSON has no fidelity guard

**Status:** Open — **not yet approved to build. Discuss before executing.**

## Context

The stub-fidelity suite (`contract/external/`) guards exactly one artifact: the stub drivers
(`ErpStubDriver` / `TaxStubDriver` / `ClockStubDriver`).

- `*StubConsumabilityContractTest` — the driver's JSON still parses through the production gateway.
- `*StubParityContractTest` / `*RealParityContractTest` — the driver's JSON still matches the real
  ERP simulator.

Both reach the stubs through the DSL, which is the point: the DSL is a choke point, so pinning it
once covers every test that arranges through it.

**The `legacy/` twins do not arrange through it.** By design — inlined raw WireMock *is* the
"before" they exist to show. So `component/legacy/`, `integration/legacy/` and
`contract/internal/frontend/legacy/` hand-write their own stub JSON, and none of it is covered by
either fidelity test.

The invariant is currently stated in prose only. From
`contract/internal/frontend/legacy/BackendPactVerificationTest`:

> The URLs and JSON bodies are byte-identical to what the stub drivers register, so the two twins
> are behaviour-neutral with respect to each other — the difference is vocabulary, not effect.

Nothing enforces "byte-identical".

## The failure mode

1. The real ERP renames `price` → `cost`.
2. `ErpRealParityContractTest` goes red. Working as intended.
3. `ErpStubDriver` is fixed; `ErpGateway` / `ProductDetailsResponse` are fixed.
4. Every `latest/` test goes green.
5. The `legacy/` twins keep asserting `{"price": …}` against a gateway that now reads `cost`.

Step 5 fails loudly if the gateway simply stops parsing (good). But where the drift is additive or
tolerated — `ProductDetailsResponse` carries `@JsonIgnoreProperties(ignoreUnknown = true)` — the
legacy twin can keep passing while asserting a shape the real system no longer sends. It then
teaches a wire format that does not exist.

**Severity: pedagogical, not correctness.** The legacy twins are teaching artifacts; nothing ships
on them. A stale one misleads a student rather than releasing a bug. That is what puts this below
the CI-wiring work in `20260812-1600-erp-real-contract-ci-wiring.md`, not above it.

## Scope

Affected files (all hand-write stub JSON):

- `componentTest/.../component/legacy/CouponComponentTest`
- `componentTest/.../component/legacy/OrderHistoryComponentTest`
- `componentTest/.../component/legacy/smoke/system/MyShopSmokeTest`
- `integrationTest/.../integration/legacy/ErpGatewayIntegrationTest`
- `contractTest/.../contract/internal/frontend/legacy/BackendPactVerificationTest` — the largest
  surface: ~17 inline stub lines across 7 state handlers.

## Options

**A. Accept it explicitly (cheapest).** Replace the "byte-identical" prose with an honest note: the
legacy twins are frozen snapshots of the pre-DSL wire format, are not fidelity-guarded, and are
expected to be updated by hand when a driver changes. Costs nothing, removes a claim the repo does
not back, but leaves the drift silent.

**B. A byte-comparison test.** One test that renders each stub driver's JSON for a fixed input and
compares it against the literals the legacy twins use, sourced from a shared constant. Catches drift
mechanically — but pulling the literals into a shared constant is *itself* a step of the refactor the
legacy twins exist to predate, which damages them as a "before". Would need the constant to live on
the legacy side and be read by the check, not the reverse.

**C. Delete the legacy twins that are not carrying their weight.** `component/legacy/` is a
representative subset by choice, and `integration/legacy/ErpGatewayIntegrationTest` largely
duplicates what `contract/external/erp/ErpStubParityContractTest` now covers. Only the pact twin
demonstrates duplication at a scale the DSL argument needs (see `docs/atdd/test-taxonomy.md`,
"Why counterparty comes first in the path"). Narrowing to that one twin shrinks the unguarded
surface to a single file.

**Recommendation: A now, C as a separate discussion.** A is honest, immediate, and costs nothing.
C is a real question about how many "before" twins the teaching material needs, which deserves its
own decision rather than being smuggled in as a fidelity fix. B is not worth its complexity —
it re-introduces sharing into the twins that exist to show the absence of sharing.

## Open questions

1. Does the tax gap change the priority? There is no `TaxRealParityContractTest`, so `TaxStubDriver`
   is consumability-checked but never held against the real simulator. That is a gap in the
   `latest/` path, tracked separately from this item, but it means tax stub JSON is unguarded on
   *both* sides right now.
2. Under option C, does `component/legacy/` survive at all? It is already a subset, so it does not
   demonstrate scale — its value is showing the vocabulary difference on a familiar scenario.

## Not in scope for this item

- Adding `TaxRealParityContractTest` / `ClockRealParityContractTest` — separate gap, `latest/` side.
- The `.NET` / TypeScript ports, which have no `contract/external/` suite yet.
