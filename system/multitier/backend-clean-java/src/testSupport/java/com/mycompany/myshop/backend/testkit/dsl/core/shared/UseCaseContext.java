package com.mycompany.myshop.backend.testkit.dsl.core.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * What one step of a scenario leaves behind for a later step to refer to, keyed by an alias the test
 * chooses.
 *
 * <p>It exists because the SUT mints order numbers. A test can say {@code given().order()} and then
 * {@code when().cancelOrder()} without ever seeing the real number: {@code given()} registers the
 * minted value under an alias, {@code when()} resolves that alias back. The alternative — letting a
 * test dictate the order number — is ruled out by {@code GivenOrder}'s contract.
 *
 * <p>Fresh per test: {@link com.mycompany.myshop.backend.testkit.dsl.core.usecase.MyShopDsl} owns one,
 * and the whole use case layer is rebuilt in {@code @BeforeEach}.
 *
 * <p>Deliberately narrower than the system-test twin of the same name, which also carries an
 * {@code externalSystemMode} and a {@code paramMap} of UUID-suffixed values. Neither has a job here:
 * the component SUT is always stubbed and always fresh, so there is no real-vs-stub branch and nothing
 * to collide with.
 */
public class UseCaseContext {

    private final Map<String, String> resultMap = new HashMap<>();

    /** Registers what a step produced. Duplicate aliases are a test bug, so they fail loudly. */
    public void setResultEntry(String alias, String value) {
        ensureAliasNotNullBlank(alias);

        if (resultMap.containsKey(alias)) {
            throw new IllegalStateException("Alias already exists: " + alias);
        }

        resultMap.put(alias, value);
    }

    /**
     * Resolves an alias to the value registered under it, or returns the argument unchanged when it
     * was never registered.
     *
     * <p>That fallback is what keeps a literal working where no alias was ever set — {@code
     * withOrderNumber("UNKNOWN")} in a not-found scenario names an order that by definition does not
     * exist, so there is nothing to look up.
     */
    public String getResultValue(String alias) {
        if (isNullOrBlank(alias)) {
            return alias;
        }

        var value = resultMap.get(alias);
        return value == null ? alias : value;
    }

    private static void ensureAliasNotNullBlank(String alias) {
        if (isNullOrBlank(alias)) {
            throw new IllegalArgumentException("Alias cannot be null or blank");
        }
    }

    private static boolean isNullOrBlank(String alias) {
        return alias == null || alias.isBlank();
    }
}
