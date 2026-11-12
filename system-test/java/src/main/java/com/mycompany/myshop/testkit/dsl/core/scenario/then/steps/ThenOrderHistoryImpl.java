package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseOrderHistoryVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenOrderHistory;

import java.util.Arrays;

public class ThenOrderHistoryImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenOrderHistory {
    private static final String ALL_CUSTOMERS = "All customers";

    private final BrowseOrderHistoryVerification verification;

    public ThenOrderHistoryImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof BrowseOrderHistoryVerification browseVerification)) {
            throw new IllegalStateException("Cannot verify order history: the executed operation did not browse the order history");
        }
        this.verification = browseVerification;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> containsOrder(String orderNumber) {
        verification.containsOrder(orderNumber);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> doesNotContainOrder(String orderNumber) {
        verification.doesNotContainOrder(orderNumber);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> isEmpty() {
        verification.isEmpty();
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> hasTotalElements(int expectedTotalElements) {
        verification.totalElements(expectedTotalElements);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> showsOrders(String... orderNumbers) {
        verification.ordersInOrder(Arrays.asList(orderNumbers));
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> showsOrderSummary(String orderNumber, String expectedOrderTimestamp,
                                                        String skuAlias, int expectedQuantity,
                                                        String expectedTotalPrice, OrderStatus expectedStatus) {
        verification.showsOrderSummary(orderNumber, expectedOrderTimestamp, skuAlias, expectedQuantity,
                expectedTotalPrice, expectedStatus);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> hasPage(int expectedPage) {
        verification.page(expectedPage);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> hasPageSize(int expectedPageSize) {
        verification.pageSize(expectedPageSize);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> showsOrder(String orderNumber, String customerAlias, String skuAlias,
                                                 int expectedQuantity, OrderStatus expectedStatus) {
        verification.showsOrder(orderNumber, executionResult.customerUsername(customerAlias), skuAlias, expectedQuantity,
                expectedStatus);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> linksOrderToItsDetails(String orderNumber) {
        verification.linksOrderToItsDetails(orderNumber);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> offersCustomers(String... customerAliases) {
        verification.offersCustomers(Arrays.stream(customerAliases).map(executionResult::customerUsername).toList());
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> hasCustomerFilterSetToAllCustomers() {
        verification.customerFilterSetTo(ALL_CUSTOMERS);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> offersNoCustomerFilter() {
        verification.noCustomerFilter();
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> showsNoCustomers() {
        verification.noCustomerColumn();
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> showsMessage(String expectedMessage) {
        verification.message(expectedMessage);
        return this;
    }

    @Override
    public ThenOrderHistoryImpl<R, V> and() {
        return this;
    }
}
