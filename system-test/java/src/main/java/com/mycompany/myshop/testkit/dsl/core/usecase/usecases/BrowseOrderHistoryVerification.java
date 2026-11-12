package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public BrowseOrderHistoryVerification isEmpty() {
        assertThat(listedOrderNumbers())
                .as("Order history page should be empty")
                .isEmpty();
        return this;
    }

    public BrowseOrderHistoryVerification totalElements(long expectedTotalElements) {
        assertThat(getResponse().getTotalElements())
                .as("Order history total elements")
                .isEqualTo(expectedTotalElements);
        return this;
    }

    /** The listed orders are exactly these (aliases), in this order. */
    public BrowseOrderHistoryVerification ordersInOrder(List<String> orderNumberResultAliases) {
        var expectedOrderNumbers = orderNumberResultAliases.stream().map(getContext()::getResultValue).toList();
        assertThat(listedOrderNumbers())
                .as("Orders listed, in order")
                .containsExactlyElementsOf(expectedOrderNumbers);
        return this;
    }

    /** The listed order shows this order date, SKU (an alias), quantity, total price and status. */
    public BrowseOrderHistoryVerification showsOrderSummary(String orderNumberResultAlias, String expectedOrderTimestamp,
                                                            String skuAlias, int expectedQuantity,
                                                            String expectedTotalPrice, OrderStatus expectedStatus) {
        var order = listedOrder(orderNumberResultAlias);
        var expectedSku = getContext().getParamValue(skuAlias);
        assertThat(order.getOrderTimestamp()).as("Order date of order '%s'", order.getOrderNumber())
                .isEqualTo(Converter.toInstant(expectedOrderTimestamp));
        assertThat(order.getSku()).as("SKU of order '%s'", order.getOrderNumber()).isEqualTo(expectedSku);
        assertThat(order.getQuantity()).as("Quantity of order '%s'", order.getOrderNumber()).isEqualTo(expectedQuantity);
        assertThat(order.getTotalPrice()).as("Total price of order '%s'", order.getOrderNumber())
                .isEqualByComparingTo(Converter.toBigDecimal(expectedTotalPrice));
        assertThat(order.getStatus()).as("Status of order '%s'", order.getOrderNumber()).isEqualTo(expectedStatus);
        return this;
    }

    public BrowseOrderHistoryVerification page(int expectedPage) {
        assertThat(getResponse().getPage()).as("Order history page").isEqualTo(expectedPage);
        return this;
    }

    public BrowseOrderHistoryVerification pageSize(int expectedPageSize) {
        assertThat(getResponse().getSize()).as("Order history page size").isEqualTo(expectedPageSize);
        return this;
    }

    /** The listed order shows this customer (a username), SKU (an alias), quantity and status. */
    public BrowseOrderHistoryVerification showsOrder(String orderNumberResultAlias, String expectedCustomer,
                                                     String skuAlias, int expectedQuantity, OrderStatus expectedStatus) {
        var order = listedOrder(orderNumberResultAlias);
        var expectedSku = getContext().getParamValue(skuAlias);
        assertThat(order.getCustomer()).as("Customer of order '%s'", order.getOrderNumber()).isEqualTo(expectedCustomer);
        assertThat(order.getSku()).as("SKU of order '%s'", order.getOrderNumber()).isEqualTo(expectedSku);
        assertThat(order.getQuantity()).as("Quantity of order '%s'", order.getOrderNumber()).isEqualTo(expectedQuantity);
        assertThat(order.getStatus()).as("Status of order '%s'", order.getOrderNumber()).isEqualTo(expectedStatus);
        return this;
    }

    /** UI only: the listed order's "View Details" link goes to its details page. */
    public BrowseOrderHistoryVerification linksOrderToItsDetails(String orderNumberResultAlias) {
        var order = listedOrder(orderNumberResultAlias);
        assertThat(order.getDetailsPath())
                .as("Details link of order '%s'", order.getOrderNumber())
                .isNotNull()
                .contains("/order-details")
                .contains(URLEncoder.encode(order.getOrderNumber(), StandardCharsets.UTF_8).replace("+", "%20"));
        return this;
    }

    /** UI only: the customer filter offers All customers and each of these usernames. */
    public BrowseOrderHistoryVerification offersCustomers(List<String> expectedCustomers) {
        assertThat(getResponse().getCustomerFilterOptions())
                .as("Customer filter choices")
                .isNotNull()
                .contains("All customers")
                .containsAll(expectedCustomers);
        return this;
    }

    /** UI only: the customer filter's chosen option, as shown. */
    public BrowseOrderHistoryVerification customerFilterSetTo(String expectedSelection) {
        assertThat(getResponse().getCustomerFilterSelection()).as("Customer filter choice").isEqualTo(expectedSelection);
        return this;
    }

    /** UI only: there is no customer filter. */
    public BrowseOrderHistoryVerification noCustomerFilter() {
        assertThat(getResponse().getCustomerFilterOptions()).as("Customer filter choices").isNull();
        return this;
    }

    /** UI only: the list has no Customer column. */
    public BrowseOrderHistoryVerification noCustomerColumn() {
        assertThat(getResponse().getCustomerColumnShown()).as("Customer column shown").isFalse();
        return this;
    }

    /** UI only: the message the list shows instead of orders. */
    public BrowseOrderHistoryVerification message(String expectedMessage) {
        assertThat(getResponse().getMessage()).as("Order list message").isEqualTo(expectedMessage);
        return this;
    }

    private BrowseOrderHistoryResponse.OrderDto listedOrder(String orderNumberResultAlias) {
        var orderNumber = getContext().getResultValue(orderNumberResultAlias);
        return getResponse().getOrders().stream()
                .filter(order -> orderNumber.equals(order.getOrderNumber()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Order history should list order '" + orderNumber + "', but lists "
                        + listedOrderNumbers()));
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
