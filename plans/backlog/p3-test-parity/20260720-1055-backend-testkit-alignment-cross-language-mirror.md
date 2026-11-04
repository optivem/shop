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
about. Its sibling `20260720-1118-component-stub-contract-cross-language-mirror` was deleted on 2026-11-03
because its payload (`20260717-1015`) shipped nothing.

**End result:** `backend-dotnet` and `backend-typescript` carry the same driver-port vocabulary and the
same assertion-overload surface as `backend-java`, with the three languages' testkits in lockstep.

## ▶ Next executable step (resume here)

Steps 1–2 are done (2026-11-03). Step 3 is narrowed to the `system-test` .NET/TS halves and is **not
scheduled** by meta-plan `20261104-1148` (was `20261103-1021`) (Q-P3d ran Steps 1–2 only); pick it up only on a new
decision. Step 4 follows Step 3.

## Decisions (2026-11-03)

- Run **Steps 1–2 only** (survey + driver-port vocabulary mirror), sequenced **after** U6
  `money-strings-end-to-end` lands (meta-plan `20261104-1148` (was `20261103-1021`), Q-A / Q-P3d). Done.
- **Steps 3–4 re-assessed against what U6 shipped:** U6 did not touch the .NET/TS DSL surfaces, so
  Step 3 is not covered by it and stays, narrowed by the survey below.

## Survey result (Step 1, 2026-11-03)

Only Java has a backend testkit. `backend-java` and `backend-clean-java` match (driver ports
`ErpDriver` / `TaxDriver` / `ClockDriver` / `MyShopDriver`, SUT readers, scenario DSL with Given /
When / Then stages and `Then*` steps; clean-java has simulator clients in place of `ErpRealDriver`).

| Item | Java | .NET (`backend-dotnet/Tests`) | TS (`backend-typescript/test`) |
|---|---|---|---|
| External-system driver ports | yes | no: private helpers in `Contract/BackendPactVerificationTest.cs` | no: public methods on `test/support/component-harness.ts` `ComponentHarness` |
| `returns*` / `returnsNo*` stubs | yes | yes, renamed in Step 2 | yes, renamed in Step 2 (money params still `number`) |
| `failsFor*`, `goTo*`, `assume()...shouldBeRunning()` | yes | missing (integration tests use generic path stubs, like Java's legacy twin) | missing (same) |
| `MyShopDriver` / SUT readers | yes | missing (raw `HttpClient` + local helpers) | missing (supertest agents) |
| Scenario DSL, `Then*` steps | yes, money/rates `String` | missing; inline `Assert`, string-typed `AssertWireString` | missing; inline `expect(...).toBe('20.00')` |

Step 2 renamed .NET `StubClock`/`StubProduct`/`StubPromotion`/`StubNoProduct`/`StubTax`/`StubNoTax` and
TS `stubClock`/`stubProduct`/`stubPromotion`/`stubProductMissing`/`stubTax`/`stubTaxMissing` to
`returnsTime`/`returnsProduct`/`returnsPromotion`/`returnsNoProduct`/`returnsTaxRate`/`returnsNoTaxRate`.

So the backend half of Step 3 has nothing to port: neither harness has assertion overloads, and their
inline assertions already compare wire strings. Converging the harness *shape* (extracting driver
ports and a DSL) is new work, not a mirror, and is out of this plan.

## Steps

- [ ] Step 3: **Port the String-only surface to the `system-test` .NET/TS halves** (from source plan
      Item 5, as settled by U6). Java's money/rate assertions take `String` only. Delete the .NET/TS
      numeric overloads (`IThenOrder.HasUnitPrice(decimal)` and its siblings, TS `hasUnitPrice(number)`,
      the `string | number` params) and convert their call sites to strings. Optional, backend side:
      `ComponentHarness.returnsProduct` / `returnsPromotion` / `returnsTaxRate` take `number`; moving
      them to `string` needs a raw-body `StubServer` variant so the stub wire keeps the numeric
      literal Java's `ErpStubDriver` writes.
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

**2. Surface form — what the test author types.**
**`String`, the only surface** (U6 deleted the numeric convenience; see below).

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
| Only surface | `String` — in all three languages |
| Numeric surface (`double`/`decimal`/`number`) | none (deleted in Java; .NET/TS port the deletion) |
| Request DTOs | `String` always — non-negotiable; negative tests must be able to send `"abc"`, `"-0.5"` |

**Enforcement, not guidance (settled by U6, 2026-11-03).** `money-strings-end-to-end` deleted the
numeric overloads and `Converter.fromDouble` from `system-test/java`, `backend-java` and
`backend-clean-java`, so a numeric literal such as `hasTotalPrice(120.00)` no longer compiles, and it put
money and rates on the wire as fixed-scale JSON strings (`"20.00"`, `"0.2000"`) in all seven backends.
Step 3 therefore ports one surface form, not two.

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
