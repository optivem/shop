package com.optivem.eshop.dsl.core.usecase.shop.usecases;

import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;

import static org.assertj.core.api.Assertions.assertThat;

public class PlaceOrderVerification extends ResponseVerification<PlaceOrderResponse> {
    public PlaceOrderVerification(PlaceOrderResponse response, UseCaseContext context) {
        super(response, context);
    }

    public PlaceOrderVerification orderNumber(String orderNumberResultAlias) {
        var expectedOrderNumber = getContext().getResultValue(orderNumberResultAlias);
        var actualOrderNumber = getResponse().getOrderNumber();
        assertThat(actualOrderNumber)
                .withFailMessage("Expected order number to be '%s', but was '%s'", expectedOrderNumber, actualOrderNumber)
                .isEqualTo(expectedOrderNumber);
        return this;
    }

    public PlaceOrderVerification orderNumberStartsWith(String prefix) {
        var actualOrderNumber = getResponse().getOrderNumber();
        assertThat(actualOrderNumber)
                .withFailMessage("Expected order number to start with '%s', but was '%s'", prefix, actualOrderNumber)
                .startsWith(prefix);
        return this;
    }
}



