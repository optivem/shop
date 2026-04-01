package com.optivem.eshop.systemtest.latest.acceptance;

import com.optivem.eshop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.optivem.eshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"3.5", "lala", "invalid-quantity"})
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
    void shouldRejectOrderForNonExistentProduct() {
        scenario
                .when().placeOrder()
                    .withSku("NON-EXISTENT-SKU-12345")
                    .withQuantity(1)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptySku(String sku) {
        scenario
                .when().placeOrder()
                    .withSku(sku)
                    .withQuantity(1)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"-10", "-1", "0"})
    void shouldRejectOrderWithNonPositiveQuantity(String quantity) {
        scenario
                .when().placeOrder()
                    .withQuantity(quantity)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyQuantity(String quantity) {
        scenario
                .when().placeOrder()
                    .withQuantity(quantity)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must not be empty");
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
}
