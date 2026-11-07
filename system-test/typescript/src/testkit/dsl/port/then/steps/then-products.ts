export interface ThenProducts extends PromiseLike<void> {
  /** Asserts the ERP product list contains the SKU at the price; other listed products are ignored. */
  containsProduct(sku: string, price: number | string): this;
}
