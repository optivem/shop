package com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages;

import com.optivem.eshop.dsl.driver.adapter.shared.client.playwright.PageClient;

import java.util.regex.Pattern;

public class NewOrderPage extends BasePage {
    private static final String SKU_INPUT_SELECTOR = "[aria-label=\"SKU\"]";
    private static final String QUANTITY_INPUT_SELECTOR = "[aria-label=\"Quantity\"]";
    private static final String PLACE_ORDER_BUTTONG_SELECTOR = "[aria-label=\"Place Order\"]";
    private static final String ORDER_NUMBER_REGEX = "Success! Order has been created with Order Number ([\\w-]+)";
    private static final int ORDER_NUMBER_MATCHER_GROUP = 1;
    private static final String ORDER_NUMBER_NOT_FOUND_ERROR = "Could not find order number";

    public NewOrderPage(PageClient pageClient) {
        super(pageClient);
    }

    public void inputSku(String sku) {
        pageClient.fill(SKU_INPUT_SELECTOR, sku);
    }

    public void inputQuantity(String quantity) {
        pageClient.fill(QUANTITY_INPUT_SELECTOR, quantity);
    }

    public void clickPlaceOrder() {
        pageClient.click(PLACE_ORDER_BUTTONG_SELECTOR);
    }

    public static String getOrderNumber(String successMessageText) {

        var pattern = Pattern.compile(ORDER_NUMBER_REGEX);
        var matcher = pattern.matcher(successMessageText);

        if (!matcher.find()) {
            throw new IllegalStateException(ORDER_NUMBER_NOT_FOUND_ERROR);
        }

        return matcher.group(ORDER_NUMBER_MATCHER_GROUP);
    }
}



