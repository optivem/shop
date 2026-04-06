package com.optivem.shop.dsl.core.scenario.when;

import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.core.scenario.when.steps.WhenBrowseCouponsImpl;
import com.optivem.shop.dsl.core.scenario.when.steps.WhenPlaceOrderImpl;
import com.optivem.shop.dsl.core.scenario.when.steps.WhenPublishCouponImpl;
import com.optivem.shop.dsl.core.scenario.when.steps.WhenViewOrderImpl;
import com.optivem.shop.dsl.port.when.WhenStage;

import static com.optivem.shop.dsl.core.scenario.ScenarioDefaults.*;

public class WhenImpl implements WhenStage {
    private final UseCaseDsl app;
    private boolean hasPromotion;
    private boolean hasProduct;

    public WhenImpl(UseCaseDsl app, boolean hasProduct, boolean hasPromotion) {
        this.app = app;
        this.hasProduct = hasProduct;
        this.hasPromotion = hasPromotion;
    }

    public WhenImpl(UseCaseDsl app, boolean hasProduct) {
        this(app, hasProduct, false);
    }

    public WhenImpl(UseCaseDsl app) {
        this(app, false, false);
    }

    private void ensureDefaults() {
        if (!hasPromotion) {
            app.erp().returnsPromotion()
                    .withActive(DEFAULT_PROMOTION_ACTIVE)
                    .withDiscount(DEFAULT_PROMOTION_DISCOUNT)
                    .execute()
                    .shouldSucceed();
            hasPromotion = true;
        }

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

    public WhenPublishCouponImpl publishCoupon() {
        return new WhenPublishCouponImpl(app);
    }

    public WhenBrowseCouponsImpl browseCoupons() {
        return new WhenBrowseCouponsImpl(app);
    }

}
