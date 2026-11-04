package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseOrderHistory;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseOrderHistoryVerification;

public class WhenBrowseOrderHistoryImpl extends BaseWhenStep<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> implements WhenBrowseOrderHistory {
    private String orderNumber;
    private String page;
    private String size;

    public WhenBrowseOrderHistoryImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
        withOrderNumber(DEFAULT_ORDER_NUMBER);
    }

    public WhenBrowseOrderHistoryImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    @Override
    protected ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> execute(UseCaseDsl app) {
        var result = app.myShop(ChannelMode.DYNAMIC).browseOrderHistory()
                .orderNumber(orderNumber)
                .page(page)
                .size(size)
                .execute();

        return new ExecutionResultBuilder<>(result)
                .orderNumber(orderNumber)
                .build();
    }
}
