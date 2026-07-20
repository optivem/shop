# frontend-react — neutralize `BackendStubDriver.stub()`'s parameter type

**Status:** 🔵 Proposed (2026-07-20). Split out of
`20260720-1319-frontend-react-driver-port-adapter-split.md`, which landed the port/adapter
layout and deliberately left the type leak for this plan.

## TL;DR

**Why:** `BackendStubDriver` now lives in `driver/port/backend-stub-driver.ts`, but its central
method still takes Pact's own `V3Interaction`. Any implementor therefore depends on Pact for its
types, so the port/adapter seam is structural rather than vocabulary-level — and the port's own
comment ("a Component Test's in-process stub could implement the same surface without Pact") is
still not true as written.
**End result:** `stub()` takes a neutral `BackendInteraction` owned by the port; the Pact adapter
maps it to `V3Interaction`. A non-Pact implementor becomes possible, which is what
`20260720-0920-second-backend-stub-driver-in-process.md` needs.

## Problem

`driver/port/backend-stub-driver.ts` carries an explicit CAVEAT comment recording this gap. The
leak is not just the type name — it is the **matcher policy**. The interaction builders in
`src/test/interactions/*.interactions.ts` construct Pact objects using `MatchersV3` (`like`,
`eachLike`, `integer`, `decimal`), and that matcher vocabulary is genuinely Pact-shaped: "this
field is an integer but I don't care which" has no meaning to an in-process stub, which needs a
concrete value.

So this is a design change, not a move. That is why it was split out.

## Constraint: the legacy suite must keep emitting identical Pact shapes

`src/test/legacy/` hand-writes the same interactions inline **on purpose** — it is the "before"
state the maintainable-contract-tests article refactors away from (see the header comment in
`order.interactions.ts`). Both suites write into the same pact file (same consumer + provider) and
Pact merges by interaction description, so the merge is only idempotent when both sides emit
byte-identical interactions.

Any neutral type must therefore round-trip through the Pact adapter to the **exact same**
`V3Interaction` the legacy suite writes by hand. A diff here corrupts `contracts/frontend-backend.json`
with two conflicting versions of one interaction.

## Proposed work

1. **Design `BackendInteraction`** in `driver/port/dtos/`. It must express: provider states,
   description (`uponReceiving`), request (method/path/headers/body), response
   (status/headers/body), and — the hard part — a matcher policy that both a Pact adapter and an
   in-process stub can honor. Sketch: carry concrete example values plus a per-field matching
   *intent* (`exact` vs `type`), letting the Pact adapter pick the `MatchersV3` call and an
   in-process stub simply serve the example.
2. **Rewrite the interaction builders** to return `BackendInteraction` instead of `V3Interaction`.
3. **Add the mapping layer** in `driver/adapter/pact-backend-stub-driver.ts`.
4. **Drop the CAVEAT comment** from the port once it no longer applies.
5. **Verify byte-identical pact output.** Snapshot `contracts/frontend-backend.json` before the
   change, run `npm run test:latest && npm run test:legacy && npm run test:pact`, and diff. Any
   difference is a bug in the mapping, not an acceptable variation.

## Resolved decisions

- **Matcher-intent model is scoped to the four matchers actually in use, tagged explicitly.**
  `BackendInteraction` carries concrete example values plus an explicit per-field intent —
  `exact` | `type` | `integer` | `decimal` — plus an array-of-template marker for `eachLike`.
  The intent is **never inferred** from the example's JS type: `decimal(22)` has an
  integer-valued example, so inference would emit `like`/`integer` and break byte-identity with
  the hand-written legacy interactions. Plain (untagged) values mean `exact`, which is what the
  suite already relies on for `status`, `detail`, and `errors[]`. The Pact adapter maps each
  intent to its `MatchersV3` call; an in-process stub ignores the intent and serves the example.
  No general matcher abstraction (regex, min/max, date/time) — widen only when a spec needs it.

## Open questions

- **Should `BackendInteraction` be one type or a small builder API?** The builders already provide
  the ergonomics, so a plain data type is probably enough — but worth a look once the shape of the
  matcher intent is settled.

## ▶ Next executable step (resume here)

Design work, not a mechanical edit: draft the `BackendInteraction` shape (item 1) — specifically
the matcher-intent model — before touching any builder. Start by reading
`src/test/interactions/order.interactions.ts` and cataloguing every distinct `MatchersV3` usage
and the field it guards. Resolve the two open questions above with the user first; use
`/refine-plan` rather than `/execute-plan` until item 1 has a concrete answer.
