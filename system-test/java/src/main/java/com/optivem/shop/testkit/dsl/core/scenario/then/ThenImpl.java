package com.optivem.shop.testkit.dsl.core.scenario.then;

import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.optivem.shop.testkit.dsl.core.scenario.then.steps.ThenClockImpl;
import com.optivem.shop.testkit.dsl.core.scenario.then.steps.ThenCountryImpl;
import com.optivem.shop.testkit.dsl.core.scenario.then.steps.ThenProductImpl;
import com.optivem.shop.testkit.dsl.port.then.ThenStage;
import com.optivem.shop.testkit.dsl.port.then.steps.ThenClock;
import com.optivem.shop.testkit.dsl.port.then.steps.ThenCountry;
import com.optivem.shop.testkit.dsl.port.then.steps.ThenProduct;

public class ThenImpl implements ThenStage {
    protected final UseCaseDsl app;

    public ThenImpl(UseCaseDsl app) {
        this.app = app;
    }

    @Override
    public ThenClock clock() {
        var verification = app.clock().getTime().execute().shouldSucceed();
        return new ThenClockImpl(app, ExecutionResultContext.empty(), verification);
    }

    @Override
    public ThenProduct product(String skuAlias) {
        var verification = app.erp().getProduct().sku(skuAlias).execute().shouldSucceed();
        return new ThenProductImpl(app, ExecutionResultContext.empty(), verification);
    }

    @Override
    public ThenCountry country(String countryAlias) {
        var verification = app.tax().getTaxRate().country(countryAlias).execute().shouldSucceed();
        return new ThenCountryImpl(app, ExecutionResultContext.empty(), verification);
    }

}
