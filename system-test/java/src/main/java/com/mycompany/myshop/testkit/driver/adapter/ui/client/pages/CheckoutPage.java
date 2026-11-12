package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ShippingAddress;

import java.util.List;

/**
 * The checkout `/checkout` (UC-009): shipping address, optional coupon and the order review. The server-rendered UIs
 * post a form and render the checkout again, with an error, when nothing was placed; the single-page UIs stay in
 * place. The address errors are plain messages ("City is required") under the notification's message, and a rejected
 * line is "SKU: reason", so they are read as they are shown rather than parsed as {@code field: message}.
 */
public class CheckoutPage extends BasePage {
    private static final String PLACE_ORDER_SELECTOR = "button[aria-label='Place Checkout Order']";
    /** The checkout has loaded (the single-page UIs load the profile first). */
    public static final String READY_SELECTOR = PLACE_ORDER_SELECTOR;
    private static final String REVIEW_LINE_SELECTOR = "[aria-label='Order Review'] li[aria-label^='Review Line ']";
    private static final String REVIEW_LINE_LABEL_PREFIX = "Review Line ";
    private static final String REVIEW_QUANTITY_SELECTOR_TEMPLATE = "[aria-label='Review Quantity %s']";
    private static final String REVIEW_LINE_TOTAL_SELECTOR_TEMPLATE = "[aria-label='Review Line Total %s']";
    private static final String REVIEW_SUBTOTAL_SELECTOR = "[aria-label='Review Subtotal']";
    private static final String TAX_COUNTRY_SELECTOR = "[aria-label='Tax Country']";
    private static final String ESTIMATED_TAX_RATE_SELECTOR = "[aria-label='Estimated Tax Rate']";
    private static final String REVIEW_NOTE_SELECTOR = "p:has-text('Discount and tax are worked out per order')";
    private static final String USE_SAVED_ADDRESS_SELECTOR = "input[aria-label='Use Saved Address']";
    private static final String SAVED_ADDRESS_SELECTOR = "[aria-label='Saved Address']";
    private static final String SHIP_TO_DIFFERENT_ADDRESS_SELECTOR = "input[aria-label='Ship to a Different Address']";
    private static final String FIRST_NAME_SELECTOR = "input[aria-label='Shipping First Name']";
    private static final String LAST_NAME_SELECTOR = "input[aria-label='Shipping Last Name']";
    private static final String STREET_SELECTOR = "input[aria-label='Shipping Street']";
    private static final String POSTAL_CODE_SELECTOR = "input[aria-label='Shipping Postal Code']";
    private static final String CITY_SELECTOR = "input[aria-label='Shipping City']";
    private static final String COUNTRY_SELECTOR = "select[aria-label='Shipping Country']";
    /** "Save as my default shipping address", offered on a first order only. */
    private static final String SAVE_AS_DEFAULT_SELECTOR = "input[aria-label='Save as Default Address']";
    private static final String COUPON_CODE_SELECTOR = "input[aria-label='Checkout Coupon Code']";
    private static final String ERROR_SELECTOR = "[role='alert'].notification.error";
    private static final String ERROR_MESSAGE_SELECTOR = ERROR_SELECTOR + " .error-message";
    private static final String ERROR_DETAIL_SELECTOR = ERROR_SELECTOR + " .field-error";

    public CheckoutPage(PageClient pageClient) {
        super(pageClient);
    }

    public void waitForLoaded() {
        pageClient.waitForVisible(READY_SELECTOR);
    }

    /** Chooses "Ship to a different address" (offered only with a saved address) and enters this one. */
    public void inputShippingAddress(ShippingAddress address) {
        if (!pageClient.isHidden(SHIP_TO_DIFFERENT_ADDRESS_SELECTOR)) {
            pageClient.click(SHIP_TO_DIFFERENT_ADDRESS_SELECTOR);
        }
        pageClient.fill(FIRST_NAME_SELECTOR, address.getFirstName());
        pageClient.fill(LAST_NAME_SELECTOR, address.getLastName());
        pageClient.fill(STREET_SELECTOR, address.getStreet());
        pageClient.fill(POSTAL_CODE_SELECTOR, address.getPostalCode());
        pageClient.fill(CITY_SELECTOR, address.getCity());
        pageClient.selectOptionByValueAddingIfMissing(COUNTRY_SELECTOR, address.getCountry() == null ? "" : address.getCountry());
    }

    /** Ticks or unticks "Save as my default shipping address" (offered on a first order only). */
    public void setSaveAsDefault(boolean saveAsDefault) {
        pageClient.setChecked(SAVE_AS_DEFAULT_SELECTOR, saveAsDefault);
    }

    public void inputCouponCode(String couponCode) {
        pageClient.fill(COUPON_CODE_SELECTOR, couponCode);
    }

    /**
     * Places the order and waits for its outcome: the confirmation, or the checkout with an error. The checkout shows
     * no error before the order is placed, so either one is the outcome.
     */
    public void clickPlaceOrder() {
        pageClient.click(PLACE_ORDER_SELECTOR);
        pageClient.waitForVisible(CheckoutConfirmationPage.READY_SELECTOR + ", " + ERROR_SELECTOR);
    }

    public boolean isShown() {
        return !pageClient.isHidden(READY_SELECTOR);
    }

    public String readTaxCountry() {
        return readText(TAX_COUNTRY_SELECTOR);
    }

    public String readEstimatedTaxRate() {
        return readText(ESTIMATED_TAX_RATE_SELECTOR);
    }

    /** The checkout as shown now. */
    public void readInto(CheckOutResponse response) {
        waitForLoaded();
        var lines = pageClient.readAllAttributesNoWait(REVIEW_LINE_SELECTOR, "aria-label").stream()
                .map(label -> label.substring(REVIEW_LINE_LABEL_PREFIX.length()))
                .map(sku -> CheckOutResponse.ReviewLineDto.builder()
                        .sku(sku)
                        .quantity(Integer.parseInt(readText(String.format(REVIEW_QUANTITY_SELECTOR_TEMPLATE, sku))))
                        .lineTotal(readText(String.format(REVIEW_LINE_TOTAL_SELECTOR_TEMPLATE, sku)))
                        .build())
                .toList();
        response.setReviewLines(lines);
        response.setReviewSubtotal(readText(REVIEW_SUBTOTAL_SELECTOR));
        response.setTaxCountry(readTaxCountry());
        response.setEstimatedTaxRate(readEstimatedTaxRate());
        response.setReviewNote(pageClient.isHidden(REVIEW_NOTE_SELECTOR) ? null : readText(REVIEW_NOTE_SELECTOR));
        var hasSaved = !pageClient.isHidden(USE_SAVED_ADDRESS_SELECTOR);
        response.setSavedAddressSelected(hasSaved && pageClient.isChecked(USE_SAVED_ADDRESS_SELECTOR));
        response.setSavedAddress(hasSaved ? readText(SAVED_ADDRESS_SELECTOR) : null);
        var formOpen = !pageClient.isHidden(FIRST_NAME_SELECTOR);
        response.setAddressFormOpen(formOpen);
        response.setShippingFirstName(formOpen ? pageClient.readInputValue(FIRST_NAME_SELECTOR) : null);
        response.setShippingLastName(formOpen ? pageClient.readInputValue(LAST_NAME_SELECTOR) : null);
        response.setSaveAsDefaultTicked(!pageClient.isHidden(SAVE_AS_DEFAULT_SELECTOR) && pageClient.isChecked(SAVE_AS_DEFAULT_SELECTOR));
        if (pageClient.isHidden(ERROR_SELECTOR)) {
            response.setMessageDetails(List.of());
        } else {
            response.setMessage(readText(ERROR_MESSAGE_SELECTOR));
            response.setMessageDetails(pageClient.readAllTextContentsNoWait(ERROR_DETAIL_SELECTOR).stream()
                    .map(String::trim)
                    .toList());
        }
    }

    private String readText(String selector) {
        return pageClient.readTextContent(selector).trim();
    }
}
