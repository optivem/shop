package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCatalogVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCatalog;

public class ThenCatalogImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenCatalog {
    private final BrowseCatalogVerification verification;

    public ThenCatalogImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof BrowseCatalogVerification catalogVerification)) {
            throw new IllegalStateException("Cannot verify the catalog: the executed operation did not browse the catalog");
        }
        this.verification = catalogVerification;
    }

    @Override
    public ThenCatalogImpl<R, V> showsProducts(String... skuAliases) {
        verification.showsProducts(skuAliases);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> showsNoProducts() {
        verification.showsNoProducts();
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> hasProductCount(int expectedCount) {
        verification.productCount(expectedCount);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> showsProduct(String skuAlias, String expectedTitle, String expectedBrand, String expectedPrice) {
        verification.product(skuAlias, expectedTitle, expectedBrand, expectedPrice);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> offersCategories(String... expectedCategories) {
        verification.categoryOptions(expectedCategories);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> hasCategorySelected(String expectedCategory) {
        verification.selectedCategory(expectedCategory);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> offersBrands(String... expectedBrands) {
        verification.brandOptions(expectedBrands);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> hasNoBrandTicked() {
        verification.noBrandTicked();
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> hasSort(String expectedSort) {
        verification.sort(expectedSort);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> showsMessage(String expectedMessage) {
        verification.message(expectedMessage);
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> offersToClearFilters() {
        verification.clearFiltersOffered();
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> canOpenMyOrders() {
        app.myShop(ChannelMode.DYNAMIC).browseOrderHistory().execute().shouldSucceed();
        return this;
    }

    @Override
    public ThenCatalogImpl<R, V> and() {
        return this;
    }
}
