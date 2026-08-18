package com.mycompany.myshop.backend.testkit.dsl.port.then;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenClock;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenCoupon;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenCountry;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenOrder;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenOrderHistory;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenProduct;

public interface ThenStage {
    ThenOrder order(String orderNumber);

    ThenCoupon coupon(String couponCode);

    ThenOrderHistory orderHistory();

    ThenProduct product(String sku);

    ThenClock clock();

    ThenCountry country(String code);
}
