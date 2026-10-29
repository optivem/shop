# 2026-07-22 12:16 UTC — make `String` the *only* money/rate surface in the Java test DSLs

**Status:** 🟡 Drafted 2026-07-22 — grounded in a call-site census of both Java test trees.

**Origin:** Raised by the user immediately after `20260720-1350` landed, questioning whether the
`double` sugar earns its keep at all. The census says it mostly doesn't.

**Related:** Revisits the **"guidance, not enforcement"** decision recorded in
`plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md` → *Agreed target*.
That section stands except for this one point, which this plan proposes to flip.

> ⚠️ **Sequence before the .NET/TS mirror** (`20260720-1055` Step 3). Deciding this first means
> porting **one** surface form to two more languages instead of two. After the mirror it is triple
> the churn.

## TL;DR

**Why:** Keeping both `String` and numeric surfaces means maintaining two forms across three
languages forever, so each layer can carry an idiom it does not actually use. The census shows the two
Java layers already have opposite, internally-consistent cultures — `backend-java` is 25:2 in favour
of `String`, `system-test/java` is 31:0 in favour of numerics. The `double` overloads added to
`backend-java` in `85c5b65c` have essentially no callers.

**End result:** Money and rate values are written as `String` literals everywhere in both Java test
DSLs. The numeric overloads are gone, so there is one form to keep in sync across languages instead
of two, and no test can hand the DSL a value the language already rounded.

## The census (2026-07-22)

| | numeric literal | `String` literal | variable |
|---|---|---|---|
| `system-test/java/src/test` — assertions | **31** | 0 | 6 |
| `system-test/java/src/test` — `with*` setup | **29** | — | — |
| `backend-java/src/componentTest` — assertions | 2 | **25** | 6 |
| `backend-java/src/componentTest` — `with*` setup | 2 | — | — |

Reproduce with the grep in this plan's commit message, or re-run the counts before starting — they
will drift.

## Rationale

- **Vocabulary parity was the wrong kind of parity.** Two surface forms × three languages is a
  standing maintenance cost. Converging the *culture* gives parity that needs no upkeep.
- **The author's decimal is already gone by the time the DSL sees it.** `hasTotalPrice(120.00)` is a
  `double` at the language level before any DSL code runs. `"120.00"` is the only form where what the
  author wrote survives to the comparison.
- **`String` carries scale.** `"120.00"` states two decimal places of currency; `120.0` states
  nothing. Real signal in material students read.
- **TypeScript has no exact decimal anyway** — it is going string-canonical regardless (see the
  mirror plan's *Agreed target*). One surface form across all three languages falls out of that.

## Non-goals

- **Non-money numerics stay numeric.** `hasQuantity(int)`, `hasUsageLimit(int)`, `hasUsedCount(int)`
  are counts, not amounts. Do not touch them.
- **Internal canonical types do not change.** `system-test/java` stays `BigDecimal`-canonical,
  `backend-java` stays `String`-canonical; both already terminate in a value-based decimal compare.
  This plan is about the *surface* only — decision 2 of the agreed target, not decision 1.
- **Request DTOs are already `String`** and stay that way, for the negative-test reason documented on
  `PublishCouponRequest`.
- **No .NET / TypeScript** — that is the mirror plan, which should run *after* this.

## Open question — resolve before Item 1

- [ ] **Is the breaking change acceptable?** Deleting the numeric overloads breaks any cloned student
      repo that calls them, and contradicts the "nothing is deleted" principle both source plans were
      built on. That principle existed to avoid touching call sites; this plan deliberately touches
      ~60. **Recommendation: accept it** — this is a teaching template where the DSL's shape *is* the
      lesson, and carrying a discouraged second form to protect old clones teaches the wrong thing.
      If rejected, close this plan rather than half-doing it: keeping the overloads but documenting
      them as discouraged is the status quo already.

## Items

### Item 1 — Convert `system-test/java` call sites to `String`

The bulk of the work: ~60 sites, all mechanical.

- [ ] Assertions: 31 numeric-literal call sites → `String` literals, preserving currency scale
      (`120.0` → `"120.00"` where the amount is money; rates keep their written form, `0.20` →
      `"0.20"`).
- [ ] Setup steps: 29 numeric `with*` call sites → `String`.
- [ ] The 6 variable-passing sites already pass `String` variables — verify, do not change.

Verification: `./gradlew compileTestJava`. **Behaviour-affecting** — a mis-scaled literal is a real
assertion change, so a system-test run is required before commit. **Ask the user; never
self-initiate.**

### Item 2 — Convert `backend-java`'s numeric outliers

- [ ] 2 assertion sites + 2 `with*` sites → `String`. Trivial; the file's 25 other sites already show
      the idiom.

### Item 3 — Delete the numeric overloads

**Only after Items 1–2 land and pass.** The compiler is the safety net: once no call site passes a
numeric literal, removing the overloads either compiles or names exactly what was missed.

- [ ] `system-test/java`: remove `double` overloads from `ThenOrder`/`ThenOrderImpl`,
      `ThenProduct`, `ThenCountry`, `ThenCoupon`, and the corresponding verification-layer `double`
      overloads.
- [ ] `backend-java`: same, including the six added in `85c5b65c`.
- [ ] `Converter.fromDouble` becomes unreachable in **both** testkits once the numeric surface is
      gone — delete it in both, and drop `backend-java`'s `testkit/common/Converter` entirely if
      `fromDouble` was its only member.

### Item 4 — State the rule where it will be read

- [ ] Javadoc on both `ViewOrderVerification` classes: money and rates are written as `String`
      literals; there is no numeric form.
- [ ] Update the mirror plan's *Agreed target* table — "Preferred surface" becomes "Only surface", and
      the "Guidance, not enforcement" paragraph is replaced by this plan's outcome.

### Item 5 — Re-run the census

- [ ] Confirm zero numeric money/rate call sites remain in either tree, and that non-money counts
      (`hasQuantity` etc.) are untouched. Per the CLAUDE.md consistency rule: enumerate, do not
      eyeball.
