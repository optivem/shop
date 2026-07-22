# frontend-react — neutralize `BackendStubDriver.stub()`'s parameter type

**Status:** ⏳ Deferred — declined 2026-07-20, together with the plan it exists to unblock.
Split out of `20260720-1319-frontend-react-driver-port-adapter-split.md` (landed as `1f05681a`),
which left the type leak for this plan.

## Why this was declined

This plan's own TL;DR states its end result is "a non-Pact implementor becomes possible, which is
what `20260720-0920-second-backend-stub-driver-in-process.md` needs." That plan was declined on the
merits the same day — see `plans/deferred/20260720-0920-second-backend-stub-driver-in-process.md`.
With 0920 declined, this plan has no remaining stated justification.

Two findings from that analysis bear directly on the work below:

1. **The type leak is intentional, not a deficiency.** `states` describes a provider you must put
   into a state; an in-process stub has no provider. Matchers exist so a *real* provider can be
   verified loosely; an in-process stub only needs the concrete example. A neutral type carries a
   field one implementor ignores entirely and a matcher concept it flattens — a fiction rather than
   an abstraction. The port comment in `driver/port/backend-stub-driver.ts` has been updated to say
   so; the old CAVEAT wording (which framed this as a gap) is gone.
2. **The byte-identity constraint below makes this expensive and brittle** in a way that is hard to
   justify without a consumer for the result.

**What would reopen this:** wanting the neutral type on *independent* design merits — "a port should
not leak its adapter's vocabulary" — rather than as a prerequisite for a second driver. That is a
legitimate argument, but this plan does not currently make it, and it must be weighed against
finding 1 above. If reopened on those grounds, rewrite the TL;DR to stand alone first.

**Worth keeping:** the resolved matcher-intent decision below is real design work and survives the
deferral. Do not re-derive it if this is picked back up.

## TL;DR

**Why:** `BackendStubDriver` now lives in `driver/port/backend-stub-driver.ts`, but its central
method still takes Pact's own `V3Interaction`. Any implementor therefore depends on Pact for its
types, so the port/adapter seam is structural rather than vocabulary-level — and the port's own
comment ("a Component Test's in-process stub could implement the same surface without Pact") is
still not true as written.
**End result:** `stub()` takes a neutral `BackendInteraction` owned by the port; the Pact adapter
maps it to `V3Interaction`. A non-Pact implementor becomes possible, which is what
`20260720-0920-second-backend-stub-driver-in-process.md` needs.

## Target state

`driver/port/dtos/` owns `BackendInteraction`: a **plain interface** (states, `uponReceiving`,
`withRequest`, `willRespondWith`) whose body values are either plain literals — meaning *match
exactly* — or values wrapped by one of four one-line tag helpers, `type` / `integer` / `decimal` /
`eachLike`, each attaching an explicit matching **intent** to a concrete example. Intent is never
inferred from the example's JS type. `BackendStubDriver.stub()` takes that type, and neither the
port nor the interaction builders import anything from `@pact-foundation/pact`.

`driver/adapter/pact-backend-stub-driver.ts` gains a mapper that walks the interaction and turns
each intent into its `MatchersV3` call (`type`→`like`, `integer`→`integer`, `decimal`→`decimal`,
`eachLike`→`eachLike`, untagged→the literal). An in-process stub can implement the same port by
ignoring intents entirely and serving the examples.

(Note: this section describes where the plan *would* land if executed. It is currently declined —
and the CAVEAT comment it planned to delete is already gone, removed when the plan was declined.)

**What the developer sees:** `*.interactions.ts` looks almost unchanged — same object literals,
same nesting, `like(x)` simply became `type(x)` and the Pact import is gone. There is still exactly
one way to build an interaction: call the named builder function.

**Explicitly unchanged:** `contracts/frontend-backend.json` is byte-identical before and after —
verified by snapshot-and-diff across `test:latest`, `test:legacy`, `test:pact`. `src/test/legacy/`
keeps hand-writing its Pact interactions inline; it is the article's "before" state and this plan
does not touch it. No new matcher capability (regex, min/max, date/time) is introduced, and no
fluent/builder construction API is added.

## Problem

`driver/port/backend-stub-driver.ts` used to carry an explicit CAVEAT comment framing this as a
gap; that wording was replaced when the plan was declined, since the leak is now held to be
intentional. The leak is not just the type name — it is the **matcher policy**. The interaction builders in
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

1. **Write `BackendInteraction`** in `driver/port/dtos/` — shape is decided, see
   `## Resolved decisions`. A plain interface (states, `uponReceiving`, `withRequest`,
   `willRespondWith`) plus the four tag helpers `type` / `integer` / `decimal` / `eachLike`,
   each wrapping a concrete example with an explicit intent; untagged values mean `exact`.
2. **Rewrite the interaction builders** to return `BackendInteraction` instead of `V3Interaction`
   — mechanically, `like(x)` → `type(x)` and drop the `@pact-foundation/pact` imports. Covers
   `order.interactions.ts` and `coupon.interactions.ts`.
3. **Add the mapping layer** in `driver/adapter/pact-backend-stub-driver.ts`.
4. ~~**Drop the CAVEAT comment** from the port once it no longer applies.~~ Already done — the
   CAVEAT wording was replaced in `driver/port/backend-stub-driver.ts` when this plan was declined.
   The comment there now states the type leak is intentional and points at the reopen criterion.
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

- **`BackendInteraction` is a plain data type, not a builder API.** The port exports an interface
  plus four one-line tag helpers (`type`, `integer`, `decimal`, `eachLike`) that wrap a value with
  its intent. The `*.interactions.ts` functions stay the only construction path — a fluent builder
  would add a second one. Rationale: object literals are the modern TS idiom (structural typing
  and literal autocomplete already give what builders were invented to provide in Java/C#), they
  keep the body reading like the JSON it produces rather than regrouping fields by matcher, and
  they express nested templates — `eachLike({ quantity: integer(2), country: type('US'), … })` in
  `browseOrderHistoryInteraction` — without sub-builders. Pact's own DSL is builder-style, but
  copying that idiom into the neutral type would re-import the very Pact shape this plan removes.
  Per-field edit is therefore just `like(x)` → `type(x)`; untagged values mean `exact`.

## ▶ Next executable step (resume here)

**None — deferred.** There is no next step unless the plan is reopened on independent design
merits (see "Why this was declined" above). If it is, the first move is to rewrite the TL;DR so it
stands without 0920 — not to start editing builders.

There are **no open questions**: both design decisions are settled in `## Resolved decisions`.
`/refine-plan` therefore has nothing to resolve on this file. Reopening is a judgement call about
whether the neutral type is worth it on its own merits, not a question this plan can answer.
