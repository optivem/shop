package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ViewProductVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProductPage;

import java.util.Map;

public class ThenProductPageImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenProductPage {
    private final ViewProductVerification verification;

    public ThenProductPageImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof ViewProductVerification productVerification)) {
            throw new IllegalStateException("Cannot verify the product page: the executed operation did not view a product");
        }
        this.verification = productVerification;
    }

    @Override
    public ThenProductPageImpl<R, V> hasTitle(String expectedTitle) {
        verification.title(expectedTitle);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasBrand(String expectedBrand) {
        verification.brand(expectedBrand);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasCategory(String expectedCategory) {
        verification.category(expectedCategory);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasSku(String expectedSkuAlias) {
        verification.sku(expectedSkuAlias);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasPrice(String expectedPrice) {
        verification.price(expectedPrice);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasDescription(String expectedDescription) {
        verification.description(expectedDescription);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasBreadcrumbs(String... expectedBreadcrumbs) {
        verification.breadcrumbs(expectedBreadcrumbs);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasQuantity(int expectedQuantity) {
        verification.quantity(expectedQuantity);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> showsMessage(String expectedMessage) {
        verification.message(expectedMessage);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> offersBackToCatalog() {
        verification.backToCatalogOffered();
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> doesNotOfferAddToCart() {
        verification.addToCartNotOffered();
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> hasCartBadge(int expectedCount) {
        verification.cartItemCount(expectedCount);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> isOnPageOfProduct(String skuAlias) {
        verification.onPageOfProduct(skuAlias);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> cartContains(String skuAlias, int expectedQuantity) {
        verification.cartContains(Map.of(skuAlias, expectedQuantity));
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> cartIsEmpty() {
        verification.cartIsEmpty();
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> catalogShowsProducts(String... skuAliases) {
        verification.catalogShowsProducts(skuAliases);
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> canOpenMyOrders() {
        app.myShop(ChannelMode.DYNAMIC).browseOrderHistory().execute().shouldSucceed();
        return this;
    }

    @Override
    public ThenProductPageImpl<R, V> and() {
        return this;
    }
}
