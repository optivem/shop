package com.mycompany.myshop.testkit.dsl.core.scenario;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.*;

/**
 * Single owner of "which default givens are present". Values come only from ScenarioDefaults;
 * each default is created at most once per scenario.
 */
public class DefaultGivens {
    private final UseCaseDsl app;
    private boolean hasProduct;
    private boolean hasTaxRate;
    private boolean hasPromotion;
    private boolean hasCoupon;
    private boolean hasOrder;

    public DefaultGivens(UseCaseDsl app) {
        this.app = app;
    }

    public void markProduct() {
        hasProduct = true;
    }

    public void markTaxRate() {
        hasTaxRate = true;
    }

    public void markPromotion() {
        hasPromotion = true;
    }

    public void markCoupon() {
        hasCoupon = true;
    }

    public void markOrder() {
        hasOrder = true;
    }

    /** Creates the missing defaults that something depends on: orders, or a When step that acts. */
    public void ensure(boolean whenStepRuns) {
        if (whenStepRuns && !hasPromotion) {
            app.erp().returnsPromotion()
                    .withActive(DEFAULT_PROMOTION_ACTIVE)
                    .withDiscount(DEFAULT_PROMOTION_DISCOUNT)
                    .execute()
                    .shouldSucceed();
            hasPromotion = true;
        }

        var dependentsExist = whenStepRuns || hasOrder;

        if (dependentsExist && !hasProduct) {
            app.erp().returnsProduct()
                    .sku(DEFAULT_SKU)
                    .unitPrice(DEFAULT_UNIT_PRICE)
                    .execute()
                    .shouldSucceed();
            hasProduct = true;
        }

        if (dependentsExist && !hasTaxRate) {
            app.tax().returnsTaxRate()
                    .country(DEFAULT_COUNTRY)
                    .taxRate(DEFAULT_TAX_RATE)
                    .execute()
                    .shouldSucceed();
            hasTaxRate = true;
        }

        if (hasOrder && !hasCoupon) {
            app.myShop().publishCoupon()
                    .couponCode(DEFAULT_COUPON_CODE)
                    .discountRate(DEFAULT_DISCOUNT_RATE)
                    .validFrom(EMPTY)
                    .validTo(EMPTY)
                    .usageLimit(EMPTY)
                    .execute()
                    .shouldSucceed();
            hasCoupon = true;
        }
    }
}
