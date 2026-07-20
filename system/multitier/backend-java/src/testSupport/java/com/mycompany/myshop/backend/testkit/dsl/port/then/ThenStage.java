package com.mycompany.myshop.backend.support.port.then;

import com.mycompany.myshop.backend.support.port.then.steps.ThenClock;
import com.mycompany.myshop.backend.support.port.then.steps.ThenCoupon;
import com.mycompany.myshop.backend.support.port.then.steps.ThenCountry;
import com.mycompany.myshop.backend.support.port.then.steps.ThenOrder;
import com.mycompany.myshop.backend.support.port.then.steps.ThenOrderHistory;
import com.mycompany.myshop.backend.support.port.then.steps.ThenProduct;

/**
 * State the scenario can assert without having executed an action — reached from {@code
 * given().then()}. The order / coupon / history steps read the SUT back through its own API.
 *
 * <p>{@link #clock()}, {@link #product(String)} and {@link #country(String)} mirror system-test's
 * {@code ThenStage} read-backs of the external systems — but they are backed differently. At
 * component level the externals are WireMock stubs the test just programmed, so a <em>test-side</em>
 * read of the stub would be a tautology: it would re-assert the value the test planted. Instead these
 * read through the SUT's <strong>production gateway</strong> ({@code ErpGateway} / {@code TaxGateway}
 * / {@code ClockGateway}) — the SUT's own view of the external, obtained via a real HTTP call and a
 * real parse. That is what makes them able to fail on a real stub drift, and it is exactly why the
 * stub-contract tests use them.
 */
public interface ThenStage {
    ThenOrder order(String orderNumber);

    ThenCoupon coupon(String couponCode);

    ThenOrderHistory orderHistory();

    ThenProduct product(String sku);

    ThenClock clock();

    ThenCountry country(String code);
}
