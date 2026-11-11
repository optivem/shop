package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;

import java.util.ArrayList;

/** The order confirmation `/checkout/confirmation` (UC-009): the orders just placed and the rejected lines. */
public class CheckoutConfirmationPage extends BasePage {
    private static final String CONFIRMED_ORDERS_SELECTOR = "ul[aria-label='Confirmed Orders']";
    private static final String REJECTED_LINES_SELECTOR = "[aria-label='Rejected Lines']";
    private static final String NO_RECENT_ORDER_SELECTOR = "h1:has-text('No recent order')";
    /** Exactly one of the confirmed orders or "No recent order" shows once the confirmation has loaded. */
    public static final String READY_SELECTOR = CONFIRMED_ORDERS_SELECTOR + ", " + NO_RECENT_ORDER_SELECTOR;
    private static final String CONFIRMED_ORDER_SELECTOR = CONFIRMED_ORDERS_SELECTOR + " a[aria-label^='Confirmed Order ']";
    private static final String REJECTED_LINE_SELECTOR = REJECTED_LINES_SELECTOR + " li[aria-label^='Rejected Line ']";
    private static final String REJECTED_LINE_LABEL_PREFIX = "Rejected Line ";
    private static final String REJECTION_REASON_SELECTOR_TEMPLATE = "[aria-label='Rejection Reason %s']";
    private static final String THANKS_SELECTOR = "h1:has-text('Thank you')";
    private static final String VIEW_MY_ORDERS_SELECTOR = "a:text-is('View my orders')";
    private static final String CONTINUE_SHOPPING_SELECTOR = "a:text-is('Continue shopping')";

    public CheckoutConfirmationPage(PageClient pageClient) {
        super(pageClient);
    }

    public boolean isShown() {
        return !pageClient.isHidden(READY_SELECTOR);
    }

    public void readInto(CheckOutResponse response) {
        pageClient.waitForVisible(READY_SELECTOR);
        response.setThanks(pageClient.isHidden(THANKS_SELECTOR) ? null : pageClient.readTextContent(THANKS_SELECTOR).trim());
        var numbers = pageClient.readAllTextContentsNoWait(CONFIRMED_ORDER_SELECTOR);
        var links = pageClient.readAllAttributesNoWait(CONFIRMED_ORDER_SELECTOR, "href");
        var orders = new ArrayList<CheckOutResponse.ConfirmedOrderDto>();
        for (var i = 0; i < numbers.size(); i++) {
            orders.add(CheckOutResponse.ConfirmedOrderDto.builder()
                    .orderNumber(numbers.get(i).trim())
                    .link(links.get(i))
                    .build());
        }
        response.setConfirmedOrders(orders);
        response.setRejectedLines(pageClient.readAllAttributesNoWait(REJECTED_LINE_SELECTOR, "aria-label").stream()
                .map(label -> label.substring(REJECTED_LINE_LABEL_PREFIX.length()))
                .map(sku -> CheckOutResponse.RejectedLineDto.builder()
                        .sku(sku)
                        .reason(pageClient.readTextContent(String.format(REJECTION_REASON_SELECTOR_TEMPLATE, sku)).trim())
                        .build())
                .toList());
        response.setViewMyOrdersOffered(!pageClient.isHidden(VIEW_MY_ORDERS_SELECTOR));
        response.setContinueShoppingOffered(!pageClient.isHidden(CONTINUE_SHOPPING_SELECTOR));
    }
}
