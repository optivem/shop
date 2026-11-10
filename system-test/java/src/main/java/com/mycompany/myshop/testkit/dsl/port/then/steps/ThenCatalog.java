package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

/** The catalog the executed browse showed. SKUs are aliases; titles, brands, prices and labels are literals as shown. */
public interface ThenCatalog extends ThenStep<ThenCatalog> {
    /** Exactly these products, in this order. */
    ThenCatalog showsProducts(String... skuAliases);

    ThenCatalog showsNoProducts();

    ThenCatalog hasProductCount(int expectedCount);

    ThenCatalog showsProduct(String skuAlias, String expectedTitle, String expectedBrand, String expectedPrice);

    /** The category filter's options, "All" included, in order. */
    ThenCatalog offersCategories(String... expectedCategories);

    ThenCatalog hasCategorySelected(String expectedCategory);

    ThenCatalog offersBrands(String... expectedBrands);

    ThenCatalog hasNoBrandTicked();

    ThenCatalog hasSort(String expectedSort);

    ThenCatalog showsMessage(String expectedMessage);

    ThenCatalog offersToClearFilters();

    /** The rest of the site keeps working: the acting user can still open their order history. */
    ThenCatalog canOpenMyOrders();
}
