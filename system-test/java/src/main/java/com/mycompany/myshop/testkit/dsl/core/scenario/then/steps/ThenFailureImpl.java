package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.ErrorVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenFailure;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class ThenFailureImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<Void, VoidVerification> implements ThenFailure {
    private final ErrorVerification failureVerification;
    private final Duration elapsed;

    public ThenFailureImpl(UseCaseDsl app, ExecutionResultContext executionResult,
            UseCaseResult<R, V> result, Duration elapsed) {
        super(app, executionResult, null);
        if (result == null) {
            throw new IllegalStateException("Cannot verify failure: no operation was executed");
        }
        this.failureVerification = result.shouldFail();
        this.elapsed = elapsed;
    }

    public ThenFailureImpl<R, V> errorMessage(String expectedMessage) {
        failureVerification.errorMessage(expectedMessage);
        return this;
    }

    public ThenFailureImpl<R, V> fieldErrorMessage(
            String expectedField, String expectedMessage) {
        failureVerification.fieldErrorMessage(expectedField, expectedMessage);
        return this;
    }

    public ThenFailureImpl<R, V> noOrderPersisted() {
        failureVerification.noOrderPersisted();
        return this;
    }

    public ThenFailureImpl<R, V> serviceUnavailable() {
        failureVerification.serviceUnavailable();
        return this;
    }

    /** The failure came back within {@code seconds}: the system gave up on its own instead of hanging. */
    public ThenFailureImpl<R, V> failedWithinSeconds(int seconds) {
        assertThat(elapsed)
                .withFailMessage("Expected the call to fail within %ds, but it took %dms", seconds, elapsed.toMillis())
                .isLessThanOrEqualTo(Duration.ofSeconds(seconds));
        return this;
    }

    @Override
    public ThenFailureImpl<R, V> and() {
        return this;
    }
}




