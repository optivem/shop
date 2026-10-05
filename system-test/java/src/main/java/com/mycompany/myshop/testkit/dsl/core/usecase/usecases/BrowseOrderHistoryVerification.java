package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BrowseOrderHistoryVerification extends ResponseVerification<BrowseOrderHistoryResponse> {
    public BrowseOrderHistoryVerification(BrowseOrderHistoryResponse response, UseCaseContext context) {
        super(response, context);
    }

    public BrowseOrderHistoryVerification containsOrder(String orderNumberResultAlias) {
        var orderNumber = getContext().getResultValue(orderNumberResultAlias);
        assertThat(listedOrderNumbers())
                .as("Order history should contain order '%s'", orderNumber)
                .contains(orderNumber);
        return this;
    }

    public BrowseOrderHistoryVerification doesNotContainOrder(String orderNumberResultAlias) {
        var orderNumber = getContext().getResultValue(orderNumberResultAlias);
        assertThat(listedOrderNumbers())
                .as("Order history should not contain order '%s'", orderNumber)
                .doesNotContain(orderNumber);
        return this;
    }

    private List<String> listedOrderNumbers() {
        assertThat(getResponse())
                .as("Response should not be null")
                .isNotNull();
        assertThat(getResponse().getOrders())
                .as("Orders list should not be null")
                .isNotNull();

        return getResponse().getOrders().stream()
                .map(BrowseOrderHistoryResponse.OrderDto::getOrderNumber)
                .toList();
    }
}
