package com.mycompany.myshop.backend.support.core.scenario.then;

import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.scenario.then.steps.ThenClockImpl;
import com.mycompany.myshop.backend.support.core.scenario.then.steps.ThenCountryImpl;
import com.mycompany.myshop.backend.support.core.scenario.then.steps.ThenCouponImpl;
import com.mycompany.myshop.backend.support.core.scenario.then.steps.ThenOrderHistoryImpl;
import com.mycompany.myshop.backend.support.core.scenario.then.steps.ThenOrderImpl;
import com.mycompany.myshop.backend.support.core.scenario.then.steps.ThenProductImpl;
import com.mycompany.myshop.backend.support.core.shared.VoidVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.then.ThenStage;

/**
 * Assertions available without an action having run — reached from {@code given().then()}. Every
 * entity has to be named explicitly here, because there is no execution result to resolve it from.
 */
public class ThenImpl implements ThenStage {

    protected final UseCaseDsl app;

    public ThenImpl(UseCaseDsl app) {
        this.app = app;
    }

    @Override
    public ThenOrderImpl<Void, VoidVerification> order(String orderNumber) {
        return new ThenOrderImpl<>(app, ExecutionResultContext.empty(), orderNumber, null);
    }

    @Override
    public ThenCouponImpl<Void, VoidVerification> coupon(String couponCode) {
        return new ThenCouponImpl<>(app, ExecutionResultContext.empty(), couponCode, null);
    }

    @Override
    public ThenOrderHistoryImpl<Void, VoidVerification> orderHistory() {
        return new ThenOrderHistoryImpl<>(app, ExecutionResultContext.empty(), null);
    }

    @Override
    public ThenProductImpl<Void, VoidVerification> product(String sku) {
        return new ThenProductImpl<>(app, ExecutionResultContext.empty(), sku, null);
    }

    @Override
    public ThenClockImpl<Void, VoidVerification> clock() {
        return new ThenClockImpl<>(app, ExecutionResultContext.empty(), null);
    }

    @Override
    public ThenCountryImpl<Void, VoidVerification> country(String code) {
        return new ThenCountryImpl<>(app, ExecutionResultContext.empty(), code, null);
    }
}
