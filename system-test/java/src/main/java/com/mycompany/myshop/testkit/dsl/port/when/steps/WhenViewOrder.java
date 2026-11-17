package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenViewOrder extends WhenStep {
    WhenViewOrder withOrderNumber(String orderNumber);

    /** A visitor who is asked to sign in signs in as the customer, and is then shown the order (UI only). */
    WhenViewOrder withSignInWhenAsked();
}

