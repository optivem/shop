package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;

import java.util.List;

/** The cart `/cart` (UC-008), read for its lines only. */
public class CartPage extends BasePage {
    private static final String CART_LINES_SELECTOR = "table[aria-label='Cart Lines']";
    private static final String EMPTY_CART_SELECTOR = "p.lead:has-text('Your cart is empty.')";
    public static final String READY_SELECTOR = CART_LINES_SELECTOR + ", " + EMPTY_CART_SELECTOR;
    private static final String CART_LINE_SELECTOR = CART_LINES_SELECTOR + " tr[aria-label^='Cart Line ']";
    private static final String CART_LINE_LABEL_PREFIX = "Cart Line ";
    private static final String QUANTITY_SELECTOR_TEMPLATE = "input[aria-label='Quantity for %s']";

    public CartPage(PageClient pageClient) {
        super(pageClient);
    }

    public List<ViewProductResponse.CartLineDto> readLines() {
        pageClient.waitForVisible(READY_SELECTOR);
        return pageClient.readAllAttributesNoWait(CART_LINE_SELECTOR, "aria-label").stream()
                .map(label -> label.substring(CART_LINE_LABEL_PREFIX.length()))
                .map(sku -> ViewProductResponse.CartLineDto.builder()
                        .sku(sku)
                        .quantity(Integer.parseInt(pageClient.readInputValue(String.format(QUANTITY_SELECTOR_TEMPLATE, sku)).trim()))
                        .build())
                .toList();
    }
}
