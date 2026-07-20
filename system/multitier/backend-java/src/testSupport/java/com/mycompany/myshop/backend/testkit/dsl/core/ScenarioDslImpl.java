package com.mycompany.myshop.backend.support.core;

import com.mycompany.myshop.backend.support.core.scenario.assume.AssumeImpl;
import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.scenario.when.WhenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.ScenarioDsl;

/**
 * The scenario DSL, built on the use case layer ({@link UseCaseDsl}).
 *
 * <pre>{@code
 * scenario.given()
 *         .product().withSku("BOOK-123").withUnitPrice("10.00")
 *     .and().country().withCode("US").withTaxRate("0.10")
 *     .when().placeOrder().withSku("BOOK-123").withQuantity(2).withCountry("US")
 *     .then().shouldSucceed()
 *     .and().order().hasTotalPrice("22.00");
 * }</pre>
 *
 * <p>One scenario per test: once an action has run, {@code given()} and {@code when()} refuse to
 * start another. A test method that wants a second Given-When-Then is a second test.
 */
public class ScenarioDslImpl implements ScenarioDsl {

    private final UseCaseDsl app;
    private boolean executed;

    public ScenarioDslImpl(UseCaseDsl app) {
        this.app = app;
    }

    @Override
    public AssumeImpl assume() {
        return new AssumeImpl(app);
    }

    @Override
    public GivenImpl given() {
        ensureNotExecuted();
        return new GivenImpl(app, this);
    }

    @Override
    public WhenImpl when() {
        ensureNotExecuted();
        return new WhenImpl(app, this);
    }

    public void markAsExecuted() {
        this.executed = true;
    }

    private void ensureNotExecuted() {
        if (executed) {
            throw new IllegalStateException(
                "Scenario has already been executed. Each test method should contain only ONE "
                    + "scenario execution (Given-When-Then). Split multiple scenarios into separate "
                    + "test methods.");
        }
    }
}
