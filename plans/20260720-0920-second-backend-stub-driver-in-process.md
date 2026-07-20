# frontend-react — a second `BackendStubDriver` implementation (in-process, no Pact)

**Status:** 🔵 Proposed (2026-07-20). Raised from the substack side while verifying the
`PAID-TDD-maintainable-contract-tests-*` series against this repo. Not yet scoped or sized.

## Why this exists

`BackendStubDriver` (`system/multitier/frontend-react/src/test/support/pact-backend-stub-driver.ts`)
is an interface with **exactly one implementation**: `PactBackendStubDriver`. The Backend Stub DSL
sits on top of the interface and never references a concrete driver — the seam is real and correctly
built. But an abstraction with a single implementor is **unproven by construction**: nothing
demonstrates the DSL is actually driver-agnostic, because nothing has ever swapped the driver.

The driver's own comment already anticipates this:

> A Component Test's in-process stub could implement the same surface without Pact.

Nothing does. There is no MSW in `frontend-react` (`npm ls msw` → absent; repo-wide hits are
transitive only), and the frontend Component Test *is* the Pact contract test.

## Where the pressure came from

The `PAID-TDD-maintainable-contract-tests-components.md` article's thesis is **one DSL, two drivers** —
`backend.returnsPlacedOrder().execute()` reading identically whether Pact or an in-process stub is
underneath. Five passages asserted that as fact. Since the second driver does not exist, they were
softened on 2026-07-20 (substack `569c6d0`) to claim the *seam* rather than a second implementation.

That is honest, but it is the **only** place in the four-article series where a paid article had to
retreat from a claim rather than the repo growing the code. Under the working rule — *shop is the
source of truth, but where an article wants something the repo lacks and arguably should have, flag
it as a candidate gap* — this is the flag.

## What "done" would look like

A second `BackendStubDriver` implementation that stubs the backend **in-process** (MSW is the obvious
candidate, but the choice is open), such that:

- The existing Backend Stub DSL drives it with **no change to the DSL and no change to any spec** —
  that is the whole point; if a spec has to change, the seam was not what we thought.
- A Frontend Component Test can run **without Pact** — no mock server boot, no pact file written,
  no provider-state coupling — while a Contract Test runs the same DSL lines over Pact.
- The `dead address` behaviour (`UNREACHABLE_BACKEND`, `http://127.0.0.1:1`) has a sensible
  equivalent, so a spec that stages nothing still fails loudly if the frontend calls out.

## Open questions (not yet answered)

- **Is this wanted at all?** Nothing is *broken* today. The seam works; it is just single-implementor.
  A reasonable answer is "no — the Pact driver is the only one we need, and the article should keep
  the softened wording permanently."
- **Does it earn its keep?** A second driver means a second thing to maintain. The payoff is faster
  Component Tests (no Pact mock-server boot) and a demonstrated abstraction — worth sizing before
  committing.
- **MSW or hand-rolled?** MSW is the conventional answer and the one the articles reference, but it
  is a new dependency in `frontend-react`.

## Consequences if it lands

The five softened passages in `PAID-TDD-maintainable-contract-tests-components.md` revert to the
stronger claim — see the Decision 2 entry in substack
`plans/20260714-1748-resolve-contract-tests-code-vs-prose-contradictions.md` for the exact locations
(~180, ~316–317, ~518, ~524, and the takeaway).

## Notes

- The article *does* already demonstrate a real two-driver swap, just not this one: `componentHarness()`
  (`UiFrontendDriver`) vs `integrationHarness()` (`GatewayFrontendDriver`). Both are Pact-backed, so it
  proves the **frontend** driver seam, not the **backend-stub** driver seam.
- Filed in `plans/` rather than `plans/deferred/` at the author's request — this is an open question to
  answer, not work already parked.
