package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.domain.values.TaxRate;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenCountry;
import java.math.BigDecimal;
import java.util.Optional;

public class ThenCountryImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenCountry {

    private final String code;
    private final Optional<TaxRate> country;

    public ThenCountryImpl(
            UseCaseDsl app,
            ExecutionResultContext executionResult,
            String code,
            V successVerification) {
        super(app, executionResult, successVerification);
        this.code = code;
        this.country = app.sutTax().readCountry(code);
    }

    @Override
    public ThenCountryImpl<R, V> hasTaxRate(double expectedTaxRate) {
        return hasTaxRate(BigDecimal.valueOf(expectedTaxRate));
    }

    @Override
    public ThenCountryImpl<R, V> hasTaxRate(String expectedTaxRate) {
        return hasTaxRate(new BigDecimal(expectedTaxRate));
    }

    private ThenCountryImpl<R, V> hasTaxRate(BigDecimal expectedTaxRate) {
        assertThat(country).as("country %s as parsed by the SUT's TaxGateway", code).isPresent();
        assertThat(country.get().rate().value()).isEqualByComparingTo(expectedTaxRate);
        return this;
    }

    @Override
    public ThenCountryImpl<R, V> and() {
        return this;
    }
}
