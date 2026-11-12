package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.CustomerResolver;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseOrderHistory;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseOrderHistoryVerification;

import java.util.function.Supplier;

public class WhenBrowseOrderHistoryImpl extends BaseWhenStep<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> implements WhenBrowseOrderHistory {
    private String orderNumber;
    private String page;
    private String size;
    private Supplier<UserIdentity> customer;
    private boolean resetFiltersAfterwards;
    private final CustomerResolver customers;

    public WhenBrowseOrderHistoryImpl(UseCaseDsl app, ScenarioDslImpl scenario, CustomerResolver customers) {
        super(app, scenario);
        this.customers = customers;
        withOrderNumber(DEFAULT_ORDER_NUMBER);
    }

    public WhenBrowseOrderHistoryImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    public WhenBrowseOrderHistoryImpl withNoOrderNumberFilter() {
        return withOrderNumber(null);
    }

    @Override
    public WhenBrowseOrderHistoryImpl withFiltersResetAfterwards() {
        this.resetFiltersAfterwards = true;
        return this;
    }

    public WhenBrowseOrderHistoryImpl withPage(String page) {
        this.page = page;
        return this;
    }

    public WhenBrowseOrderHistoryImpl withSize(String size) {
        this.size = size;
        return this;
    }

    public WhenBrowseOrderHistoryImpl withCustomer() {
        this.customer = customers::defaultCustomer;
        return this;
    }

    public WhenBrowseOrderHistoryImpl withCustomer(String customerAlias) {
        this.customer = () -> customers.customer(customerAlias);
        return this;
    }

    @Override
    protected ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> execute(UseCaseDsl app) {
        var browseOrderHistory = app.myShop(ChannelMode.DYNAMIC).browseOrderHistory()
                .orderNumber(orderNumber)
                .page(page)
                .size(size)
                .customer(customer == null ? null : customer.get().username());
        if (resetFiltersAfterwards) {
            browseOrderHistory.resetFiltersAfterwards();
        }
        var result = browseOrderHistory.execute();

        return new ExecutionResultBuilder<>(result)
                .orderNumber(orderNumber)
                .customerUsernames(alias -> customers.customer(alias).username())
                .build();
    }
}
