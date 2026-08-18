package com.mycompany.myshop.backend.testkit.dsl.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class UseCaseResult<R, V extends ResponseVerification<R>> {

    private final HttpStatusCode actualStatus;
    private final HttpStatus successStatus;
    private final Set<HttpStatus> rejectionStatuses;
    private final Supplier<R> successBody;
    private final Supplier<ErrorVerification> rejectionBody;
    private final Function<R, V> successVerificationFactory;

    public UseCaseResult(
            HttpStatusCode actualStatus,
            HttpStatus successStatus,
            Set<HttpStatus> rejectionStatuses,
            Supplier<R> successBody,
            Supplier<ErrorVerification> rejectionBody,
            Function<R, V> successVerificationFactory) {
        this.actualStatus = actualStatus;
        this.successStatus = successStatus;
        this.rejectionStatuses = rejectionStatuses;
        this.successBody = successBody;
        this.rejectionBody = rejectionBody;
        this.successVerificationFactory = successVerificationFactory;
    }

    public boolean isSuccess() {
        return successStatus.equals(actualStatus);
    }

    public V shouldSucceed() {
        assertThat(actualStatus).as("response status").isEqualTo(successStatus);
        return successVerificationFactory.apply(successBody.get());
    }

    public ErrorVerification shouldFail() {
        if (rejectionStatuses.isEmpty()) {
            throw new IllegalStateException(
                "This use case has no rejection contract in the component-test DSL, so shouldFail() "
                    + "cannot assert anything. Drive it through the use case layer if you need to "
                    + "inspect the raw response.");
        }
        assertThat(actualStatus).as("rejection status").isIn(rejectionStatuses);
        return rejectionBody.get();
    }

    public R responseOrNull() {
        return isSuccess() ? successBody.get() : null;
    }
}
