export interface ThenOrderHistory extends PromiseLike<void> {
  and(): this;
  containsOrder(orderNumber: string): this;
  doesNotContainOrder(orderNumber: string): this;
}
