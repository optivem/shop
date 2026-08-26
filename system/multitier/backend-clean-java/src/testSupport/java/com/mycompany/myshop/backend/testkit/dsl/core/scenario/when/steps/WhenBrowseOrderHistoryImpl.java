package com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.BrowseOrderHistoryVerification;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenBrowseOrderHistory;

public class WhenBrowseOrderHistoryImpl
        extends BaseWhenStep<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>
        implements WhenBrowseOrderHistory {

    public WhenBrowseOrderHistoryImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    protected ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> execute(
            UseCaseDsl app) {
        var result = app.myShop().browseOrderHistory().execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}
