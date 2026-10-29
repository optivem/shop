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
- ~~**`String` is canonical for money, `double` is sugar.**~~ **Superseded 2026-07-22 — see
  "Agreed target" below.** This constraint predated the Java work landing and states a target that was
  reversed. Following it literally would push .NET/TS `system-test` toward `String`-canonical
  *internals*, the exact direction rejected for Java, leaving the three languages inconsistent in
  `system-test` while claiming to align them.
- Component/contract tests stay **off the default build** in every language.

## Agreed target for money/rate assertions (settled 2026-07-22)

Derived from first principles and agreed with the user, **independent of what any language currently
does**. Step 3 ports *this*, not the superseded constraint above.

The two decisions are separable, and conflating them is what caused this plan and its parents to state
a target that had to be reversed twice:

**1. Internal canonical type — what the comparison runs on.**
The language's **exact decimal type**, never the float type. `BigDecimal` (Java), `decimal` (.NET).
Comparison must be **value-based, not representation-based** — `isEqualByComparingTo`, not `equals` —
so `0.10` equals `0.1`. Scale is decided by the SUT's serialization, not the test, so a
scale-sensitive compare is a latent failure. This is what the `GetTax.taxRate` defect was.

**2. Preferred surface form — what the test author types.**
**`String`, documented as the default**, with the numeric form available as a labelled convenience.

The reason is not precision loss in the DSL — there is none for realistic money values. It is that
`hasTotalPrice(120.00)` has **already** become a `double` at the language level before any DSL code
runs; the author's exact decimal is reinterpreted as binary floating point and nothing downstream can
undo it. `"120.00"` is the only surface form where what the author wrote survives to the comparison.
`String` also carries scale — `"120.00"` states two decimal places of currency, `120.0` states
nothing — which is real signal in material students read.

**TypeScript is the binding constraint.** JS has no native exact decimal; `number` is IEEE 754. The
options are normalized-string comparison, a decimal dependency (`decimal.js` / `big.js`), or
float-with-tolerance. A dependency violates the "templates must not push cost or infra onto cloning
students" rule; tolerance is what this whole effort removed. **So TS goes string-canonical
internally** — and that is itself the strongest argument for `String`-preferred surface everywhere,
since it is the only surface form meaning the same thing in all three languages.

| | Target |
|---|---|
| Internal comparison | Exact decimal type per language, value-based compare. **TS: normalized string compare** |
| Preferred surface | `String` — documented default in all three languages |
| Numeric surface (`double`/`decimal`/`number`) | Available everywhere, labelled convenience, **not** the example form |
| Request DTOs | `String` always — non-negotiable; negative tests must be able to send `"abc"`, `"-0.5"` |

~~**Guidance, not enforcement.** The numeric overloads stay.~~ **⚠️ Under review — see
`plans/20260722-1216-string-only-money-surface.md`.** A call-site census taken the same day showed
`backend-java` is already 25:2 in favour of `String` (so the numeric overloads there have almost no
callers) while `system-test/java` is 31:0 the other way. That plan proposes **enforcement**: delete
the numeric surface entirely, leaving one form to mirror instead of two.

**Resolve that plan before running Step 3** — it decides whether this mirror ports one surface form or
two, and doing it after the mirror triples the churn. Everything else in this *Agreed target* section
stands regardless.

**Note the per-layer split in Java is compatible with this, not an exception to it.**
`system-test/java` is `BigDecimal`-canonical; `backend-java` is `String`-canonical *by documented
intent* (its `ViewOrderVerification` Javadoc: money asserted against a `String` literal "so the test
states the figure the way the domain writes it"), and AssertJ's `isEqualByComparingTo(String)`
terminates in a `BigDecimal` compare regardless. Both satisfy decision 1. Do not "converge" them.

## Notes

- If the Java-side work ships only partially, port only what shipped — and if it ships nothing, delete
  this file rather than leaving a mirror of an empty set.
- **Java side has now shipped in full** (`a731fed8`, `735cea6a`, `85c5b65c`): both Java layers carry
  `String` + numeric overloads on every money/rate step, with byte-identical signature sets, and no
  raw-float money comparison remains. Step 3 has a concrete payload to mirror.
- Distinct from `20260720-1118-component-stub-contract-cross-language-mirror.md`: that file ports
  specific **stub-contract test items**; this one ports the **testkit vocabulary and assertion
  surface** underneath them. They touch the same projects and should be sequenced together if both
  become live.
