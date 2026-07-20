package com.mycompany.myshop.backend.support.core.scenario.given.steps;

import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.given.steps.GivenPromotion;
import java.math.BigDecimal;

public class GivenPromotionImpl extends BaseGivenStep implements GivenPromotion {

    private boolean promotionActive = ScenarioDefaults.DEFAULT_PROMOTION_ACTIVE;
    private String discount = ScenarioDefaults.DEFAULT_PROMOTION_DISCOUNT;

    public GivenPromotionImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenPromotionImpl withActive(boolean promotionActive) {
        this.promotionActive = promotionActive;
        return this;
    }

    @Override
    public GivenPromotionImpl withDiscount(String discount) {
        this.discount = discount;
        return this;
    }

    @Override
    public GivenPromotionImpl withDiscount(double discount) {
        return withDiscount(BigDecimal.valueOf(discount).toPlainString());
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.erp().returnsPromotion().active(promotionActive).discount(discount).execute();
    }
}
