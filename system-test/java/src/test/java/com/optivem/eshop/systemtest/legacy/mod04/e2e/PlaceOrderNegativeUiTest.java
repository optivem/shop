package com.optivem.eshop.systemtest.legacy.mod04.e2e;

import com.optivem.eshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeUiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopUiClient();
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku(createUniqueSku(SKU));
        newOrderPage.inputQuantity("invalid-quantity");
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }

    @Test
    void shouldRejectOrderWithNonExistentSku() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku("NON-EXISTENT-SKU-12345");
        newOrderPage.inputQuantity(QUANTITY);
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("Product does not exist for SKU: NON-EXISTENT-SKU-12345");
        });
    }

    @Test
    void shouldRejectOrderWithNegativeQuantity() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku(createUniqueSku(SKU));
        newOrderPage.inputQuantity("-10");
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be positive");
        });
    }

    @Test
    void shouldRejectOrderWithZeroQuantity() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku(createUniqueSku(SKU));
        newOrderPage.inputQuantity("0");
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be positive");
        });
    }

    @Test
    void shouldRejectOrderWithEmptySku() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku("");
        newOrderPage.inputQuantity(QUANTITY);
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("SKU must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithEmptyQuantity() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku(createUniqueSku(SKU));
        newOrderPage.inputQuantity("");
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithNonIntegerQuantity() {
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();

        newOrderPage.inputSku(createUniqueSku(SKU));
        newOrderPage.inputQuantity("3.5");
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }

}

