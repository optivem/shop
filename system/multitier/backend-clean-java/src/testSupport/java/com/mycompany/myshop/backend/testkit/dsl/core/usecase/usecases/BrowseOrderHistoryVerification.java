package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryItemResponse;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;

public class BrowseOrderHistoryVerification
        extends ResponseVerification<BrowseOrderHistoryResponse> {

    public BrowseOrderHistoryVerification(BrowseOrderHistoryResponse response) {
        super(response);
    }

    public BrowseOrderHistoryVerification hasOrderWithNumber(String expectedOrderNumber) {
        assertThat(getResponse().orders())
            .as("order history")
            .extracting(BrowseOrderHistoryItemResponse::orderNumber)
            .contains(expectedOrderNumber);
        return this;
    }
}
