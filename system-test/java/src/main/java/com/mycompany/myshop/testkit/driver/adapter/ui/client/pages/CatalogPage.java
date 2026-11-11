package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;

import java.util.ArrayList;
import java.util.List;

/** The catalog on the home page `/` (UC-006): header search and category links, filters, sort and the product grid. */
public class CatalogPage extends BasePage {
    private static final String CATALOG_SELECTOR = "section[aria-label='Catalog']";
    private static final String PRODUCT_LIST_SELECTOR = CATALOG_SELECTOR + " [aria-label='Product List']";
    private static final String CLEAR_FILTERS_SELECTOR = CATALOG_SELECTOR + " [aria-label='Clear Filters']";
    private static final String UNAVAILABLE_SELECTOR = CATALOG_SELECTOR + " .alert-danger";
    private static final String NO_MATCH_SELECTOR = CATALOG_SELECTOR + " p.lead";
    /** Exactly one of these shows once the catalog has loaded: products, the no-match notice or the ERP outage. */
    public static final String READY_SELECTOR = CATALOG_SELECTOR
            + " :is([aria-label='Product List'], [aria-label='Clear Filters'], .alert-danger)";
    private static final String PRODUCT_CARD_SELECTOR = PRODUCT_LIST_SELECTOR + " article[aria-label^='Product ']";
    private static final String PRODUCT_CARD_LABEL_PREFIX = "Product ";
    private static final String PRODUCT_COUNT_SELECTOR = CATALOG_SELECTOR + " [aria-label='Product Count']";
    private static final String SORT_SELECTOR = CATALOG_SELECTOR + " select[aria-label='Sort']";
    private static final String CATEGORY_FILTER_SELECTOR = "fieldset[aria-label='Category Filter']";
    private static final String BRAND_FILTER_SELECTOR = "fieldset[aria-label='Brand Filter']";
    private static final String CHECKED_OPTION_SELECTOR = " .form-check:has(input:checked) label";
    private static final String SEARCH_INPUT_SELECTOR = "input[aria-label='Search Products']";
    private static final String SEARCH_BUTTON_SELECTOR = "button[aria-label='Search']";
    private static final String HEADER_CATEGORY_LINK_SELECTOR_TEMPLATE = "nav[aria-label='Store navigation'] a:text-is('%s')";
    private static final String OPTION_LABEL_SELECTOR_TEMPLATE = "%s label:text-is('%s')";
    private static final String SEARCH_PARAMETER = "q=";

    public CatalogPage(PageClient pageClient) {
        super(pageClient);
    }

    public void waitForLoaded() {
        pageClient.waitForVisible(READY_SELECTOR);
    }

    public void clickHeaderCategory(String category) {
        changeView(() -> pageClient.click(String.format(HEADER_CATEGORY_LINK_SELECTOR_TEMPLATE, category)));
    }

    /** An empty search on an unsearched catalog shows the same page address, so there is no change to wait for. */
    public void search(String term) {
        var sameAddress = term.isBlank() && !pageClient.currentUrl().contains(SEARCH_PARAMETER);
        pageClient.fill(SEARCH_INPUT_SELECTOR, term);
        if (sameAddress) {
            pageClient.click(SEARCH_BUTTON_SELECTOR);
            waitForLoaded();
            return;
        }
        changeView(() -> pageClient.click(SEARCH_BUTTON_SELECTOR));
    }

    public void selectCategory(String category) {
        if (category.equals(readSelectedCategory())) {
            return;
        }
        changeView(() -> pageClient.click(String.format(OPTION_LABEL_SELECTOR_TEMPLATE, CATEGORY_FILTER_SELECTOR, category)));
    }

    public void tickBrand(String brand) {
        if (readTickedBrands().contains(brand)) {
            return;
        }
        changeView(() -> pageClient.click(String.format(OPTION_LABEL_SELECTOR_TEMPLATE, BRAND_FILTER_SELECTOR, brand)));
    }

    public void sortBy(String sortLabel) {
        if (sortLabel.equals(pageClient.readSelectedOptionLabel(SORT_SELECTOR))) {
            return;
        }
        changeView(() -> pageClient.selectOptionByLabel(SORT_SELECTOR, sortLabel));
    }

    /** Opens a product from its card, by the title link. */
    public ProductPage clickProduct(String sku) {
        var previousUrl = pageClient.currentUrl();
        pageClient.click(PRODUCT_LIST_SELECTOR + " article[aria-label='" + PRODUCT_CARD_LABEL_PREFIX + sku + "'] a[aria-label='Product Title']");
        pageClient.waitForUrlChange(previousUrl);
        return new ProductPage(pageClient);
    }

    /** Adds a product with its card's "Add to cart" (quantity 1) and returns the confirmation or the error. */
    public String clickAddToCart(String sku) {
        pageClient.click(PRODUCT_LIST_SELECTOR + " button[aria-label='Add " + sku + " to Cart']");
        var result = getResult();
        var message = result.isSuccess() ? result.getValue() : result.getError().getMessage();
        waitForLoaded();
        return message == null ? null : message.trim();
    }

    public void reopenPageAddress() {
        pageClient.reopenCurrentUrl();
        waitForLoaded();
    }

    public void clickClearFilters() {
        changeView(() -> pageClient.click(CLEAR_FILTERS_SELECTOR));
    }

    public BrowseCatalogResponse read() {
        waitForLoaded();
        var skus = pageClient.readAllAttributesNoWait(PRODUCT_CARD_SELECTOR, "aria-label").stream()
                .map(label -> label.substring(PRODUCT_CARD_LABEL_PREFIX.length()))
                .toList();
        var titles = trimmed(pageClient.readAllTextContentsNoWait(PRODUCT_CARD_SELECTOR + " [aria-label='Product Title']"));
        var brands = trimmed(pageClient.readAllTextContentsNoWait(PRODUCT_CARD_SELECTOR + " [aria-label='Product Brand']"));
        var prices = trimmed(pageClient.readAllTextContentsNoWait(PRODUCT_CARD_SELECTOR + " [aria-label='Display Price']"));

        var products = new ArrayList<BrowseCatalogResponse.ProductDto>();
        for (var i = 0; i < skus.size(); i++) {
            products.add(BrowseCatalogResponse.ProductDto.builder()
                    .sku(skus.get(i))
                    .title(titles.get(i))
                    .brand(brands.get(i))
                    .price(prices.get(i))
                    .build());
        }

        return BrowseCatalogResponse.builder()
                .products(products)
                .productCount(readProductCount())
                .categories(trimmed(pageClient.readAllTextContentsNoWait(CATEGORY_FILTER_SELECTOR + " label")))
                .selectedCategory(readSelectedCategory())
                .brands(trimmed(pageClient.readAllTextContentsNoWait(BRAND_FILTER_SELECTOR + " label")))
                .tickedBrands(readTickedBrands())
                .sort(pageClient.readSelectedOptionLabel(SORT_SELECTOR))
                .message(readMessage())
                .clearFiltersOffered(!pageClient.isHidden(CLEAR_FILTERS_SELECTOR))
                .build();
    }

    /** Runs an action that changes the view, then waits for the new page address and the re-rendered catalog. */
    private void changeView(Runnable action) {
        var previousUrl = pageClient.currentUrl();
        action.run();
        pageClient.waitForUrlChange(previousUrl);
        waitForLoaded();
    }

    private Integer readProductCount() {
        var text = pageClient.readTextContent(PRODUCT_COUNT_SELECTOR).trim();
        return Integer.parseInt(text.split("\\s+")[0]);
    }

    private String readSelectedCategory() {
        var selected = trimmed(pageClient.readAllTextContentsNoWait(CATEGORY_FILTER_SELECTOR + CHECKED_OPTION_SELECTOR));
        return selected.isEmpty() ? null : selected.get(0);
    }

    private List<String> readTickedBrands() {
        return trimmed(pageClient.readAllTextContentsNoWait(BRAND_FILTER_SELECTOR + CHECKED_OPTION_SELECTOR));
    }

    private String readMessage() {
        if (!pageClient.isHidden(UNAVAILABLE_SELECTOR)) {
            return pageClient.readTextContent(UNAVAILABLE_SELECTOR).trim();
        }
        if (!pageClient.isHidden(NO_MATCH_SELECTOR)) {
            return pageClient.readTextContent(NO_MATCH_SELECTOR).trim();
        }
        return null;
    }

    private static List<String> trimmed(List<String> values) {
        return values.stream().map(String::trim).toList();
    }
}
