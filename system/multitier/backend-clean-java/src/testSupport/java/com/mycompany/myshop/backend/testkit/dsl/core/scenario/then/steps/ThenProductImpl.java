package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.infrastructure.external.erp.ProductDetailsResponse;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.ThenProduct;
import java.math.BigDecimal;
import java.util.Optional;

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
        return hasPrice(BigDecimal.valueOf(expectedPrice));
    }

    @Override
    public ThenProductImpl<R, V> hasPrice(String expectedPrice) {
        return hasPrice(new BigDecimal(expectedPrice));
    }

    private ThenProductImpl<R, V> hasPrice(BigDecimal expectedPrice) {
        assertThat(product).as("product %s as parsed by the SUT's ErpGateway", sku).isPresent();
        assertThat(product.get().getPrice()).isEqualByComparingTo(expectedPrice);
        return this;
    }

    @Override
    public ThenProductImpl<R, V> and() {
        return this;
    }
}
