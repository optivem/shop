package com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages;

import com.optivem.eshop.dsl.driver.adapter.shared.client.playwright.PageClient;

public class HomePage extends BasePage {
    private static final String SHOP_BUTTON_SELECTOR = "a[href='/shop']";
    private static final String ORDER_HISTORY_BUTTON_SELECTOR = "a[href='/order-history']";
    public HomePage(PageClient pageClient) {
        super(pageClient);
    }

    public NewOrderPage clickNewOrder() {
        pageClient.click(SHOP_BUTTON_SELECTOR);
        return new NewOrderPage(pageClient);
    }

    public OrderHistoryPage clickOrderHistory() {
        pageClient.click(ORDER_HISTORY_BUTTON_SELECTOR);
        return new OrderHistoryPage(pageClient);
    }
}



