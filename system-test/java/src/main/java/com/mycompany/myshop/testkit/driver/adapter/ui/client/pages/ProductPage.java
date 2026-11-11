package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** A product's page `/products/{sku}` (UC-007): its details, the quantity stepper and "Add to cart". */
public class ProductPage extends BasePage {
    private static final String TITLE_SELECTOR = "h1[aria-label='Product Title']";
    private static final String BACK_TO_CATALOG_SELECTOR = "[aria-label='Back to Catalog']";
    private static final String NOT_FOUND_HEADING_SELECTOR = ":has(> " + BACK_TO_CATALOG_SELECTOR + ") > h1";
    private static final String UNAVAILABLE_SELECTOR = "main .alert-danger";
    /** Exactly one of these shows once the product page has loaded: the product, product not found or the ERP outage. */
    public static final String READY_SELECTOR = ":is(" + TITLE_SELECTOR + ", " + BACK_TO_CATALOG_SELECTOR + ", " + UNAVAILABLE_SELECTOR + ")";
    private static final String BRAND_SELECTOR = "[aria-label='Product Brand']";
    private static final String CATEGORY_SELECTOR = "[aria-label='Product Category']";
    private static final String SKU_SELECTOR = "[aria-label='Product SKU']";
    private static final String PRICE_SELECTOR = "[aria-label='Display Price']";
    private static final String DESCRIPTION_SELECTOR = "[aria-label='Product Description']";
    private static final String BREADCRUMB_SELECTOR = "nav[aria-label='breadcrumb'] .breadcrumb-item";
    private static final String QUANTITY_SELECTOR = "input[aria-label='Product Quantity']";
    private static final String INCREASE_SELECTOR = "button[aria-label='Increase Quantity']";
    private static final String DECREASE_SELECTOR = "button[aria-label='Decrease Quantity']";
    private static final String ADD_TO_CART_SELECTOR = "button[aria-label='Add to Cart']";
    private static final String CART_ITEM_COUNT_SELECTOR = "[aria-label='Cart Item Count']";
    private static final String PRODUCT_PATH_PREFIX = "/products/";

    private String notificationMessage;

    public ProductPage(PageClient pageClient) {
        super(pageClient);
    }

    public void waitForLoaded() {
        pageClient.waitForVisible(READY_SELECTOR);
    }

    public void inputQuantity(String quantity) {
        pageClient.fill(QUANTITY_SELECTOR, quantity);
    }

    public void clickIncreaseQuantity() {
        pageClient.click(INCREASE_SELECTOR);
    }

    public void clickDecreaseQuantity() {
        pageClient.click(DECREASE_SELECTOR);
    }

    /** Adds to the cart and waits for the confirmation or the error, which {@link #read()} reports as the message. */
    public void clickAddToCart() {
        pageClient.click(ADD_TO_CART_SELECTOR);
        var result = getResult();
        var message = result.isSuccess() ? result.getValue() : result.getError().getMessage();
        notificationMessage = message == null ? null : message.trim();
        waitForLoaded();
    }

    public CatalogPage clickBackToCatalog() {
        var previousUrl = pageClient.currentUrl();
        pageClient.click(BACK_TO_CATALOG_SELECTOR);
        pageClient.waitForUrlChange(previousUrl);
        var catalogPage = new CatalogPage(pageClient);
        catalogPage.waitForLoaded();
        return catalogPage;
    }

    public ViewProductResponse read() {
        waitForLoaded();
        var builder = ViewProductResponse.builder()
                .message(readMessage())
                .backToCatalogOffered(!pageClient.isHidden(BACK_TO_CATALOG_SELECTOR))
                .addToCartOffered(!pageClient.isHidden(ADD_TO_CART_SELECTOR))
                .cartItemCount(readCartItemCount())
                .pageSku(readPageSku());
        if (pageClient.isHidden(TITLE_SELECTOR)) {
            return builder.breadcrumbs(List.of()).build();
        }
        return builder
                .title(readText(TITLE_SELECTOR))
                .brand(readText(BRAND_SELECTOR))
                .category(readText(CATEGORY_SELECTOR))
                .sku(readText(SKU_SELECTOR))
                .price(readText(PRICE_SELECTOR))
                .description(readText(DESCRIPTION_SELECTOR))
                .breadcrumbs(pageClient.readAllTextContentsNoWait(BREADCRUMB_SELECTOR).stream().map(String::trim).toList())
                .quantity(pageClient.readInputValue(QUANTITY_SELECTOR))
                .build();
    }

    private String readMessage() {
        if (notificationMessage != null) {
            return notificationMessage;
        }
        if (!pageClient.isHidden(NOT_FOUND_HEADING_SELECTOR)) {
            return readText(NOT_FOUND_HEADING_SELECTOR);
        }
        if (!pageClient.isHidden(UNAVAILABLE_SELECTOR)) {
            return readText(UNAVAILABLE_SELECTOR);
        }
        return null;
    }

    private Integer readCartItemCount() {
        if (pageClient.isHidden(CART_ITEM_COUNT_SELECTOR)) {
            return 0;
        }
        return Integer.parseInt(readText(CART_ITEM_COUNT_SELECTOR));
    }

    private String readPageSku() {
        var path = URI.create(pageClient.currentUrl()).getRawPath();
        if (!path.startsWith(PRODUCT_PATH_PREFIX)) {
            return null;
        }
        return URLDecoder.decode(path.substring(PRODUCT_PATH_PREFIX.length()), StandardCharsets.UTF_8);
    }

    private String readText(String selector) {
        return pageClient.readTextContent(selector).trim();
    }
}
