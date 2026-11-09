package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenPlaceOrder;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

class PlaceOrderNegativeTest extends BaseAcceptanceTest {
    @Tag("UC-001.8")
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

    @Tag("UC-001.9")
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

    @Tag("UC-001.8")
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

    @Tag("UC-001.8")
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

    @Tag("UC-001.9")
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptySku(String sku) {
        scenario
                .when().placeOrder()
                    .withSku(sku)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @Tag("UC-001.8")
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyQuantity(String emptyQuantity) {
        scenario
                .when().placeOrder()
                    .withQuantity(emptyQuantity)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @Tag("UC-001.8")
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ValueSource(strings = {"3.5", "lala"})
    void shouldRejectOrderWithNonIntegerQuantity(String nonIntegerQuantity) {
        scenario
                .when().placeOrder()
                    .withQuantity(nonIntegerQuantity)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @Tag("UC-001.16")
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyCountry(String emptyCountry) {
        scenario
                .when().placeOrder()
                    .withCountry(emptyCountry)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("country", "Country must not be empty");
    }

    @Tag("UC-001.16")
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

    @Tag("UC-001.8")
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

    @Tag("UC-001.9")
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

    @Tag("UC-001.16")
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

    @Tag("UC-001.11")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPlaceOrderWithNonExistentCoupon() {
        scenario
                .when().placeOrder()
                    .withCouponCode("INVALIDCOUPON")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code INVALIDCOUPON does not exist")
                .and().noOrderPersisted();
    }

    @Tag("UC-001.11")
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

    @Tag("UC-001.12")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldNotBeAbleToPlaceOrder() {
        scenario
                .given().loggedInAsAdmin()
                .when().placeOrder()
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectReusingAnIdempotencyKeyForADifferentOrder() {
        scenario
                .given().order()
                    .withIdempotencyKey("ORDER-KEY")
                    .withQuantity("1")
                .and().loggedInAsCustomer()
                .when().placeOrder()
                    .withOrderNumber("SECOND-ORDER")
                    .withIdempotencyKey("ORDER-KEY")
                    .withQuantity("2")
                .then().shouldFail()
                    .errorMessage("Idempotency-Key has already been used with a different request");
    }

    @Tag("UC-001.10")
    @TestTemplate
    @Channel(ChannelType.API)
    @DataSource({"firstName", "First name is required"})
    @DataSource({"lastName", "Last name is required"})
    @DataSource({"street", "Street is required"})
    @DataSource({"postalCode", "Postal code is required"})
    @DataSource({"city", "City is required"})
    @DataSource({"country", "Country is required"})
    void shouldRejectOrderWithIncompleteShippingAddress(String field, String message) {
        var placeOrder = scenario
                .when().placeOrder();
        withShippingField(placeOrder, field, "")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("shippingAddress." + field, message);
    }

    @Tag("UC-001.10")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectOrderWithShippingAddressFieldOfOnlySpaces() {
        scenario
                .when().placeOrder()
                    .withShippingCity("   ")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("shippingAddress.city", "City is required");
    }

    @Tag("UC-001.10")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectOrderShippedToUnsupportedCountry() {
        scenario
                .when().placeOrder()
                    .withShippingCountry("XX")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("shippingAddress.country", "Country does not exist: XX");
    }

    private static WhenPlaceOrder withShippingField(WhenPlaceOrder placeOrder, String field, String value) {
        return switch (field) {
            case "firstName" -> placeOrder.withShippingFirstName(value);
            case "lastName" -> placeOrder.withShippingLastName(value);
            case "street" -> placeOrder.withShippingStreet(value);
            case "postalCode" -> placeOrder.withShippingPostalCode(value);
            case "city" -> placeOrder.withShippingCity(value);
            case "country" -> placeOrder.withShippingCountry(value);
            default -> throw new IllegalArgumentException("Unknown shipping address field: " + field);
        };
    }
}
