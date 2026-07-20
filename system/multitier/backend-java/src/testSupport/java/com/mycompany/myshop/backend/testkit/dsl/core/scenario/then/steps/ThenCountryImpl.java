package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.then.steps.ThenCountry;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * The country's tax details AS THE SUT SEES THEM: reads through the SUT's production {@code
 * TaxGateway} in the constructor. See {@link ThenProductImpl} for why the read goes through the
 * production gateway rather than the stub client.
 */
public class ThenCountryImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenCountry {

    private final String code;
    private final Optional<TaxDetailsResponse> country;

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
        assertThat(country).as("country %s as parsed by the SUT's TaxGateway", code).isPresent();
        assertThat(country.get().getTaxRate()).isEqualByComparingTo(BigDecimal.valueOf(expectedTaxRate));
        return this;
    }

    @Override
    public ThenCountryImpl<R, V> and() {
        return this;
    }
}
