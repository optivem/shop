package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.CustomerResolver;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCustomersVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseCustomers;

public class WhenBrowseCustomersImpl extends BaseWhenStep<BrowseCustomersResponse, BrowseCustomersVerification> implements WhenBrowseCustomers {
    private final CustomerResolver customers;

    public WhenBrowseCustomersImpl(UseCaseDsl app, ScenarioDslImpl scenario, CustomerResolver customers) {
        super(app, scenario);
        this.customers = customers;
    }

    @Override
    protected ExecutionResult<BrowseCustomersResponse, BrowseCustomersVerification> execute(UseCaseDsl app) {
        var result = app.myShop(ChannelMode.DYNAMIC).browseCustomers().execute();
        return new ExecutionResultBuilder<>(result)
                .customerUsernames(alias -> customers.customer(alias).username())
                .build();
    }
}
