package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ViewProfileVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenViewProfile;

public class WhenViewProfileImpl extends BaseWhenStep<ViewProfileResponse, ViewProfileVerification> implements WhenViewProfile {
    public WhenViewProfileImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    protected ExecutionResult<ViewProfileResponse, ViewProfileVerification> execute(UseCaseDsl app) {
        var result = app.myShop(ChannelMode.DYNAMIC).viewProfile().execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}
