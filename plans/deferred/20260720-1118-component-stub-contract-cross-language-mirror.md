# Mirror the component stub-contract items into the other multitier backends (DEFERRED)

**Source plan:** `plans/20260717-1015-component-stub-contract-beyond.md` (item 5, split out 2026-07-20).
**Status:** Deferred — execute only **after** the java/react items land and their shape is settled.
**Scope (this file):** `system/multitier/backend-dotnet` + `system/multitier/backend-typescript`. **Monolith stays untouched.**

## TL;DR

**Why:** The stub-contract work in the source plan is being built first in `backend-java` (+
`frontend-react`). CLAUDE.md's "check all languages" rule means .NET and TypeScript should eventually
get the same coverage — but only once the DSL surface stops moving, to avoid porting a shape that is
still being argued about. This is the same sequencing as
`plans/deferred/20260616-0830-component-pact-layer-other-multitier-backends.md`.

**End result:** `backend-dotnet` and `backend-typescript` carry the same component stub-contract
coverage as `backend-java`, with the three languages' component contract tests in lockstep.

## Dependency

**Blocked.** Nothing here is executable until the source plan's items are decided and built. This
file's payload is *defined by* whatever actually ships there — it is deliberately not enumerated in
advance, because items 1–4 of the source plan are each still open questions, and item 3 (interaction
verification) may well be rejected outright.

## Steps

- [ ] Step 1: **Confirm the java/react payload** — list exactly which source-plan items shipped and
      what DSL surface they added (`doesNotExist()`, `then().promotion()`, `then().erp().wasAskedFor*`,
      widened product field assertions + enriched stub mappings). Only shipped items get ported.
- [ ] Step 2: **backend-dotnet mirror** — port the confirmed payload into the .NET component test
      project, keeping it off the default `dotnet build` / `dotnet test` per the opt-in convention.
- [ ] Step 3: **backend-typescript mirror** — same payload, off the default `npm test`.
- [ ] Step 4: **Cross-language consistency check** — enumerate every contract test / DSL verb in all
      three languages side by side and confirm parity; flag anything present in one and missing in
      another (per the CLAUDE.md consistency-check rule: enumerate concretely, never conclude "no
      changes needed" from a quick read).

## Constraints inherited from the source plan

- **Do not "fix" the real-driver no-op convention.** `returns*` seeding is deliberately a no-op in
  REAL mode because a real external system cannot be configured — `ClockRealDriver.returnsTime` says
  so explicitly, and `TaxRealDriver.returnsTaxRate` / `ErpRealDriver.returnsPromotion` follow the same
  pattern. It is what lets one scenario script run in both modes. The .NET and TS real drivers carry
  the identical shape; leave them alone.
- The stub-vs-real field-set item is viable for ERP **products** only (where `returnsProduct` really
  does POST to the simulator), not for promotion or clock/tax.
- Component layer stays in-process, zero-infra, $0 — no real-ERP target
  (`ErpRealContractTest` stays a system-test concern).
- Component/contract tests stay **off the default build** in every language.

## Notes

- If the source plan ships nothing, delete this file rather than leaving a mirror of an empty set.
- Distinct from `20260616-0830-component-pact-layer-other-multitier-backends.md`: that file is about
  the component/Pact **layer existing at all** in .NET/TS; this one is about porting the specific
  **stub-contract test items** on top of it. That file is a prerequisite for this one.
