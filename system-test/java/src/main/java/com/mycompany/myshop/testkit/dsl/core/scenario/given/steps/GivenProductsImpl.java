package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenProducts;

import java.util.ArrayList;
import java.util.List;

public class GivenProductsImpl extends BaseGivenStep implements GivenProducts {
    private final List<ProductEntry> products = new ArrayList<>();

    public GivenProductsImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenProductsImpl withProduct(String sku, String unitPrice) {
        products.add(new ProductEntry(sku, null, null, null, unitPrice, null));
        return this;
    }

    @Override
    public GivenProductsImpl withProduct(String sku, String title, String brand, String category, String unitPrice, String description) {
        products.add(new ProductEntry(sku, title, brand, category, unitPrice, description));
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        stage(app, null, null);
    }

    /** Stages the list again with this product at a new price; a list without the product is left as it is. */
    public void executeWithPrice(UseCaseDsl app, String skuAlias, String unitPrice) {
        if (products.stream().anyMatch(product -> product.sku().equals(skuAlias))) {
            stage(app, skuAlias, unitPrice);
        }
    }

    /** Stages the list again without this product; a list without the product is left as it is. */
    public void executeWithout(UseCaseDsl app, String skuAlias) {
        if (products.stream().anyMatch(product -> product.sku().equals(skuAlias))) {
            stage(app, null, null, skuAlias);
        }
    }

    private void stage(UseCaseDsl app, String changedSkuAlias, String changedUnitPrice) {
        stage(app, changedSkuAlias, changedUnitPrice, null);
    }

    private void stage(UseCaseDsl app, String changedSkuAlias, String changedUnitPrice, String withdrawnSkuAlias) {
        var returnsProducts = app.erp().returnsProducts();
        for (var product : products) {
            if (product.sku().equals(withdrawnSkuAlias)) {
                continue;
            }
            var unitPrice = product.sku().equals(changedSkuAlias) ? changedUnitPrice : product.unitPrice();
            returnsProducts.product(product.sku(), product.title(), product.brand(), product.category(),
                    unitPrice, product.description());
        }
        returnsProducts.execute().shouldSucceed();
    }

    private record ProductEntry(String sku, String title, String brand, String category, String unitPrice, String description) {
    }
}
