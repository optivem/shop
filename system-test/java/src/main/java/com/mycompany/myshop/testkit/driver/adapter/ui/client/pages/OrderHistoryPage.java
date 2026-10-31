package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;

import java.util.List;

public class OrderHistoryPage extends BasePage {
    private static final String ORDER_NUMBER_INPUT_SELECTOR = "[aria-label='Order Number']";
    private static final String SEARCH_BUTTON_SELECTOR = "[aria-label='Refresh Order List']";
    private static final String TABLE_ROW_SELECTOR = "table tbody tr:visible";
    private static final String ORDER_NUMBER_CELL_SELECTOR = "table tbody tr:visible:has(a) td:first-child";
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

    /** Reads the listed order numbers; the table holds a single linkless row when there are no orders. */
    public List<String> readOrderNumbers() {
        pageClient.readAllTextContents(TABLE_ROW_SELECTOR);
        return pageClient.readAllTextContentsNoWait(ORDER_NUMBER_CELL_SELECTOR).stream().map(String::trim).toList();
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


