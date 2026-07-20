# frontend-react — a second `BackendStubDriver` implementation (in-process, no Pact)

**Status:** ⏳ Deferred — declined 2026-07-20 after analysis. Not "parked for later capacity";
declined on the merits. Reopen only if the finding below is shown to be wrong.

## TL;DR

**Why:** `BackendStubDriver` in `frontend-react` has exactly one implementation (`PactBackendStubDriver`), so the "one DSL, many drivers" seam looked unproven by construction. This forced five passages in a paid article to retreat from claiming a second driver exists.
**End result (as proposed):** A second, in-process (likely MSW) `BackendStubDriver` lets a Frontend Component Test run with no Pact mock server, driven by the *unchanged* Backend Stub DSL and unchanged specs.

## Decision: declined

The single implementor is **not** evidence of an unproven abstraction. It is evidence that the port
is correctly Pact-shaped.

Both of the Pact-specific things inside an interaction are load-bearing for Pact and *meaningless*
in-process:

```ts
// test/interactions/coupon.interactions.ts
states: [{ description: 'at least one coupon exists' }],    // provider state
body: { coupons: eachLike({ discountRate: decimal(0.2) }) } // Pact matchers
```

- `states` describes a provider you must put into a state. An in-process stub has no provider.
- Matchers exist so a *real* provider can be verified loosely. MSW only ever needs the concrete
  example value.

A neutral interaction type would therefore carry one field that the MSW adapter ignores entirely,
and a matcher concept that the MSW adapter flattens to its example. That is a fiction, not an
abstraction.

Meanwhile the claim the article actually wants is **already true one level up**: the 130-line Backend
Stub DSL (`test/support/backend-stub-dsl.ts`) never names Pact, and `backend.returnsPlacedOrder().execute()`
reads identically regardless of what is underneath. The DSL seam exists and is real. The *driver* seam
being single-implementor is a consequence of Pact's vocabulary being intrinsic to the port, not a gap.

Supporting context: this matches prior decisions not to grow the component layer's surface —
no real-ERP target at the component layer, component/Pact stays opt-in rather than a doubled
project, and dependency cost (MSW) should not be pushed onto cloning students by default.

### Cost that was avoided

~400–500 lines: a neutral matcher vocabulary, a rewrite of ~293 lines of `test/interactions/*.ts`
against it, two mapping layers (neutral→`MatchersV3`, neutral→concrete for MSW), plus an MSW
dependency in `frontend-react` — and a permanent second implementation to maintain.

### The counter-argument, recorded honestly

The strongest case *for* building it is that the finding above is a reading of the code, not a
proof. The only way to be certain the Pact vocabulary is intrinsic is to attempt the second adapter
and see what breaks. That was judged not worth 500 lines and a permanent maintenance surface in a
student template. **If this is ever reopened, do it as a timeboxed throwaway spike on a single
interaction (`browseCouponsInteraction`) — not as a full build.**

## Consequences of declining

The five softened passages in substack `PAID-TDD-maintainable-contract-tests-components.md`
(~180, ~316–317, ~518, ~524, and the takeaway) **keep their softened wording permanently** — they
claim the seam rather than a second implementation, which is now the accurate claim. See the
Decision 2 entry in substack `plans/20260714-1748-resolve-contract-tests-code-vs-prose-contradictions.md`
for exact locations.

The article is not wrong to celebrate "one DSL, many drivers" — it should point at the DSL seam and
at `componentHarness()` (`UiFrontendDriver`) vs `integrationHarness()` (`GatewayFrontendDriver`),
which is a genuine two-driver swap that already exists.

## Related

- **`20260720-1319-frontend-react-driver-port-adapter-split.md`** — the port/adapter split. Landed
  as `1f05681a`, stands on its own merits, unaffected by this decision.
- **`plans/deferred/20260720-1410-frontend-react-neutral-backend-interaction-type.md`** — the
  neutral-type work split out of 1319. Deferred alongside this plan, since its only stated
  justification was unblocking this one. Its resolved matcher-intent design is worth keeping if
  either plan is ever reopened.
- `test/support/driver/port/backend-stub-driver.ts` — the old `CAVEAT` comment (which framed the
  Pact vocabulary as a known deficiency) has been replaced with the reasoning above, so the code
  no longer advertises a gap we have decided is not one.
