package com.mycompany.myshop.backend.support.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;

/** What a successful place-order answers with: the order number, and nothing else. */
public class PlaceOrderVerification extends ResponseVerification<PlaceOrderResponse> {

    public PlaceOrderVerification(PlaceOrderResponse response) {
        super(response);
    }

    public String orderNumber() {
        return getResponse().getOrderNumber();
    }

    public PlaceOrderVerification hasOrderNumber() {
        assertThat(orderNumber()).as("order number").isNotBlank();
        return this;
    }
}
