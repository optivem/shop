package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;

import java.util.List;

/**
 * The cart `/cart` (UC-008): its lines, quantity changes, removal and the subtotal. The server-rendered UIs post a
 * form and load the cart again for every change; the single-page UIs change it in place.
 */
public class CartPage extends BasePage {
    private static final String CART_LINES_SELECTOR = "table[aria-label='Cart Lines']";
    private static final String EMPTY_CART_SELECTOR = "p.lead:has-text('Your cart is empty.')";
    private static final String UNAVAILABLE_SELECTOR = "main .alert-danger";
    /** Exactly one of these shows once the cart has loaded: its lines, the empty cart or the ERP outage. */
    public static final String READY_SELECTOR = CART_LINES_SELECTOR + ", " + EMPTY_CART_SELECTOR + ", " + UNAVAILABLE_SELECTOR;
    private static final String CART_LINE_SELECTOR = CART_LINES_SELECTOR + " tr[aria-label^='Cart Line ']";
    private static final String CART_LINE_LABEL_PREFIX = "Cart Line ";
    private static final String QUANTITY_SELECTOR_TEMPLATE = "input[aria-label='Quantity for %s']";
    private static final String TITLE_SELECTOR_TEMPLATE = "[aria-label='Title %s']";
    private static final String UNIT_PRICE_SELECTOR_TEMPLATE = "[aria-label='Unit Price %s']";
    private static final String LINE_TOTAL_SELECTOR_TEMPLATE = "[aria-label='Line Total %s']";
    private static final String INCREASE_SELECTOR_TEMPLATE = "button[aria-label='Increase Quantity for %s']";
    private static final String DECREASE_SELECTOR_TEMPLATE = "button[aria-label='Decrease Quantity for %s']";
    private static final String REMOVE_SELECTOR_TEMPLATE = "button[aria-label='Remove %s']";
    private static final String LINE_COUNT_SELECTOR = "[aria-label='Cart Line Count']";
    private static final String SUBTOTAL_SELECTOR = "[aria-label='Cart Subtotal']";
    private static final String CHECKOUT_NOTE_SELECTOR = "p:has-text('Tax and discounts are calculated at checkout')";
    private static final String START_SHOPPING_SELECTOR = "a:text-is('Start shopping')";
    /** A typed quantity is committed when the field loses focus. */
    private static final String COMMIT_KEY = "Tab";

    public CartPage(PageClient pageClient) {
        super(pageClient);
    }

    public void waitForLoaded() {
        pageClient.waitForVisible(READY_SELECTOR);
    }

    public void inputQuantity(String sku, String quantity) {
        var selector = String.format(QUANTITY_SELECTOR_TEMPLATE, sku);
        pageClient.fill(selector, quantity);
        change(selector, () -> pageClient.press(selector, COMMIT_KEY));
    }

    public void clickIncreaseQuantity(String sku) {
        click(String.format(INCREASE_SELECTOR_TEMPLATE, sku));
    }

    public void clickDecreaseQuantity(String sku) {
        click(String.format(DECREASE_SELECTOR_TEMPLATE, sku));
    }

    public void clickRemove(String sku) {
        click(String.format(REMOVE_SELECTOR_TEMPLATE, sku));
    }

    public List<ViewProductResponse.CartLineDto> readLines() {
        waitForLoaded();
        return readCartLineSkus().stream()
                .map(sku -> ViewProductResponse.CartLineDto.builder()
                        .sku(sku)
                        .quantity(readQuantity(sku))
                        .build())
                .toList();
    }

    /** The cart as shown; {@code message} is the action's notification, if it had one. */
    public ManageCartResponse read(String message, Integer cartItemCount) {
        waitForLoaded();
        var lines = readCartLineSkus().stream()
                .map(sku -> ManageCartResponse.CartLineDto.builder()
                        .sku(sku)
                        .title(readText(String.format(TITLE_SELECTOR_TEMPLATE, sku)))
                        .unitPrice(readText(String.format(UNIT_PRICE_SELECTOR_TEMPLATE, sku)))
                        .quantity(readQuantity(sku))
                        .lineTotal(readText(String.format(LINE_TOTAL_SELECTOR_TEMPLATE, sku)))
                        .build())
                .toList();
        return ManageCartResponse.builder()
                .message(message != null ? message : readMessage())
                .cartItemCount(cartItemCount != null ? cartItemCount : readCartItemCount())
                .lines(lines)
                .itemCount(readItemCount())
                .subtotal(pageClient.isHidden(SUBTOTAL_SELECTOR) ? null : readText(SUBTOTAL_SELECTOR))
                .checkoutNote(pageClient.isHidden(CHECKOUT_NOTE_SELECTOR) ? null : readText(CHECKOUT_NOTE_SELECTOR))
                .startShoppingOffered(!pageClient.isHidden(START_SHOPPING_SELECTOR))
                .build();
    }

    /** The notification an action on the cart showed, if any. */
    public String readNotification() {
        return readShownNotification();
    }

    private void click(String selector) {
        change(selector, () -> pageClient.click(selector));
    }

    /** Runs a change and waits for the cart to show it: a new document when the control posts a form. */
    private void change(String selector, Runnable action) {
        if (pageClient.belongsToForm(selector)) {
            pageClient.runAndWaitForNewDocument(action);
        } else {
            action.run();
        }
        waitForLoaded();
    }

    private List<String> readCartLineSkus() {
        return pageClient.readAllAttributesNoWait(CART_LINE_SELECTOR, "aria-label").stream()
                .map(label -> label.substring(CART_LINE_LABEL_PREFIX.length()))
                .toList();
    }

    private Integer readQuantity(String sku) {
        return Integer.parseInt(pageClient.readInputValue(String.format(QUANTITY_SELECTOR_TEMPLATE, sku)).trim());
    }

    /** "(2 items)" next to the heading. */
    private Integer readItemCount() {
        if (pageClient.isHidden(LINE_COUNT_SELECTOR)) {
            return null;
        }
        return Integer.parseInt(readText(LINE_COUNT_SELECTOR).replaceAll("\\D+", " ").trim().split(" ")[0]);
    }

    private String readMessage() {
        var notification = readShownNotification();
        if (notification != null) {
            return notification;
        }
        if (!pageClient.isHidden(EMPTY_CART_SELECTOR)) {
            return readText(EMPTY_CART_SELECTOR);
        }
        if (!pageClient.isHidden(UNAVAILABLE_SELECTOR)) {
            return readText(UNAVAILABLE_SELECTOR);
        }
        return null;
    }

    private String readText(String selector) {
        return pageClient.readTextContent(selector).trim();
    }
}
