package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.dtos.external.ProductDetailsResponse;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.then.steps.ThenProduct;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * The product AS THE SUT SEES IT: reads through the SUT's production {@code ErpGateway} in the
 * constructor — real HTTP to the ERP stub + real {@code ProductDetailsResponse} parse — so the stub's
 * bytes actually travel the SUT's consumption path. A field-name drift in the component stub
 * (e.g. {@code price}→{@code cost}) then makes the assertion fail, rather than silently yielding null.
 */
public class ThenProductImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenProduct {

    private final String sku;
    private final Optional<ProductDetailsResponse> product;

    public ThenProductImpl(
            UseCaseDsl app,
            ExecutionResultContext executionResult,
            String sku,
            V successVerification) {
        super(app, executionResult, successVerification);
        this.sku = sku;
        this.product = app.sutErp().readProduct(sku);
    }

    @Override
    public ThenProductImpl<R, V> hasSku(String expectedSku) {
        assertThat(product).as("product %s as parsed by the SUT's ErpGateway", sku).isPresent();
        assertThat(product.get().getId()).isEqualTo(expectedSku);
        return this;
    }

    @Override
    public ThenProductImpl<R, V> hasPrice(double expectedPrice) {
        assertThat(product).as("product %s as parsed by the SUT's ErpGateway", sku).isPresent();
        assertThat(product.get().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(expectedPrice));
        return this;
    }

    @Override
    public ThenProductImpl<R, V> and() {
        return this;
    }
}
