export interface ThenCustomerList extends PromiseLike<void> {
  and(): this;
  /**
   * Each of these customers (aliases) is listed once, in this order. Every customer who has ever ordered on the
   * shared stack is listed too, so others may come before, between or after them.
   */
  listsCustomersInOrder(...customerAliases: string[]): this;
}
