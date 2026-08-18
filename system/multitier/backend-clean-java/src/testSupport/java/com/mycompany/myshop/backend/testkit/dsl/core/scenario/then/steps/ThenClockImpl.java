package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenClock;
import java.time.Instant;

public class ThenClockImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenClock {

    private final Instant time;

    public ThenClockImpl(
            UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        this.time = app.sutClock().readTime();
    }

    @Override
    public ThenClockImpl<R, V> hasTime(String expectedTime) {
        assertThat(time)
            .as("current time as parsed by the SUT's ClockGateway")
            .isEqualTo(Instant.parse(expectedTime));
        return this;
    }

    @Override
    public ThenClockImpl<R, V> and() {
        return this;
    }
}
