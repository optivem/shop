package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The country an order is placed from and a tax rate belongs to. Small on purpose: the interesting
 * assertion is the one about what it does <em>not</em> do — see
 * {@link #doesNotTreatDifferentlyCasedValuesAsTheSameCountry()}.
 */
class CountryTest {

    @Test
    void carriesItsValue() {
        assertThat(Country.of("US").value()).isEqualTo("US");
    }

    /** Compared by value, which is what lets an order's country be matched against a tax rate's. */
    @Test
    void isEqualToAnotherCountryWithTheSameValue() {
        assertThat(Country.of("US")).isEqualTo(Country.of("US"));
        assertThat(Country.of("US")).isNotEqualTo(Country.of("DE"));
    }

    /** Prints as the bare value: it reaches an outbound URL and log lines as-is. */
    @Test
    void printsAsItsValue() {
        assertThat(Country.of("US")).hasToString("US");
    }

    @Test
    void requiresAValue() {
        assertThat(catchThrowable(() -> Country.of(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("country cannot be null or empty");
        assertThat(catchThrowable(() -> Country.of("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("country cannot be null or empty");
    }

    /**
     * Pins the deliberate omission. Whether {@code "us"} and {@code "US"} name the same country is
     * the tax system's answer to give, and normalising here would silently change which orders can be
     * placed. If this ever becomes the domain's decision, this is the test that should change first.
     */
    @Test
    void doesNotTreatDifferentlyCasedValuesAsTheSameCountry() {
        assertThat(Country.of("us")).isNotEqualTo(Country.of("US"));
    }

    /** Nor does it trim: the value reaches the tax system exactly as it arrived. */
    @Test
    void doesNotTrimSurroundingWhitespace() {
        assertThat(Country.of(" US ").value()).isEqualTo(" US ");
    }
}
