# 2026-07-20 13:50 UTC — money/rate assertion convergence (Java)

**Status:** 🟡 Drafted 2026-07-20 — grounded in a full signature survey of both layers. The survey
**corrected the premise it inherited** (see `## The premise correction`), and surfaced four
pre-existing precision defects that were not visible when the parent item was written.

**Origin:** Item 5 of `plans/20260720-1035-backend-java-testkit-alignment-followups.md`, which decided
additive convergence on a `String`-canonical model and scheduled it as its own plan. That parent plan
is now closed.

**Related:** `plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md` Step 3
owns the eventual .NET / TypeScript rollout. This plan is **Java-only**.

## TL;DR

**Why:** The two Java test DSLs disagree about how a money or rate assertion is written — `String` in
the component layer, `double` in system-test — so the same assertion reads differently depending on
which layer you are in, and four step types offer an overload the other lacks. Underneath that
cosmetic split, the survey found genuine precision defects: one true `double == double` money
comparison, two `String` paths that round-trip through `double` and lose exactness, and one
scale-sensitive `isEqualTo`.

**End result:** Every money/rate assertion in both Java DSLs accepts both `String` and `double`, with
`String` canonical (exact `BigDecimal` comparison) and `double` a delegating convenience. Nothing is
deleted, so all 37 existing call sites keep compiling untouched.

## The premise correction

The parent item's rationale was: *"`system-test` compares money as raw `double`, so it is the weaker
end."* **That is true of exactly one method, not the layer.** The survey found:

- **`system-test` overwhelmingly already converts to `BigDecimal`** before comparing —
  `Converter.toBigDecimal(double)` is `BigDecimal.valueOf`, and the verifications use
  `isEqualByComparingTo`. Passing a `double` is *not* generally a float-equality bug.
- **Exactly one true raw-double comparison exists:** `ThenCoupon.hasDiscountRate(double)` →
  `BrowseCouponsVerification.couponHasDiscountRate(String, double)` →
  `assertThat(coupon.getDiscountRate()).isEqualTo(expected)`, where
  `BrowseCouponsResponse.CouponDto.discountRate` is a **primitive `double`**
  (`driver/port/dtos/BrowseCouponsResponse.java:24`). Exactness is capped by the DTO here, not the DSL.
- **`backend-java` has no raw double comparison anywhere** — every path terminates in
  `isEqualByComparingTo` against a `String` or `BigDecimal`. That part of the premise held.

So the correctness argument survives but shrinks: it justifies **Item 3** (the real defects) rather
than the whole convergence. Items 1–2 are parity work, and should be argued as parity, not as bug-fixing.

## Target state

`String` canonical, `double` delegating via `BigDecimal.valueOf(x).toPlainString()` — the pattern
`backend-java` already uses at `ThenOrderImpl.java:71,82`.

**Current state — `ThenOrder` (component | system-test):**

| Method | backend-java | system-test | Gap |
|---|---|---|---|
| `hasUnitPrice` | `String` | `double` | both sides |
| `hasBasePrice` | `String` | `double`+`String` | component needs `double` |
| `hasSubtotalPrice` | `String` | `double`+`String` | component needs `double` |
| `hasTotalPrice` | `String` | `double`+`String` | component needs `double` |
| `hasDiscountAmount` | `String` | `double`+`String` | component needs `double` |
| `hasTaxAmount` | `String` | `String` | system-test needs `double` |
| `hasTaxRate` | `String`+`double` ✅ | `double`+`String` ✅ | none |
| `hasDiscountRate` | `String`+`double` ✅ | `double` | system-test needs `String` |

**Other steps:**

| Method | backend-java | system-test |
|---|---|---|
| `ThenCountry.hasTaxRate` | `double` | `double` |
| `ThenCoupon.hasDiscountRate` | `String` | `double` |
| `ThenProduct.hasPrice` | `double` | `double` |

**Additivity is verified safe.** Across `system-test/java/src/test`: **31** call sites pass a numeric
literal, **6** pass a `String` variable, **0** pass a `String` literal. `String` and `double` are
unrelated types so no overload ambiguity arises; ambiguity would need a `null` literal, and there are
none.

## ▶ Next executable step (resume here)

**Execute Item 3 first, not Item 1.** It is the only item with a correctness payload, it is the
smallest, and it is independent of the parity work. Doing it first means that if this plan stalls, the
part that actually matters has landed.

Verification for every item: `./gradlew build` in `system/multitier/backend-java`, and
`./gradlew build` in `system-test/java` (compile + unit level). Item 3 changes assertion *semantics*,
so it additionally needs a system-test run to confirm no existing assertion starts failing —
**ask before running it.**

## Items

### Item 3 — Precision defects (do this first)

Four pre-existing defects, all in `system-test/java`, none introduced by this plan:

- [ ] `ViewOrderVerification.java:59` — `unitPrice(String)` round-trips through
      `Converter.toDouble(...)` before comparing. Parse straight to `BigDecimal` instead.
- [ ] `ViewOrderVerification.java:111` — `subtotalPrice(String)` has the same lossy round-trip. Same fix.
- [ ] `GetTaxVerification.java:30` — `taxRate(BigDecimal)` uses `isEqualTo`, which is **scale-sensitive**
      (`0.10` ≠ `0.1`). Change to `isEqualByComparingTo`, matching every sibling verification.
- [ ] `common/Converter.java:28` — `fromDouble(double)` uses `.toString()`, which can emit scientific
      notation for extreme magnitudes. Change to `.toPlainString()`. Currently unused by the assertion
      path, so this is pre-emptive — fix it before Items 1–2 start routing through it.

**These change assertion behaviour** (strictly: they make assertions stricter or more correct), unlike
Items 1–2. That is why they need the system-test run and why they are listed separately.

### Item 1 — Component layer gains `double` sugar

- [ ] Add delegating `double` overloads to `ThenOrder` / `ThenOrderImpl` for `hasUnitPrice`,
      `hasBasePrice`, `hasSubtotalPrice`, `hasTotalPrice`, `hasDiscountAmount`, `hasTaxAmount`.
- [ ] Add a `String` overload to `ThenProduct.hasPrice` and `ThenCountry.hasTaxRate`.

Each new `double` overload is a one-liner delegating to the `String` form via
`BigDecimal.valueOf(x).toPlainString()`. **Consider extracting that to a shared helper** — the literal
expression is currently inlined at six sites (`ThenOrderImpl:71,82`, `WhenPublishCouponImpl:39`,
`GivenCountryImpl:35`, `GivenCouponImpl:45`, `GivenProductImpl:35`, `GivenPromotionImpl:32`) and this
item would add six more.

`ThenProductImpl` / `ThenCountryImpl` assert inline against DTO `BigDecimal` getters with no
verification object, so their `String` overloads are DSL-local — no verification-layer change.

### Item 2 — System-test layer gains `String` forms

- [ ] `ThenOrder.hasUnitPrice(String)` — verification already has the overload; DSL-only change.
- [ ] `ThenOrder.hasTaxAmount(double)` — delegates to the existing `taxAmount(double)`; DSL-only.
- [ ] `ThenOrder.hasDiscountRate(String)` — **requires a new** `ViewOrderVerification.discountRate(String)`
      (currently only `BigDecimal` + `double`, line 178/186).
- [ ] `ThenProduct.hasPrice(String)` and `ThenCountry.hasTaxRate(String)` — both verifications already
      carry a `String` overload (`GetProductVerification:38`, `GetTaxVerification:38`); DSL-only.
- [ ] `ThenCoupon.hasDiscountRate(String)` — **requires a new**
      `BrowseCouponsVerification.couponHasDiscountRate(String, String)`.

On that last one: the DTO field is a primitive `double`, so a `String` overload improves the *call-site
vocabulary* but cannot make the comparison exact. **Do not present it as a precision fix.** Making it
genuinely exact means changing `BrowseCouponsResponse.CouponDto.discountRate` to `BigDecimal`, which
touches the driver DTO contract and is deliberately **out of scope** — note it, do not do it here.

## Non-goals

- **Nothing is deleted.** No `double` overload is removed, no call site is rewritten. If an item seems
  to require touching a test file, that item has been misread.
- **No .NET / TypeScript** — deferred to the cross-language mirror plan, Step 3.
- **No DTO contract changes** — see the `ThenCoupon` note above.
- **No convergence of the two layers' *structure*.** Only the assertion vocabulary is in scope; the
  layers keep their own verification objects and their own port types (settled as Item 2 of the parent
  plan: production types in the component DSL port are correct there).
