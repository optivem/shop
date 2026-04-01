package com.optivem.eshop.systemtest.legacy.mod07.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.optivem.eshop.systemtest.legacy.mod07.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;

class PlaceOrderNegativeTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithInvalidQuantity() {
        app.shop().placeOrder().sku(SKU).country(COUNTRY).quantity("invalid-quantity").execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNonExistentSku() {
        app.shop().placeOrder().sku("NON-EXISTENT-SKU-12345").quantity(QUANTITY).country(COUNTRY).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNegativeQuantity() {
        app.shop().placeOrder().sku(SKU).country(COUNTRY).quantity(-10).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithZeroQuantity() {
        app.shop().placeOrder().sku("ANOTHER-SKU-67890").country(COUNTRY).quantity(0).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must be positive");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptySku(String sku) {
        app.shop().placeOrder().sku(sku).quantity(QUANTITY).country(COUNTRY).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyQuantity(String emptyQuantity) {
        app.shop().placeOrder().sku(SKU).country(COUNTRY).quantity(emptyQuantity).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"3.5", "lala"})
    void shouldRejectOrderWithNonIntegerQuantity(String nonIntegerQuantity) {
        app.shop().placeOrder().sku(SKU).country(COUNTRY).quantity(nonIntegerQuantity).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must be an integer");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptyCountry(String emptyCountry) {
        app.shop().placeOrder().sku(SKU).quantity(QUANTITY).country(emptyCountry).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("country", "Country must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithInvalidCountry() {
        app.erp().returnsProduct().sku(SKU).execute()
                .shouldSucceed();

        app.shop().placeOrder().sku(SKU).quantity(QUANTITY).country("XX").execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("country", "Country does not exist: XX");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldRejectOrderWithNullQuantity() {
        app.shop().placeOrder().sku(SKU).country(COUNTRY).quantity(null).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldRejectOrderWithNullSku() {
        app.shop().placeOrder().sku(null).quantity(QUANTITY).country(COUNTRY).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("sku", "SKU must not be empty");
    }

    @TestTemplate
    @Channel({ChannelType.API})
    void shouldRejectOrderWithNullCountry() {
        app.shop().placeOrder().sku(SKU).quantity(QUANTITY).country(null).execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("country", "Country must not be empty");
    }
}


