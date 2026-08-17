package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The component-level twin of the system test's {@code latest/acceptance/PlaceOrderPositiveTest} —
 * scenario for scenario, name for name. Everything asserted here is priced, taxed and stored by the
 * backend alone, so the backend is where it can be pinned down: same rules, no browser, no deployed
 * stack, seconds instead of minutes.
 *
 * <p>The figures match the system test's because the defaults line up ({@code ScenarioDefaults}: unit
 * price 20.00, quantity 1, country US, tax 0.07, no promotion) — so a scenario states only the
 * quantity or rate it is actually about, and the rest is filled in.
 */
class PlaceOrderPositiveComponentTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToPlaceOrderForValidInput() {
        scenario.given()
                .product().withSku("BOOK-123").withUnitPrice("20.00")
            .and().country().withCode("US").withTaxRate("0.10")
            .when().placeOrder()
                .withSku("BOOK-123")
                .withQuantity(5)
                .withCountry("US")
            .then().shouldSucceed();
    }

    @Test
    void orderStatusShouldBePlacedAfterPlacingOrder() {
        scenario.when().placeOrder()
            .then().shouldSucceed()
            .and().order()
                .hasStatus(OrderStatus.PLACED);
    }

    @Test
    void shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity() {
        scenario.given()
                .product().withUnitPrice("20.00")
            .when().placeOrder().withQuantity(5)
            .then().shouldSucceed()
            .and().order()
                .hasBasePrice("100.00");
    }

    @ParameterizedTest
    @CsvSource({
        "20.00, 5, 100.00",
        "10.00, 3,  30.00",
        "15.50, 4,  62.00",
        "99.99, 1,  99.99",
    })
    void shouldPlaceOrderWithCorrectBasePrice(String unitPrice, int quantity, String basePrice) {
        scenario.given()
                .product().withUnitPrice(unitPrice)
            .when().placeOrder().withQuantity(quantity)
            .then().shouldSucceed()
            .and().order()
                .hasBasePrice(basePrice);
    }

    @Test
    void orderPrefixShouldBeOrd() {
        scenario.when().placeOrder()
            .then().shouldSucceed()
            .and().order()
                .hasOrderNumberPrefix("ORD-");
    }

    @Test
    void discountRateShouldBeAppliedForCoupon() {
        scenario.given()
                .coupon().withCouponCode("SUMMER2025").withDiscountRate("0.15")
            .when().placeOrder().withCouponCode("SUMMER2025")
            .then().shouldSucceed()
            .and().order()
                .hasAppliedCoupon("SUMMER2025")
                .hasDiscountRate("0.15");
    }

    @Test
    void discountRateShouldNotBeAppliedWhenThereIsNoCoupon() {
        scenario.when().placeOrder()
            .then().shouldSucceed()
            .and().order()
                .hasNoAppliedCoupon()
                .hasDiscountRate("0.00")
                .hasDiscountAmount("0.00");
    }

    @Test
    void subtotalPriceShouldBeBasePriceMinusDiscountAmountWhenThereIsACoupon() {
        scenario.given()
                .coupon().withDiscountRate("0.15")
            .and().product().withUnitPrice("20.00")
            .when().placeOrder().withCouponCode().withQuantity(5)
            .then().shouldSucceed()
            .and().order()
                .hasAppliedCoupon()
                .hasDiscountRate("0.15")
                .hasBasePrice("100.00")
                .hasDiscountAmount("15.00")   // 100.00 x 0.15
                .hasSubtotalPrice("85.00");   // 100.00 - 15.00
    }

    @Test
    void subtotalPriceShouldBeSameAsBasePriceWhenThereIsNoCoupon() {
        scenario.given()
                .product().withUnitPrice("20.00")
            .when().placeOrder().withQuantity(5)
            .then().shouldSucceed()
            .and().order()
                .hasBasePrice("100.00")
                .hasDiscountAmount("0.00")
                .hasSubtotalPrice("100.00");
    }

    @ParameterizedTest
    @CsvSource({
        "UK, 0.09",
        "US, 0.20",
    })
    void correctTaxRateShouldBeUsedBasedOnCountry(String country, String taxRate) {
        scenario.given()
                .country().withCode(country).withTaxRate(taxRate)
            .when().placeOrder().withCountry(country)
            .then().shouldSucceed()
            .and().order()
                .hasTaxRate(taxRate);
    }

    @ParameterizedTest
    @CsvSource({
        "UK, 0.09,  50.00,  4.50,  54.50",
        "US, 0.20, 100.00, 20.00, 120.00",
    })
    void totalPriceShouldBeSubtotalPricePlusTaxAmount(
            String country, String taxRate, String subtotalPrice, String taxAmount, String totalPrice) {
        scenario.given()
                .country().withCode(country).withTaxRate(taxRate)
            .and().product().withUnitPrice(subtotalPrice)
            .when().placeOrder().withCountry(country).withQuantity(1)
            .then().shouldSucceed()
            .and().order()
                .hasTaxRate(taxRate)
                .hasSubtotalPrice(subtotalPrice)
                .hasTaxAmount(taxAmount)
                .hasTotalPrice(totalPrice);
    }

    @Test
    void couponUsageCountShouldBeIncrementedAfterItHasBeenUsed() {
        scenario.given()
                .coupon().withCouponCode("SUMMER2025")
            .when().placeOrder().withCouponCode("SUMMER2025")
            .then().shouldSucceed()
            .and().coupon("SUMMER2025")
                .hasUsedCount(1);
    }

    @Test
    void orderTotalShouldIncludeTax() {
        scenario.given()
                .country().withCode("DE").withTaxRate("0.19")
            .when().placeOrder().withCountry("DE")
            .then().shouldSucceed()
            .and().order()
                .hasSubtotalPrice("20.00")
                .hasTaxRate("0.19")
                .hasTotalPrice("23.80");      // 20.00 x 1.19
    }

    @Test
    void orderTotalShouldReflectCouponDiscount() {
        scenario.given()
                .coupon().withCouponCode("DISC10").withDiscountRate("0.10")
            .when().placeOrder().withCouponCode("DISC10")
            .then().shouldSucceed()
            .and().order()
                .hasSubtotalPrice("18.00")    // 20.00 - 10%
                .hasDiscountRate("0.10")
                .hasAppliedCoupon("DISC10")
                .hasTotalPrice("19.26");      // 18.00 x 1.07 (default tax)
    }

    @Test
    void orderTotalShouldApplyCouponDiscountAndTax() {
        scenario.given()
                .coupon().withCouponCode("COMBO10").withDiscountRate("0.10")
            .and().country().withCode("GB").withTaxRate("0.20")
            .when().placeOrder().withCountry("GB").withCouponCode("COMBO10")
            .then().shouldSucceed()
            .and().order()
                .hasSubtotalPrice("18.00")    // 20.00 - 10%
                .hasDiscountRate("0.10")
                .hasTaxRate("0.20")
                .hasAppliedCoupon("COMBO10")
                .hasTotalPrice("21.60");      // 18.00 x 1.20
    }

    @Test
    void appliesActivePromotionDiscount() {
        scenario.given()
                .product().withUnitPrice("10.00")
            .and().promotion().withActive(true).withDiscount("0.9")
            .and().country().withTaxRate("0.10")
            .when().placeOrder().withQuantity(2)
            .then().shouldSucceed()
            .and().order()
                .hasSubtotalPrice("18.00")   // 20.00 x 0.9
                .hasTaxAmount("1.80")        // 18.00 x 0.10
                .hasTotalPrice("19.80");
    }
}
