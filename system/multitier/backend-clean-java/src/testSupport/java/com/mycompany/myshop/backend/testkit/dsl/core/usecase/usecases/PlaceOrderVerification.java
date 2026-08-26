package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderResponse;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;

public class PlaceOrderVerification extends ResponseVerification<PlaceOrderResponse> {

    public PlaceOrderVerification(PlaceOrderResponse response) {
        super(response);
    }

    public String orderNumber() {
        return getResponse().orderNumber();
    }

    public PlaceOrderVerification hasOrderNumber() {
        assertThat(orderNumber()).as("order number").isNotBlank();
        return this;
    }
}
