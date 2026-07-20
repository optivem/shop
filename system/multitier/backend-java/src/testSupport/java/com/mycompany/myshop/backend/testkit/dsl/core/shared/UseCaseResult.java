package com.mycompany.myshop.backend.support.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * The outcome of a use case against the system under test, awaiting the test's expectation:
 * {@link #shouldSucceed()} asserts acceptance and hands back the payload verification,
 * {@link #shouldFail()} asserts rejection and hands back the {@link ErrorVerification}.
 *
 * <p>The system under test owns its HTTP contract, so each use case declares the statuses that
 * contract uses — place order accepts with {@code 201} and rejects with {@code 422}, view order
 * answers {@code 200} or {@code 404} — and the assertion lives here rather than leaking into the
 * tests. A use case whose endpoint cannot be rejected (or whose driver binds the body to a type that
 * would discard a {@code ProblemDetail}) passes a {@code null} {@code rejectionStatus}; calling
 * {@link #shouldFail()} on it then fails loudly instead of asserting something meaningless.
 */
public class UseCaseResult<R, V extends ResponseVerification<R>> {

    private final HttpStatusCode actualStatus;
    private final HttpStatus successStatus;
    private final HttpStatus rejectionStatus;
    private final Supplier<R> successBody;
    private final Supplier<ErrorVerification> rejectionBody;
    private final Function<R, V> successVerificationFactory;

    public UseCaseResult(
            HttpStatusCode actualStatus,
            HttpStatus successStatus,
            HttpStatus rejectionStatus,
            Supplier<R> successBody,
            Supplier<ErrorVerification> rejectionBody,
            Function<R, V> successVerificationFactory) {
        this.actualStatus = actualStatus;
        this.successStatus = successStatus;
        this.rejectionStatus = rejectionStatus;
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
        if (rejectionStatus == null) {
            throw new IllegalStateException(
                "This use case has no rejection contract in the component-test DSL, so shouldFail() "
                    + "cannot assert anything. Drive it through the use case layer if you need to "
                    + "inspect the raw response.");
        }
        assertThat(actualStatus).as("rejection status").isEqualTo(rejectionStatus);
        return rejectionBody.get();
    }

    /** The success payload, or {@code null} if the call was not accepted. Used to carry the order number forward. */
    public R responseOrNull() {
        return isSuccess() ? successBody.get() : null;
    }
}
