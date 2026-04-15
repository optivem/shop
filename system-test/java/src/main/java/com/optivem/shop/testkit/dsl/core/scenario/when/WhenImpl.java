package com.optivem.shop.testkit.dsl.core.scenario.when;

import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.when.steps.WhenBrowseCouponsImpl;
import com.optivem.shop.testkit.dsl.core.scenario.when.steps.WhenCancelOrderImpl;
import com.optivem.shop.testkit.dsl.core.scenario.when.steps.WhenPlaceOrderImpl;
import com.optivem.shop.testkit.dsl.core.scenario.when.steps.WhenPublishCouponImpl;
import com.optivem.shop.testkit.dsl.core.scenario.when.steps.WhenViewOrderImpl;
import com.optivem.shop.testkit.dsl.port.when.WhenStage;

import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class WhenImpl implements WhenStage {
    private final UseCaseDsl app;
    private boolean hasProduct;
    private boolean hasTaxRate;
    private boolean hasPromotion;

    public WhenImpl(UseCaseDsl app, boolean hasProduct, boolean hasTaxRate, boolean hasPromotion) {
        this.app = app;
        this.hasProduct = hasProduct;
        this.hasTaxRate = hasTaxRate;
        this.hasPromotion = hasPromotion;
    }

    public WhenImpl(UseCaseDsl app) {
        this(app, false, false, false);
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

    public WhenPlaceOrderImpl placeOrder() {
        ensureDefaults();
        return new WhenPlaceOrderImpl(app);
    }

    public WhenCancelOrderImpl cancelOrder() {
        ensureDefaults();
        return new WhenCancelOrderImpl(app);
    }

    public WhenViewOrderImpl viewOrder() {
        ensureDefaults();
        return new WhenViewOrderImpl(app);
    }

    public WhenPublishCouponImpl publishCoupon() {
        return new WhenPublishCouponImpl(app);
    }

    public WhenBrowseCouponsImpl browseCoupons() {
        return new WhenBrowseCouponsImpl(app);
    }

}
