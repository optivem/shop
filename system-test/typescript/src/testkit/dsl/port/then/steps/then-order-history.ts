import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';

export interface ThenOrderHistory extends PromiseLike<void> {
  and(): this;
  containsOrder(orderNumber: string): this;
  doesNotContainOrder(orderNumber: string): this;
  isEmpty(): this;
  hasTotalElements(expectedTotalElements: number): this;
  /** The list shows exactly these orders (aliases), in this order; none means the list is empty. */
  showsOrders(...orderNumbers: string[]): this;
  /** The listed order's summary: order date (ISO-8601), SKU (an alias), quantity, total price and status. */
  showsOrderSummary(
    orderNumber: string,
    expectedOrderTimestamp: string,
    skuAlias: string,
    expectedQuantity: number,
    expectedTotalPrice: string,
    expectedStatus: OrderStatus,
  ): this;
  /** The page number the list shows (API only). */
  hasPage(expectedPage: number): this;
  /** The page size the list uses (API only). */
  hasPageSize(expectedPageSize: number): this;
  /** The listed order shows who placed it (a customer alias), its SKU (an alias), quantity and status. */
  showsOrder(orderNumber: string, customerAlias: string, skuAlias: string, expectedQuantity: number, expectedStatus: OrderStatus): this;
  /** The listed order links to its details page (UI only). */
  linksOrderToItsDetails(orderNumber: string): this;
  /** The customer filter offers All customers and these customers (aliases), among the others who ordered (UI only). */
  offersCustomers(...customerAliases: string[]): this;
  /** The customer filter is set to All customers (UI only). */
  hasCustomerFilterSetToAllCustomers(): this;
  /** There is no customer filter (UI only). */
  offersNoCustomerFilter(): this;
  /** The list has no Customer column (UI only). */
  showsNoCustomers(): this;
  /** The list shows this message instead of orders, such as "No orders found" (UI only). */
  showsMessage(expectedMessage: string): this;
}
