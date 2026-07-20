package com.mycompany.myshop.backend.support.port.given.steps;

import com.mycompany.myshop.backend.support.port.given.steps.base.GivenStep;

public interface GivenPromotion extends GivenStep {
    GivenPromotion withActive(boolean promotionActive);

    GivenPromotion withDiscount(String discount);

    GivenPromotion withDiscount(double discount);
}
