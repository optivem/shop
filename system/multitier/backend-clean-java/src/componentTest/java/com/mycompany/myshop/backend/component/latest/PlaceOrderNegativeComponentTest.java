package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceOrderNegativeComponentTest extends BaseComponentTest {

    private static final String VALIDATION_FAILED = "The request contains one or more validation errors";

    @Test
    void shouldRejectOrderWithInvalidQuantity() {
        scenario.when().placeOrder()
                .withQuantity("invalid-quantity")
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @Test
    void shouldRejectOrderWithNonExistentSku() {
        scenario.given()
                .product().withSku("NON-EXISTENT-SKU-12345").doesNotExist()
            .when().placeOrder().withSku("NON-EXISTENT-SKU-12345")
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    @Test
    void shouldRejectOrderWithNegativeQuantity() {
        scenario.when().placeOrder()
                .withQuantity(-10)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @Test
    void shouldRejectOrderWithZeroQuantity() {
        scenario.when().placeOrder()
                .withQuantity(0)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectOrderWithEmptySku(String emptySku) {
        scenario.when().placeOrder()
                .withSku(emptySku)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectOrderWithEmptyQuantity(String emptyQuantity) {
        scenario.when().placeOrder()
                .withQuantity(emptyQuantity)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.5", "lala"})
    void shouldRejectOrderWithNonIntegerQuantity(String nonIntegerQuantity) {
        scenario.when().placeOrder()
                .withQuantity(nonIntegerQuantity)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectOrderWithEmptyCountry(String emptyCountry) {
        scenario.when().placeOrder()
                .withCountry(emptyCountry)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("country", "Country must not be empty");
    }

    @Test
    void shouldRejectOrderWithInvalidCountry() {
        scenario.given()
                .country().withCode("XX").doesNotExist()
            .when().placeOrder().withCountry("XX")
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("country", "Country does not exist: XX");
    }

    @Test
    void shouldRejectOrderWithNullQuantity() {
        scenario.when().placeOrder()
                .withQuantity(null)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @Test
    void shouldRejectOrderWithNullSku() {
        scenario.when().placeOrder()
                .withSku(null)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @Test
    void shouldRejectOrderWithNullCountry() {
        scenario.when().placeOrder()
                .withCountry(null)
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("country", "Country must not be empty");
    }

    @Test
    void cannotPlaceOrderWithNonExistentCoupon() {
        scenario.when().placeOrder()
                .withCouponCode("INVALIDCOUPON")
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("couponCode", "Coupon code INVALIDCOUPON does not exist");
    }

    @Test
    void cannotPlaceOrderWithCouponThatHasExceededUsageLimit() {
        scenario.given()
                .coupon().withCouponCode("LIMITED2024").withUsageLimit(2)
            .and().order().withCouponCode("LIMITED2024")
            .and().order().withCouponCode("LIMITED2024")
            .when().placeOrder().withCouponCode("LIMITED2024")
            .then().shouldFail()
                .errorMessage(VALIDATION_FAILED)
                .fieldErrorMessage("couponCode", "Coupon code LIMITED2024 has exceeded its usage limit");
    }

    @Test
    void rejectsOrderDuringNewYearBlackout() {
        scenario.given()
                .clock().withTime("2026-12-31T23:59:00Z")
            .when().placeOrder()
            .then().shouldFail()
                .errorMessage("Orders cannot be placed between 23:59 and 00:00 on December 31st");
    }
}
