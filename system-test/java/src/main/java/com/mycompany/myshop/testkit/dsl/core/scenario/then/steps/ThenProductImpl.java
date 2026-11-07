package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.GetProductVerification;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProduct;

public class ThenProductImpl extends BaseThenStep<Void, VoidVerification> implements ThenProduct {
    private final GetProductVerification verification;

    public ThenProductImpl(UseCaseDsl app, ExecutionResultContext executionResult, GetProductVerification verification) {
        super(app, executionResult, null);
        this.verification = verification;
    }

    @Override
    public ThenProductImpl hasSku(String sku) {
        verification.sku(sku);
        return this;
    }

    @Override
    public ThenProductImpl hasPrice(String price) {
        verification.price(price);
        return this;
    }

    @Override
    public ThenProductImpl hasTitle(String title) {
        verification.title(title);
        return this;
    }

    @Override
    public ThenProductImpl hasDescription(String description) {
        verification.description(description);
        return this;
    }

    @Override
    public ThenProductImpl hasCategory(String category) {
        verification.category(category);
        return this;
    }

    @Override
    public ThenProductImpl hasBrand(String brand) {
        verification.brand(brand);
        return this;
    }

    @Override
    public ThenProductImpl and() {
        return this;
    }
}
