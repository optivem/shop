package com.optivem.eshop.dsl.core.scenario.when;

import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.when.steps.WhenPlaceOrderImpl;
import com.optivem.eshop.dsl.core.scenario.when.steps.WhenViewOrderImpl;
import com.optivem.eshop.dsl.port.when.WhenStage;

import static com.optivem.eshop.dsl.core.scenario.ScenarioDefaults.*;

public class WhenImpl implements WhenStage {
    private final UseCaseDsl app;
    private boolean hasProduct;

    public WhenImpl(UseCaseDsl app, boolean hasProduct) {
        this.app = app;
        this.hasProduct = hasProduct;
    }

    public WhenImpl(UseCaseDsl app) {
        this(app, false);
    }

    private void ensureDefaults() {
        if (!hasProduct) {
            app.erp().returnsProduct()
                    .sku(DEFAULT_SKU)
                    .unitPrice(DEFAULT_UNIT_PRICE)
                    .execute()
                    .shouldSucceed();
            hasProduct = true;
        }
    }

    public WhenPlaceOrderImpl placeOrder() {
        ensureDefaults();
        return new WhenPlaceOrderImpl(app);
    }

    public WhenViewOrderImpl viewOrder() {
        ensureDefaults();
        return new WhenViewOrderImpl(app);
    }

}



