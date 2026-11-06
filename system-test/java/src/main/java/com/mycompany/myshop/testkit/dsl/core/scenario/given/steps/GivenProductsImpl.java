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
        products.add(new ProductEntry(sku, unitPrice));
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        var returnsProducts = app.erp().returnsProducts();
        for (var product : products) {
            returnsProducts.product(product.sku(), product.unitPrice());
        }
        returnsProducts.execute().shouldSucceed();
    }

    private record ProductEntry(String sku, String unitPrice) {
    }
}
