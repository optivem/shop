# Mirror the backend-java testkit alignment into backend-dotnet / backend-typescript (DEFERRED)

**Source plan:** `plans/20260720-1035-backend-java-testkit-alignment-followups.md` (cross-cutting
question + Item 1 scope + Item 5, split out 2026-07-20 during `/refine-plan`).
**Status:** Deferred — execute only **after** the Java-side renames and overload work land and their
shape is settled.
**Scope (this file):** `system/multitier/backend-dotnet` + `system/multitier/backend-typescript`, plus
the `system-test` .NET/TypeScript halves where Item 5 reaches them. **Monolith stays untouched.**

## TL;DR

**Why:** The `backend-java` test-support restructure (`03798adf`, `6397c3b1`) was Java-only, and the
follow-up decisions on top of it are also Java-first. That leaves .NET and TypeScript **further out of
sync than before the restructure**. CLAUDE.md's "check all languages" rule says they should catch up —
but only once the Java shape stops moving, so we do not port a vocabulary that is still being argued
about. Same sequencing as
`plans/deferred/20260720-1118-component-stub-contract-cross-language-mirror.md`.

**End result:** `backend-dotnet` and `backend-typescript` carry the same driver-port vocabulary and the
same assertion-overload surface as `backend-java`, with the three languages' testkits in lockstep.

## Dependency

**Blocked**, and additionally **unsurveyed** — the .NET and TypeScript harnesses have not been looked
at once since the restructure. The first deliverable is therefore a survey, not a port: it is entirely
possible those harnesses are structurally different enough that parts of the Java payload do not apply.
Do not commit to full convergence before Step 1 reports.

## Steps

- [ ] Step 1: **Survey** `backend-dotnet` and `backend-typescript` test support against the
      restructured `backend-java` one. Enumerate concretely (per the CLAUDE.md consistency-check rule):
      every driver port + method, every `Then*` step + signature, side by side across the three
      languages. Output is a gap table, not a verdict. **This step sizes the rest of the file.**
- [ ] Step 2: **Driver-port vocabulary mirror** (from source plan Item 1). Java renames the ten
      stub-programming methods on `ErpDriver` / `TaxDriver` / `ClockDriver` to match their DSL callers
      (`stubProduct` → `returnsProduct`, `stubTaxError` → `failsForCountry`, …). Apply the equivalent
      rename in .NET and TypeScript — only where Step 1 confirms the same port shape exists.
- [ ] Step 3: **Assertion-overload rollout** (from source plan Item 5). Java converges on
      `String`-canonical money/rate assertions with `double` as a delegating convenience
      (`BigDecimal.toPlainString()` equivalent per language). Mirror in .NET and TypeScript, including
      the `system-test` halves. **Additive only — nothing is deleted, so no call site changes.**
- [ ] Step 4: **Cross-language consistency check** — re-run the Step 1 enumeration and confirm parity;
      flag anything present in one language and missing in another. Never conclude "no changes needed"
      from a quick read.

## Constraints inherited from the source plan

- **`returns*` is not stub-specific language.** The rename is justified precisely because the name
  describes observable behaviour that stays true for a real implementor — `system-test`'s `ErpDriver`
  already uses `returnsProduct(...)` on a port that `ErpRealDriver` implements. Do not "correct" it
  back to `stub*` in .NET/TS on the grounds that the adapter is a stub.
- **Do not "fix" the real-driver no-op convention.** `returns*` seeding is deliberately a no-op in REAL
  mode because a real external system cannot be configured. The .NET and TS real drivers carry the
  identical shape; leave them alone.
- **`String` is canonical for money, `double` is sugar.** The whole point of Item 5 is avoiding float
  equality on monetary values. A .NET/TS mirror that asserts on `decimal`/`number` directly defeats it.
- Component/contract tests stay **off the default build** in every language.

## Notes

- If the Java-side work ships only partially, port only what shipped — and if it ships nothing, delete
  this file rather than leaving a mirror of an empty set.
- Distinct from `20260720-1118-component-stub-contract-cross-language-mirror.md`: that file ports
  specific **stub-contract test items**; this one ports the **testkit vocabulary and assertion
  surface** underneath them. They touch the same projects and should be sequenced together if both
  become live.
