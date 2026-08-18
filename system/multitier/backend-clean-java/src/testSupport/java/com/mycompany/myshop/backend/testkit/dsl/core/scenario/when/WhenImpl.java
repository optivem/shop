package com.mycompany.myshop.backend.testkit.dsl.core.scenario.when;

import com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps.WhenBrowseCouponsImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps.WhenBrowseOrderHistoryImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps.WhenCancelOrderImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps.WhenPlaceOrderImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps.WhenPublishCouponImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps.WhenViewOrderImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.when.WhenStage;

public class WhenImpl implements WhenStage {

    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;
    private boolean hasClock;
    private boolean hasProduct;
    private boolean hasTaxRate;
    private boolean hasPromotion;

    public WhenImpl(
            UseCaseDsl app,
            ScenarioDslImpl scenario,
            boolean hasClock,
            boolean hasProduct,
            boolean hasTaxRate,
            boolean hasPromotion) {
        this.app = app;
        this.scenario = scenario;
        this.hasClock = hasClock;
        this.hasProduct = hasProduct;
        this.hasTaxRate = hasTaxRate;
        this.hasPromotion = hasPromotion;
    }

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        this(app, scenario, false, false, false, false);
    }

    @Override
    public WhenPlaceOrderImpl placeOrder() {
        ensureDefaults();
        return new WhenPlaceOrderImpl(app, scenario);
    }

    // Defaults apply: cancelling reads the clock to evaluate the year-end blackout, so an unstated
    // clock would 404 the SUT's time lookup rather than quietly giving it "now".
    @Override
    public WhenCancelOrderImpl cancelOrder() {
        ensureDefaults();
        return new WhenCancelOrderImpl(app, scenario);
    }

    @Override
    public WhenViewOrderImpl viewOrder() {
        ensureDefaults();
        return new WhenViewOrderImpl(app, scenario);
    }

    @Override
    public WhenBrowseOrderHistoryImpl browseOrderHistory() {
        ensureDefaults();
        return new WhenBrowseOrderHistoryImpl(app, scenario);
    }

    @Override
    public WhenPublishCouponImpl publishCoupon() {
        return new WhenPublishCouponImpl(app, scenario);
    }

    @Override
    public WhenBrowseCouponsImpl browseCoupons() {
        return new WhenBrowseCouponsImpl(app, scenario);
    }

    private void ensureDefaults() {
        if (!hasClock) {
            app.clock().returnsTime().time(ScenarioDefaults.DEFAULT_TIME).execute();
            hasClock = true;
        }

        if (!hasProduct) {
            app.erp().returnsProduct()
                .sku(ScenarioDefaults.DEFAULT_SKU)
                .unitPrice(ScenarioDefaults.DEFAULT_UNIT_PRICE)
                .execute();
            hasProduct = true;
        }

        if (!hasTaxRate) {
            app.tax().returnsTaxRate()
                .country(ScenarioDefaults.DEFAULT_COUNTRY)
                .taxRate(ScenarioDefaults.DEFAULT_TAX_RATE)
                .execute();
            hasTaxRate = true;
        }

        if (!hasPromotion) {
            app.erp().returnsPromotion()
                .active(ScenarioDefaults.DEFAULT_PROMOTION_ACTIVE)
                .discount(ScenarioDefaults.DEFAULT_PROMOTION_DISCOUNT)
                .execute();
            hasPromotion = true;
        }
    }
}
