package com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenClock;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenCoupon;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenCountry;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenOrder;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenOrderHistory;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenProduct;

public interface ThenStep<T> {
    T and();

    ThenOrder order();

    ThenOrder order(String orderNumber);

    ThenCoupon coupon();

    ThenCoupon coupon(String couponCode);

    ThenOrderHistory orderHistory();

    ThenProduct product(String sku);

    ThenClock clock();

    ThenCountry country(String code);
}
