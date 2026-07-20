package com.mycompany.myshop.backend.support.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;

public class BrowseOrderHistoryVerification
        extends ResponseVerification<BrowseOrderHistoryResponse> {

    public BrowseOrderHistoryVerification(BrowseOrderHistoryResponse response) {
        super(response);
    }

    public BrowseOrderHistoryVerification hasOrderWithNumber(String expectedOrderNumber) {
        assertThat(getResponse().getOrders())
            .as("order history")
            .extracting(BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse::getOrderNumber)
            .contains(expectedOrderNumber);
        return this;
    }
}
