package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then;

import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenClockImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenCountryImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenCouponImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenOrderHistoryImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenOrderImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenProductImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.then.ThenStage;

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
