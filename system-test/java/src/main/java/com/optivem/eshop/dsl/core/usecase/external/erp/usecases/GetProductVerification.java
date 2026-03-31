package com.optivem.eshop.dsl.core.usecase.external.erp.usecases;

import com.optivem.eshop.dsl.driver.port.external.erp.dtos.GetProductResponse;
import com.optivem.eshop.dsl.common.Converter;
import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class GetProductVerification extends ResponseVerification<GetProductResponse> {
    public GetProductVerification(GetProductResponse response, UseCaseContext context) {
        super(response, context);
    }

    public GetProductVerification sku(String skuParamAlias) {
        var expectedSku = getContext().getParamValue(skuParamAlias);
        var actualSku = getResponse().getSku();
        assertThat(actualSku)
                .withFailMessage("Expected SKU to be '%s', but was '%s'", expectedSku, actualSku)
                .isEqualTo(expectedSku);
        return this;
    }

    public GetProductVerification price(BigDecimal expectedPrice) {
        var actualPrice = getResponse().getPrice();
        assertThat(actualPrice)
                .withFailMessage("Expected price to be %s, but was %s", expectedPrice, actualPrice)
                .isEqualTo(expectedPrice);
        return this;
    }

    public GetProductVerification price(double expectedPrice) {
        return price(Converter.toBigDecimal(expectedPrice));
    }

    public GetProductVerification price(String expectedPrice) {
        return price(Converter.toBigDecimal(expectedPrice));
    }
}
