package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrderHistory extends ThenStep<ThenOrderHistory> {
    ThenOrderHistory containsOrder(String orderNumber);

    ThenOrderHistory doesNotContainOrder(String orderNumber);

    ThenOrderHistory isEmpty();

    ThenOrderHistory hasTotalElements(int expectedTotalElements);

    /** The list shows exactly these orders (aliases), in this order; none means the list is empty. */
    ThenOrderHistory showsOrders(String... orderNumbers);

    /** The listed order's summary: order date (ISO-8601), SKU (an alias), quantity, total price and status. */
    ThenOrderHistory showsOrderSummary(String orderNumber, String expectedOrderTimestamp, String skuAlias,
                                       int expectedQuantity, String expectedTotalPrice, OrderStatus expectedStatus);

    /** The page number the list shows (API only). */
    ThenOrderHistory hasPage(int expectedPage);

    /** The page size the list uses (API only). */
    ThenOrderHistory hasPageSize(int expectedPageSize);

    /** The listed order shows who placed it (a customer alias), its SKU (an alias), quantity and status. */
    ThenOrderHistory showsOrder(String orderNumber, String customerAlias, String skuAlias, int expectedQuantity,
                                OrderStatus expectedStatus);

    /** The listed order links to its details page (UI only). */
    ThenOrderHistory linksOrderToItsDetails(String orderNumber);

    /** The customer filter offers All customers and these customers (aliases), among the others who ordered (UI only). */
    ThenOrderHistory offersCustomers(String... customerAliases);

    /** The customer filter is set to All customers (UI only). */
    ThenOrderHistory hasCustomerFilterSetToAllCustomers();

    /** There is no customer filter (UI only). */
    ThenOrderHistory offersNoCustomerFilter();

    /** The list has no Customer column (UI only). */
    ThenOrderHistory showsNoCustomers();

    /** The list shows this message instead of orders, such as "No orders found" (UI only). */
    ThenOrderHistory showsMessage(String expectedMessage);
}
