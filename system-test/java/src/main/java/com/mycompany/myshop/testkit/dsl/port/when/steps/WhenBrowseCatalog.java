package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

/**
 * Browses the catalog as a visitor (or as the logged-in user). The steps run in this order: follow a header
 * category link, search, pick a category, tick brands, sort; then reopen the page address or clear the filters.
 */
public interface WhenBrowseCatalog extends WhenStep {
    WhenBrowseCatalog withHeaderCategoryLink(String category);

    WhenBrowseCatalog withSearch(String term);

    WhenBrowseCatalog withCategory(String category);

    WhenBrowseCatalog withBrands(String... brands);

    /** The sort order's label, e.g. "Price: high to low". */
    WhenBrowseCatalog withSort(String sort);

    /** After the other steps, opens the same page address again. */
    WhenBrowseCatalog withPageAddressReopened();

    /** After the other steps, clears the filters. */
    WhenBrowseCatalog withFiltersCleared();
}
