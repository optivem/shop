package com.mycompany.myshop.systemtest.legacy.mod03.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.mycompany.myshop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.mycompany.myshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveUiTest extends BaseE2eTest {
    @Override
    protected void setMyShopClient() {
        setUpMyShopBrowser();
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

        myShopUiPage.navigate(getMyShopUiBaseUrl());
        loginToMyShopUiIfRequired();
        myShopUiPage.locator("a[href='/new-order']").click();

        myShopUiPage.locator("[aria-label=\"SKU\"]").fill(sku);
        myShopUiPage.locator("[aria-label=\"Quantity\"]").fill("5");
        myShopUiPage.locator("[aria-label=\"Country\"]").fill(COUNTRY);
        myShopUiPage.locator("[aria-label=\"Place Order\"]").click();

        var successMessageText = myShopUiPage.locator("[role='alert'][data-notification-id]").textContent();
        var pattern = Pattern.compile("Success! Order has been created with Order Number ([\\w-]+)");
        var matcher = pattern.matcher(successMessageText);
        assertThat(matcher.find()).isTrue();
        var orderNumber = matcher.group(1);
        assertThat(orderNumber).startsWith("ORD-");

        myShopUiPage.navigate(getMyShopUiBaseUrl());
        myShopUiPage.locator("a[href='/order-history']").click();
        myShopUiPage.locator("[aria-label='Order Number']").fill(orderNumber);
        myShopUiPage.locator("[aria-label='Refresh Order List']").click();

        var rowSelector = String.format("//tr[contains(., '%s')]", orderNumber);
        myShopUiPage.locator(rowSelector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(myShopUiPage.locator(rowSelector).isVisible()).isTrue();

        var viewDetailsSelector = String.format("%s//a[contains(text(), 'View Details')]", rowSelector);
        myShopUiPage.locator(viewDetailsSelector).click();

        myShopUiPage.locator("[aria-label='Display Order Number']")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertThat(myShopUiPage.locator("[aria-label='Display Order Number']").textContent()).isEqualTo(orderNumber);
        assertThat(myShopUiPage.locator("[aria-label='Display SKU']").textContent()).isEqualTo(sku);
        assertThat(Integer.parseInt(myShopUiPage.locator("[aria-label='Display Quantity']").textContent())).isEqualTo(5);

        var unitPriceText = myShopUiPage.locator("[aria-label='Display Unit Price']").textContent().replace("$", "");
        assertThat(Double.parseDouble(unitPriceText)).isEqualTo(20.00);

        var basePriceText = myShopUiPage.locator("[aria-label='Display Base Price']").textContent().replace("$", "");
        assertThat(Double.parseDouble(basePriceText)).isEqualTo(100.00);

        var totalPriceText = myShopUiPage.locator("[aria-label='Display Total Price']").textContent().replace("$", "");
        assertThat(Double.parseDouble(totalPriceText)).isGreaterThan(0.0);

        assertThat(myShopUiPage.locator("[aria-label='Display Status']").textContent()).isEqualTo("PLACED");
    }
}
