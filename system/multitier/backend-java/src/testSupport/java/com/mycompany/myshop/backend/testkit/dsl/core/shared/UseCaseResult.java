package com.mycompany.myshop.backend.testkit.dsl.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
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
    private final Set<HttpStatus> rejectionStatuses;
    private final Supplier<R> successBody;
    private final Supplier<ErrorVerification> rejectionBody;
    private final Function<R, V> successVerificationFactory;

    /**
     * {@code rejectionStatuses} is a set rather than a single status because an endpoint can reject
     * for more than one reason. Cancelling an order is the case in point: the year-end blackout and
     * an already-cancelled order are both {@code 422}, but an unknown order number is {@code 404}.
     * Declaring one and asserting it would make the other scenario unwritable. Pass {@code Set.of()}
     * for an endpoint with no rejection contract.
     */
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

    /** The success payload, or {@code null} if the call was not accepted. Used to carry the order number forward. */
    public R responseOrNull() {
        return isSuccess() ? successBody.get() : null;
    }
}
