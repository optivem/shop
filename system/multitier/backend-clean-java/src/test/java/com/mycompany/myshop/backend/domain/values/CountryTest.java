package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CountryTest {

    @Test
    void carriesItsValue() {
        assertThat(Country.of("US").value()).isEqualTo("US");
    }

    @Test
    void isEqualToAnotherCountryWithTheSameValue() {
        assertThat(Country.of("US")).isEqualTo(Country.of("US"));
        assertThat(Country.of("US")).isNotEqualTo(Country.of("DE"));
    }

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

    @Test
    void doesNotTreatDifferentlyCasedValuesAsTheSameCountry() {
        assertThat(Country.of("us")).isNotEqualTo(Country.of("US"));
    }

    @Test
    void doesNotTrimSurroundingWhitespace() {
        assertThat(Country.of(" US ").value()).isEqualTo(" US ");
    }
}
