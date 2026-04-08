package com.optivem.shop.systemtest.legacy.mod03.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.optivem.shop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.optivem.shop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveUiTest extends BaseE2eTest {
    @Override
    protected void setShopClient() {
        setUpShopBrowser();
    }

    @Test
    void shouldPlaceOrderForValidInput() throws Exception {
        var sku = SKU + "-" + UUID.randomUUID().toString().substring(0, 8);
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

        var createProductUri = URI.create(getErpBaseUrl() + "/api/products");
        var createProductRequest = HttpRequest.newBuilder()
                .uri(createProductUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createProductJson))
                .build();

        var createProductResponse = erpHttpClient.send(createProductRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createProductResponse.statusCode()).isEqualTo(201);

        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/new-order']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(sku);
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("5");
        shopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        var successMessageText = shopUiPage.locator("[role='alert']").textContent();
        var pattern = Pattern.compile("Success! Order has been created with Order Number ([\\w-]+)");
        var matcher = pattern.matcher(successMessageText);
        assertThat(matcher.find()).isTrue();
        var orderNumber = matcher.group(1);
        assertThat(orderNumber).startsWith("ORD-");

        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/order-history']").click();
        shopUiPage.locator("[aria-label='Order Number']").fill(orderNumber);
        shopUiPage.locator("[aria-label='Refresh Order List']").click();

        var rowSelector = String.format("//tr[contains(., '%s')]", orderNumber);
        shopUiPage.locator(rowSelector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(shopUiPage.locator(rowSelector).isVisible()).isTrue();

        var viewDetailsSelector = String.format("%s//a[contains(text(), 'View Details')]", rowSelector);
        shopUiPage.locator(viewDetailsSelector).click();

        shopUiPage.locator("[aria-label='Display Order Number']")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(shopUiPage.locator("[aria-label='Display Order Number']").textContent()).isEqualTo(orderNumber);
        assertThat(shopUiPage.locator("[aria-label='Display SKU']").textContent()).isEqualTo(sku);
        assertThat(Integer.parseInt(shopUiPage.locator("[aria-label='Display Quantity']").textContent())).isEqualTo(5);

        var unitPriceText = shopUiPage.locator("[aria-label='Display Unit Price']").textContent().replace("$", "");
        assertThat(Double.parseDouble(unitPriceText)).isEqualTo(20.00);

        var totalPriceText = shopUiPage.locator("[aria-label='Display Total Price']").textContent().replace("$", "");
        assertThat(Double.parseDouble(totalPriceText)).isGreaterThan(0.0);

        assertThat(shopUiPage.locator("[aria-label='Display Status']").textContent()).isEqualTo("PLACED");
    }
}
