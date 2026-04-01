package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithInvalidQuantity() {
        scenario
                .when().placeOrder()
                    .withQuantity("invalid-quantity")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNonExistentSku() {
        scenario
                .when().placeOrder()
                    .withSku("NON-EXISTENT-SKU-12345")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNegativeQuantity() {
        scenario
                .when().placeOrder()
                    .withQuantity(-10)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithZeroQuantity() {
        scenario
                .when().placeOrder()
                    .withQuantity(0)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptySku(String sku) {
        scenario
                .when().placeOrder()
                    .withSku(sku)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyQuantity(String emptyQuantity) {
        scenario
                .when().placeOrder()
                    .withQuantity(emptyQuantity)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"3.5", "lala"})
    void shouldRejectOrderWithNonIntegerQuantity(String nonIntegerQuantity) {
        scenario
                .when().placeOrder()
                    .withQuantity(nonIntegerQuantity)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyCountry(String emptyCountry) {
        scenario
                .when().placeOrder()
                    .withCountry(emptyCountry)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("country", "Country must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithInvalidCountry() {
        scenario
                .when().placeOrder()
                    .withCountry("XX")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("country", "Country does not exist: XX");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldRejectOrderWithNullQuantity() {
        scenario
                .when().placeOrder()
                    .withQuantity(null)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldRejectOrderWithNullSku() {
        scenario
                .when().placeOrder()
                    .withSku(null)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldRejectOrderWithNullCountry() {
        scenario
                .when().placeOrder()
                    .withCountry(null)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("country", "Country must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPlaceOrderWithNonExistentCoupon() {
        scenario
                .when().placeOrder()
                    .withCouponCode("INVALIDCOUPON")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code INVALIDCOUPON does not exist");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPlaceOrderWithCouponThatHasExceededUsageLimit() {
        scenario
                .given().coupon()
                    .withCouponCode("LIMITED2024")
                    .withUsageLimit(2)
                .and().order()
                    .withOrderNumber("ORD-1")
                    .withCouponCode("LIMITED2024")
                .and().order()
                    .withOrderNumber("ORD-2")
                    .withCouponCode("LIMITED2024")
                .when().placeOrder()
                    .withOrderNumber("ORD-3")
                    .withCouponCode("LIMITED2024")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code LIMITED2024 has exceeded its usage limit");
    }
}



