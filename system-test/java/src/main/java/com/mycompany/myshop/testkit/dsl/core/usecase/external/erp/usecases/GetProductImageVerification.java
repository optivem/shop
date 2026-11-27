package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductImageResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import static org.assertj.core.api.Assertions.assertThat;

public class GetProductImageVerification extends ResponseVerification<GetProductImageResponse> {
    public GetProductImageVerification(GetProductImageResponse response, UseCaseContext context) {
        super(response, context);
    }

    public GetProductImageVerification contentType(String expectedContentType) {
        var actualContentType = getResponse().getContentType();
        assertThat(actualContentType)
                .withFailMessage("Expected image content type to be '%s', but was '%s'", expectedContentType, actualContentType)
                .isEqualTo(expectedContentType);
        return this;
    }

    public GetProductImageVerification notEmpty() {
        var actualSize = getResponse().getSizeInBytes();
        assertThat(actualSize)
                .withFailMessage("Expected the image to have content, but it was empty")
                .isPositive();
        return this;
    }
}
