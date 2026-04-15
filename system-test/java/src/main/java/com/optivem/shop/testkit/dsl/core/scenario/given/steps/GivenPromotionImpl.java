package com.optivem.shop.testkit.dsl.core.scenario.given.steps;

import com.optivem.shop.testkit.common.Converter;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.given.GivenImpl;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenPromotion;

import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_PROMOTION_ACTIVE;
import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_PROMOTION_DISCOUNT;

public class GivenPromotionImpl extends BaseGivenStep implements GivenPromotion {
    private boolean promotionActive;
    private String discount;

    public GivenPromotionImpl(GivenImpl given) {
        super(given);
        this.promotionActive = DEFAULT_PROMOTION_ACTIVE;
        this.discount = DEFAULT_PROMOTION_DISCOUNT;
    }

    @Override
    public GivenPromotionImpl withActive(boolean promotionActive) {
        this.promotionActive = promotionActive;
        return this;
    }

    @Override
    public GivenPromotionImpl withDiscount(double discount) {
        return withDiscount(Converter.fromDouble(discount));
    }

    @Override
    public GivenPromotionImpl withDiscount(String discount) {
        this.discount = discount;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.erp().returnsPromotion()
                .withActive(promotionActive)
                .withDiscount(discount)
                .execute()
                .shouldSucceed();
    }
}
