package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductResponse;
import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

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
                .isEqualByComparingTo(expectedPrice);
        return this;
    }

    public GetProductVerification price(String expectedPrice) {
        return price(Converter.toBigDecimal(expectedPrice));
    }

    public GetProductVerification title(String expectedTitle) {
        var actualTitle = getResponse().getTitle();
        assertThat(actualTitle)
                .withFailMessage("Expected title to be '%s', but was '%s'", expectedTitle, actualTitle)
                .isEqualTo(expectedTitle);
        return this;
    }

    public GetProductVerification description(String expectedDescription) {
        var actualDescription = getResponse().getDescription();
        assertThat(actualDescription)
                .withFailMessage("Expected description to be '%s', but was '%s'", expectedDescription, actualDescription)
                .isEqualTo(expectedDescription);
        return this;
    }

    public GetProductVerification category(String expectedCategory) {
        var actualCategory = getResponse().getCategory();
        assertThat(actualCategory)
                .withFailMessage("Expected category to be '%s', but was '%s'", expectedCategory, actualCategory)
                .isEqualTo(expectedCategory);
        return this;
    }

    public GetProductVerification brand(String expectedBrand) {
        var actualBrand = getResponse().getBrand();
        assertThat(actualBrand)
                .withFailMessage("Expected brand to be '%s', but was '%s'", expectedBrand, actualBrand)
                .isEqualTo(expectedBrand);
        return this;
    }
}
