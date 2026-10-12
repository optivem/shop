package com.mycompany.myshop.testkit.dsl.core.scenario.when;

import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.DefaultGivens;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenBrowseCouponsImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenBrowseOrderHistoryImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenCancelOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenDeliverOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenPlaceOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenPublishCouponImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.BaseWhenStep;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenViewOrderImpl;
import com.mycompany.myshop.testkit.dsl.port.when.WhenStage;

public class WhenImpl implements WhenStage {
    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;
    private final DefaultGivens defaults;

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario, DefaultGivens defaults) {
        this.app = app;
        this.scenario = scenario;
        this.defaults = defaults;
    }

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        this(app, scenario, new DefaultGivens(app));
    }

    private void ensureDefaults() {
        defaults.ensure(true);
    }

    public WhenPlaceOrderImpl placeOrder() {
        ensureDefaults();
        return new WhenPlaceOrderImpl(app, scenario);
    }

    public WhenCancelOrderImpl cancelOrder() {
        ensureDefaults();
        return new WhenCancelOrderImpl(app, scenario);
    }

    public WhenDeliverOrderImpl deliverOrder() {
        ensureDefaults();
        return new WhenDeliverOrderImpl(app, scenario);
    }

    public WhenViewOrderImpl viewOrder() {
        ensureDefaults();
        return new WhenViewOrderImpl(app, scenario);
    }

    public WhenBrowseOrderHistoryImpl browseOrderHistory() {
        ensureDefaults();
        return new WhenBrowseOrderHistoryImpl(app, scenario);
    }

    public WhenPublishCouponImpl publishCoupon() {
        ensureDefaults();
        return new WhenPublishCouponImpl(app, scenario);
    }

    public WhenBrowseCouponsImpl browseCoupons() {
        ensureDefaults();
        return new WhenBrowseCouponsImpl(app, scenario);
    }

}
