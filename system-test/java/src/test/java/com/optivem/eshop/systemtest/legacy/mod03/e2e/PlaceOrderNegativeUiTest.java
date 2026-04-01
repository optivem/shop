package com.optivem.eshop.systemtest.legacy.mod03.e2e;

import com.optivem.eshop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeUiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopBrowser();
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(createUniqueSku(SKU));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("invalid-quantity");
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "quantity", "Quantity must be an integer");
    }

    @Test
    void shouldRejectOrderWithNonExistentSku() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill("NON-EXISTENT-SKU-12345");
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill(QUANTITY);
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    @Test
    void shouldRejectOrderWithNegativeQuantity() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(createUniqueSku(SKU));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("-10");
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "quantity", "Quantity must be positive");
    }

    @Test
    void shouldRejectOrderWithZeroQuantity() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(createUniqueSku(SKU));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("0");
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "quantity", "Quantity must be positive");
    }

    @Test
    void shouldRejectOrderWithEmptySku() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill("");
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill(QUANTITY);
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "sku", "SKU must not be empty");
    }

    @Test
    void shouldRejectOrderWithEmptyQuantity() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(createUniqueSku(SKU));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("");
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "quantity", "Quantity must not be empty");
    }

    @Test
    void shouldRejectOrderWithNonIntegerQuantity() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(createUniqueSku(SKU));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("3.5");
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "quantity", "Quantity must be an integer");
    }

    @Test
    void shouldRejectOrderWithEmptyCountry() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(createUniqueSku(SKU));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill(QUANTITY);
        shopUiPage.locator("[aria-label=\"Country\"]").fill("");
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "country", "Country must not be empty");
    }

    @Test
    void shouldRejectOrderWithInvalidCountry() throws Exception {
        var sku = createUniqueSku(SKU);
        var createProductJson = """
                {
                    "id": "%s",
                    "title": "Test Product",
                    "description": "Test Description",
                    "category": "Test Category",
                    "brand": "Test Brand",
                    "price": "20.00"
                }
                """.formatted(sku);

        var createProductResponse = erpHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getErpBaseUrl() + "/api/products"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(createProductJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(createProductResponse.statusCode()).isEqualTo(201);

        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/shop']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(sku);
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill(QUANTITY);
        shopUiPage.locator("[aria-label=\"Country\"]").fill("XX");
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        assertErrorAlertContains("The request contains one or more validation errors", "country", "Country does not exist: XX");
    }

    private void assertErrorAlertContains(String... expected) {
        var errorAlert = shopUiPage.locator("[role='alert']");
        errorAlert.waitFor();
        assertThat(errorAlert.isVisible()).isTrue();
        var errorText = errorAlert.textContent();
        for (var text : expected) {
            assertThat(errorText).contains(text);
        }
    }
}

