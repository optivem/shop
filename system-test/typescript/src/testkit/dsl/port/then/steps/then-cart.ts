export interface ThenCartResultStage {
  shouldSucceed(): ThenCartSuccess;
}

export interface ThenCartSuccess extends PromiseLike<void> {
  and(): this;
  /** The cart as the visitor's action left it. */
  cart(): ThenCart;
}

/** The cart the visitor's action left. SKUs are aliases; titles, prices and messages are literals as shown. */
export interface ThenCart extends PromiseLike<void> {
  and(): this;
  showsMessage(expectedMessage: string): this;
  hasCartBadge(expectedCount: number): this;
  hasNoCartBadge(): this;
  /** The cart has exactly these lines, in this order; none for an empty cart. */
  hasLines(...skuAliases: string[]): this;
  hasLine(skuAlias: string, expectedTitle: string, expectedUnitPrice: string, expectedQuantity: number, expectedLineTotal: string): this;
  hasLineQuantity(skuAlias: string, expectedQuantity: number): this;
  hasLineTotal(skuAlias: string, expectedLineTotal: string): this;
  hasLinePrices(skuAlias: string, expectedUnitPrice: string, expectedLineTotal: string): this;
  /** The number of items (lines) the cart shows. */
  hasItemCount(expectedCount: number): this;
  hasSubtotal(expectedSubtotal: string): this;
  saysTaxAndDiscountsAreCalculatedAtCheckout(): this;
  offersStartShopping(): this;
  didNotAskToSignIn(): this;
}
