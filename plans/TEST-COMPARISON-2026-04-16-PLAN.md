# System Test Comparison — Action Plan (2026-04-16)

**Source:** [TEST-COMPARISON-2026-04-16.md](../reports/TEST-COMPARISON-2026-04-16.md)  
**Date:** 2026-04-16  
**Target repo:** `starter` only. Do NOT modify `eshop-tests`.

---

## TypeScript

### Latest — Missing/Wrong Assertions

- [ ] (DIFF-L1): **PlaceOrderPositive** — `shouldBeAbleToPlaceOrderForValidInput` missing entire given-setup and when-params. Java/.NET have `.given().product().withSku("ABC").withUnitPrice(20.00).and().country().withCode("US").withTaxRate(0.10)` and `.when().placeOrder().withSku("ABC").withQuantity(5).withCountry("US")`. TS just has `.when().placeOrder().then().shouldSucceed()`.

- [ ] (DIFF-L8): **PlaceOrderPositive** — `couponUsageCountHasBeenIncrementedAfterItsBeenUsed` asserts on wrong entity (order instead of coupon). Should be `.and().coupon(code).hasUsedCount(1)` not `.and().order().hasAppliedCouponCode(code)`.

- [ ] (DIFF-L19): **CancelOrderNegativeIsolated** — Missing `.and().order().hasStatus(OrderStatus.PLACED)` after `.shouldFail().errorMessage(BLACKOUT_ERROR)`.

- [ ] (DIFF-L5): **PlaceOrderPositive** — `subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon` missing `.hasAppliedCoupon()` and `.hasDiscountRate(0.15)` assertions.

- [ ] (DIFF-L6): **PlaceOrderPositive** — `totalPriceShouldBeSubtotalPricePlusTaxAmount` missing `.hasTaxRate(taxRate)` assertion.

- [ ] (DIFF-L3): **PlaceOrderPositive** — `discountRateShouldBeNotAppliedWhenThereIsNoCoupon` missing `.withCouponCode(null)` on the when-step.

- [ ] (from DIFF-L9): **PlaceOrderNegative** — Add standalone `shouldRejectOrderWithInvalidQuantity` test (currently folded into parameterized `shouldRejectOrderWithNonIntegerQuantity`). Match Java's standalone approach.

### Latest — DSL Naming

- [ ] (DIFF-L4): **DSL layer (systematic)** — TS uses `hasAppliedCouponCode` everywhere, Java/.NET use `hasAppliedCoupon`. Rename in TS DSL source + all test files.

- [ ] (DIFF-L2): **PlaceOrderPositive** — Test name `discountRateShouldNotBeAppliedWhenThereIsNoCoupon` differs from Java/.NET `discountRateShouldBeNotAppliedWhenThereIsNoCoupon`.

### Latest — Test Data Mismatches

- [ ] (DIFF-L21): **PublishCouponNegative** — `cannotPublishCouponWithZeroOrNegativeDiscount` uses coupon code `'INVALID'`, Java/.NET use `'INVALID-COUPON'`.

- [ ] (DIFF-L22): **PublishCouponNegative** — `cannotPublishCouponWithZeroOrNegativeUsageLimit` uses `.withDiscountRate(0.1)`, Java/.NET use `.withDiscountRate(0.15)`.

- [ ] (DIFF-L11–L15): **PlaceOrderNegative** — Several tests add `.withQuantity(1)` that Java/.NET don't have. Affected: `shouldRejectOrderWithNonExistentSku`, `shouldRejectOrderWithEmptySku`, `shouldRejectOrderWithInvalidCountry`, `cannotPlaceOrderWithNonExistentCoupon`, `shouldRejectOrderWithEmptyCountry`. Decide: add to Java/.NET or remove from TS.

### Latest — Contract Tests

- [ ] (from DIFF-G16): **Clock contract tests (latest)** — `.withTime("2024-01-02T09:00:00Z")` should be `.withTime()` (no argument) to match Java. Check all clock contract test files.

- [ ] (DIFF-L25): **ClockStubContractIsolatedTest** — Has extra `shouldBeAbleToGetTime` that Java/.NET don't have. Remove.

- [ ] (DIFF-L26): **TaxStubContractTest** — Type inconsistency: real uses string `'0.09'`, stub uses number `0.09`. Make consistent.

### Latest — Structural

- [ ] (DIFF-L7): **PlaceOrderPositive** — `totalPriceShouldBeSubtotalPricePlusTaxAmount` given-order is `product` then `country`, Java/.NET is `country` then `product`. Reorder to match.

- [ ] (DIFF-L23): **ViewOrderNegative** — Only runs API channel. Java/.NET run API + UI for first row. Add UI.

### Latest — Review

- [ ] (DIFF-L20): **PublishCouponPositive** — `shouldBeAbleToPublishCouponWithEmptyOptionalFields` uses `undefined`, Java/.NET use `null`. May be intentional language difference.

### Legacy

- [ ] (DIFF-G3): **mod03 PlaceOrderNegative** — Uses `'3.5'` as invalid quantity, Java/.NET use `"invalid-quantity"`. Align.

- [ ] (DIFF-G5): **mod05 PlaceOrderPositive** — Missing assertions: `hasQuantity`, `hasUnitPrice`, `hasTotalPriceGreaterThanZero`.

- [ ] (DIFF-G8, G9): **mod08 PlaceOrderPositive** — Splits into two tests, Java/.NET have one combined. Also missing assertions. Decide structure.

- [ ] (DIFF-G17): **mod11 ClockStubContractIsolatedTest** — Has extra `shouldBeAbleToGetTime` (same as Step 17). Remove.

---

## Cross-Language Decisions Needed

- **DIFF-L11–L15 (`.withQuantity(1)`)**: Add to Java/.NET or remove from TS?
- **DIFF-G8/G9 (mod08 structure)**: TS splits into two tests vs Java/.NET one combined. Decide structure.

---

## Investigations

- [ ] **DSL: skip `.given()` when no setup needed** — Investigate whether `scenario.then()` (without `.given()`) is possible across all three DSL implementations. Would simplify contract tests like `shouldBeAbleToGetTime` which currently use `.given().then()` with no actual setup.

---

## Structural Observations (No Action)

- **DIFF-G1, G2, G4**: TS legacy mod03/mod04 uses unified channel-aware tests while Java/.NET use separate Api/Ui classes. TS is structurally ahead.
- **DIFF-G6**: Java/.NET mod07 use use-case DSL while TS uses scenario DSL. TS is at a later DSL stage.
- **DIFF-G12, G13**: mod10 isolated tests are consistent across all languages.
