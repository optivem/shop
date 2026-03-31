package com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages;

import com.optivem.eshop.dsl.driver.adapter.shared.client.playwright.PageClient;

public class OrderHistoryPage extends BasePage {
    private static final String ORDER_NUMBER_INPUT_SELECTOR = "[aria-label='Order Number']";
    private static final String SEARCH_BUTTON_SELECTOR = "[aria-label='Refresh Order List']";
    private static final String ROW_SELECTOR_TEMPLATE = "//tr[contains(., '%s')]";
    private static final String VIEW_DETAILS_LINK_SELECTOR_TEMPLATE = "%s//a[contains(text(), 'View Details')]";

    public OrderHistoryPage(PageClient pageClient) {
        super(pageClient);
    }

    public void inputOrderNumber(String orderNumber) {
        pageClient.fill(ORDER_NUMBER_INPUT_SELECTOR, orderNumber);
    }

    public void clickSearch() {
        pageClient.click(SEARCH_BUTTON_SELECTOR);
    }

    public boolean isOrderListed(String orderNumber) {
        var rowSelector = getRowSelector(orderNumber);
        return pageClient.isVisible(rowSelector);
    }

    public OrderDetailsPage clickViewOrderDetails(String orderNumber) {
        var rowSelector = getRowSelector(orderNumber);
        var viewDetailsLinkSelector = String.format(VIEW_DETAILS_LINK_SELECTOR_TEMPLATE, rowSelector);
        pageClient.click(viewDetailsLinkSelector);
        return new OrderDetailsPage(pageClient);
    }

    private static String getRowSelector(String orderNumber) {
        return String.format(ROW_SELECTOR_TEMPLATE, orderNumber);
    }
}


