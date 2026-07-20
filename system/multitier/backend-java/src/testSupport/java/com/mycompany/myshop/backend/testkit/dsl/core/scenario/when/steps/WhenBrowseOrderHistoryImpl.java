package com.mycompany.myshop.backend.support.core.scenario.when.steps;

import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.BrowseOrderHistoryVerification;
import com.mycompany.myshop.backend.support.port.when.steps.WhenBrowseOrderHistory;

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
