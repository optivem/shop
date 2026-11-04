package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenBrowseCoupons extends WhenStep {
    WhenBrowseCoupons withPage(String page);

    WhenBrowseCoupons withSize(String size);
}
