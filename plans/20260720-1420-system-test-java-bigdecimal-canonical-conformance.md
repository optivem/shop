# 2026-07-20 14:20 UTC — `system-test/java`: conform money/rate assertions to `BigDecimal`-canonical

**Status:** 🟡 Drafted 2026-07-20 — grounded in a full enumeration of all 11 money/rate verification
methods in `system-test/java`.

**Origin:** Extracted from Item 3 of `plans/20260720-1350-money-assertion-overload-convergence.md` at
the user's request. That plan framed these as four unrelated "precision defects"; the enumeration
showed they are all the same thing — deviations from a convention the codebase already follows. They
are separated here because they change assertion *semantics*, whereas the parent plan is purely
additive.

**Related:** The parent plan's remaining items (overload parity) depend on the canonical type being
settled. This plan settles it. Execute this plan first.

## TL;DR

**Why:** `system-test/java` has an established convention for money/rate assertions — `BigDecimal` is
canonical, `String` and `double` are thin overloads that delegate through `Converter.toBigDecimal(...)`,
and the comparison is always `isEqualByComparingTo`. Nine of eleven verification methods follow it.
The two that don't, plus two one-line deviations inside methods that otherwise do, are the only places
where a money assertion can silently lose precision or fail on scale.

**End result:** All eleven money/rate verification methods in `system-test/java` follow the
`BigDecimal`-canonical pattern, with the single exception of `couponHasDiscountRate`, which is capped
by a primitive-`double` DTO field and is documented as such rather than papered over.

## The convention (already in the code — this plan does not invent it)

```java
foo(BigDecimal expected) { assertThat(actual).isEqualByComparingTo(expected); }  // canonical
foo(double expected)     { return foo(Converter.toBigDecimal(expected)); }
foo(String expected)     { return foo(Converter.toBigDecimal(expected)); }
```

Followed by: `ViewOrder.totalPrice`, `basePrice`, `discountAmount`, `taxRate`, `taxAmount`,
`discountRate`, and `GetProduct.price`.

`Converter.toBigDecimal(String)` is `new BigDecimal(s)` (exact); `Converter.toBigDecimal(double)` is
`BigDecimal.valueOf(d)` (shortest round-trip repr). Neither round-trips through a `String`.

Note the layering: the **DSL** layer (`ThenOrderImpl` etc.) has no canonical type and does no
conversion — every method is a one-line pass-through to the verification. All conversion lives in the
verification layer. This plan touches only the verification layer.

## ▶ Next executable step (resume here)

Start with Item 1 (`unitPrice`) — it is the only structural change; the rest are one-liners.

Verification for every item: `./gradlew build` in `system-test/java`.
**Every item in this plan changes assertion behaviour** (strictly: makes assertions stricter or more
exact), so compilation is not sufficient evidence. A system-test run is required before commit —
**ask the user before running it; never self-initiate.**

## Items

### Item 1 — `ViewOrder.unitPrice` is `double`-canonical

`ViewOrderVerification.java:49-60`. The only `ViewOrder` money method with no `BigDecimal` overload:
`unitPrice(double)` holds the assertion body, and `unitPrice(String)` reaches it via
`Converter.toDouble(...)` — so a `String` expectation is parsed to `double` and back, losing exactness
for values outside `double`'s exact range.

- [ ] Add `unitPrice(BigDecimal)` carrying the assertion body (`isEqualByComparingTo`, same fail
      message wording as its siblings).
- [ ] Reduce `unitPrice(double)` to `return unitPrice(Converter.toBigDecimal(expected));`.
- [ ] Repoint `unitPrice(String)` to `return unitPrice(Converter.toBigDecimal(expected));`.

### Item 2 — `ViewOrder.subtotalPrice(String)` routes through `double`

`ViewOrderVerification.java:110-112`. `subtotalPrice(BigDecimal)` exists two methods above, but the
`String` overload calls `Converter.toDouble(...)` instead of `toBigDecimal(...)`. Same lossy
round-trip as Item 1, but here it is a one-word deviation, not a missing overload.

- [ ] Change `Converter.toDouble` → `Converter.toBigDecimal`.

### Item 3 — `GetTax.taxRate` uses scale-sensitive `isEqualTo`

`GetTaxVerification.java:26-31`. The canonical `BigDecimal` overload is correct in shape but compares
with `isEqualTo`, which is scale-sensitive on `BigDecimal` — `0.10` fails against `0.1`. It is the
only money/rate verification in the project that does this.

- [ ] Change `isEqualTo` → `isEqualByComparingTo`.

**This is the item most likely to surface a currently-passing assertion that was passing by luck of
scale.** Expect a failure here and treat it as the fix working, not as a regression.

### Item 4 — `BrowseCoupons.couponHasDiscountRate` cannot be made exact

`BrowseCouponsVerification.java:20-24`. Compares `coupon.getDiscountRate()` with raw `isEqualTo` on a
primitive `double` — a true float-equality comparison. It is the only one in the project.

The cause is the DTO: `BrowseCouponsResponse.CouponDto.discountRate` is a primitive `double`
(`driver/port/dtos/BrowseCouponsResponse.java:24`). No change confined to the verification layer can
make this exact.

- [ ] Add a short comment at the assertion naming the DTO field as the precision ceiling, so the next
      reader doesn't file this as an oversight.
- [ ] **Do not** add a `String` overload here as if it were a fix — it would improve call-site
      vocabulary while leaving the comparison inexact, which is worse than the honest current state.

Making it genuinely exact means changing the DTO field to `BigDecimal`, which touches the driver DTO
contract. **Out of scope here** — if wanted, it needs its own plan.

### Item 5 — `Converter.fromDouble` emits scientific notation

`common/Converter.java:28-30`. Uses `BigDecimal.valueOf(value).toString()`, which can emit scientific
notation for extreme magnitudes; `.toPlainString()` cannot.

- [ ] Decide whether to fix or delete. Under the `BigDecimal`-canonical convention nothing in the
      assertion path converts *to* `String`, so this method has no caller in that path. The parent
      plan wanted it fixed because it assumed a `String`-canonical target; that assumption is now
      reversed. **Recommendation: check for callers and delete if unused** — a correct-but-unreachable
      converter invites someone to reach for it and reintroduce the round-trip this plan removes.

## Non-goals

- **No DSL-layer changes.** The DSL (`ThenOrderImpl` and siblings) is a pass-through; overload parity
  there belongs to the parent plan.
- **No DTO contract changes** — see Item 4.
- **No `backend-java` changes.** Its verification layer already terminates every path in
  `isEqualByComparingTo`; these four deviations are `system-test/java`-only.
- **No .NET / TypeScript** — deferred to
  `plans/deferred/20260720-1055-backend-testkit-alignment-cross-language-mirror.md` Step 3. The
  equivalent enumeration has **not** been run for those languages; do not assume the same four
  deviations exist there, and do not assume they don't.
