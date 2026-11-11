export interface ThenProductPageResultStage {
  shouldSucceed(): ThenProductPageSuccess;
  shouldFail(): ThenProductPageFailure;
}

export interface ThenProductPageSuccess extends PromiseLike<void> {
  and(): this;
  /** The product page the executed view showed. */
  productPage(): ThenProductPage;
}

export interface ThenProductPageFailure extends PromiseLike<void> {
  and(): this;
  errorMessage(expectedMessage: string): this;
}

/** The product page the executed view showed. SKUs are aliases; titles, prices and messages are literals as shown. */
export interface ThenProductPage extends PromiseLike<void> {
  and(): this;
  hasTitle(expectedTitle: string): this;
  hasBrand(expectedBrand: string): this;
  hasCategory(expectedCategory: string): this;
  hasSku(expectedSkuAlias: string): this;
  hasPrice(expectedPrice: string): this;
  hasDescription(expectedDescription: string): this;
  /** The breadcrumb trail, in order. */
  hasBreadcrumbs(...expectedBreadcrumbs: string[]): this;
  hasQuantity(expectedQuantity: number): this;
  showsMessage(expectedMessage: string): this;
  offersBackToCatalog(): this;
  doesNotOfferAddToCart(): this;
  hasCartBadge(expectedCount: number): this;
  isOnPageOfProduct(skuAlias: string): this;
  /** The visitor's cart has exactly this line. */
  cartContains(skuAlias: string, expectedQuantity: number): this;
  cartIsEmpty(): this;
  /** After following the link back, the catalog shows exactly these products, in this order. */
  catalogShowsProducts(...skuAliases: string[]): this;
  /** The rest of the site keeps working: the acting user can still open their order history. */
  canOpenMyOrders(): this;
}
