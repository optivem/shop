package com.mycompany.myshop.testkit.dsl.core.scenario.when;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
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

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class WhenImpl implements WhenStage {
    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;
    private boolean hasProduct;
    private boolean hasTaxRate;
    private boolean hasPromotion;
    private UserIdentity identity = UserIdentity.DEFAULT;

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario, boolean hasProduct, boolean hasTaxRate, boolean hasPromotion) {
        this.app = app;
        this.scenario = scenario;
        this.hasProduct = hasProduct;
        this.hasTaxRate = hasTaxRate;
        this.hasPromotion = hasPromotion;
    }

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        this(app, scenario, false, false, false);
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

        if (!hasTaxRate) {
            app.tax().returnsTaxRate()
                    .country(DEFAULT_COUNTRY)
                    .taxRate(DEFAULT_TAX_RATE)
                    .execute()
                    .shouldSucceed();
            hasTaxRate = true;
        }

        if (!hasPromotion) {
            app.erp().returnsPromotion()
                    .withActive(DEFAULT_PROMOTION_ACTIVE)
                    .withDiscount(DEFAULT_PROMOTION_DISCOUNT)
                    .execute()
                    .shouldSucceed();
            hasPromotion = true;
        }
    }

    @Override
    public WhenImpl actingAsCustomer() {
        return actingAs(UserIdentity.CUSTOMER);
    }

    @Override
    public WhenImpl actingAsAnotherCustomer() {
        return actingAs(UserIdentity.OTHER_CUSTOMER);
    }

    @Override
    public WhenImpl actingAsAdmin() {
        return actingAs(UserIdentity.ADMIN);
    }

    @Override
    public WhenImpl actingAsAnonymous() {
        return actingAs(UserIdentity.ANONYMOUS);
    }

    private WhenImpl actingAs(UserIdentity identity) {
        this.identity = identity;
        return this;
    }

    private <S extends BaseWhenStep<?, ?>> S asIdentity(S step) {
        step.actingAs(identity);
        return step;
    }

    public WhenPlaceOrderImpl placeOrder() {
        ensureDefaults();
        return asIdentity(new WhenPlaceOrderImpl(app, scenario));
    }

    public WhenCancelOrderImpl cancelOrder() {
        ensureDefaults();
        return asIdentity(new WhenCancelOrderImpl(app, scenario));
    }

    public WhenDeliverOrderImpl deliverOrder() {
        return asIdentity(new WhenDeliverOrderImpl(app, scenario));
    }

    public WhenViewOrderImpl viewOrder() {
        ensureDefaults();
        return asIdentity(new WhenViewOrderImpl(app, scenario));
    }

    public WhenBrowseOrderHistoryImpl browseOrderHistory() {
        return asIdentity(new WhenBrowseOrderHistoryImpl(app, scenario));
    }

    public WhenPublishCouponImpl publishCoupon() {
        return asIdentity(new WhenPublishCouponImpl(app, scenario));
    }

    public WhenBrowseCouponsImpl browseCoupons() {
        return asIdentity(new WhenBrowseCouponsImpl(app, scenario));
    }

}
