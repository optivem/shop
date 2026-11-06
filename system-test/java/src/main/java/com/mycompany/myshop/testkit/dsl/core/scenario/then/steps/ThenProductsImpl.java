package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.GetProductsVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProducts;

public class ThenProductsImpl implements ThenProducts {
    private final GetProductsVerification verification;

    public ThenProductsImpl(GetProductsVerification verification) {
        this.verification = verification;
    }

    @Override
    public ThenProductsImpl containsProduct(String skuAlias, String price) {
        verification.containsProduct(skuAlias, price);
        return this;
    }

    @Override
    public ThenProductsImpl and() {
        return this;
    }
}
