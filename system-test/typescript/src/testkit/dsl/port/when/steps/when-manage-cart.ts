import type { ThenCartResultStage } from '../../then/steps/then-cart.js';

/**
 * The visitor manages the cart. The steps run in this order: put the cart lines in the cart (each from its product
 * page, in the order given); change the ERP; then the visitor's one action, which is viewing the cart unless another
 * is given. The cart is read last. SKUs are aliases.
 */
export interface WhenManageCart {
  /** Before the visitor acts, the cart already contains this line. */
  withCartContaining(skuAlias: string, quantity: number): this;
  /** After the cart is filled, the ERP's price of this product changes. */
  withErpPriceChanged(skuAlias: string, unitPrice: string): this;
  /** After the cart is filled, the ERP becomes unavailable. */
  withErpUnavailable(): this;
  /** After the cart is filled, the visitor views it while the ERP is unavailable; then the ERP is available again. */
  withCartViewedWhileErpUnavailable(): this;
  withCatalogBrowsed(): this;
  /** Adds the product with its card's "Add to cart" in the catalog (quantity 1). */
  withAddedFromCatalog(skuAlias: string): this;
  withAddedFromProductPage(skuAlias: string, quantity: string): this;
  /** Types this quantity into the product's cart line. */
  withQuantityChanged(skuAlias: string, quantity: string): this;
  withQuantityIncreased(skuAlias: string): this;
  withQuantityDecreased(skuAlias: string): this;
  withLineRemoved(skuAlias: string): this;
  /** Leaves the site and comes back later in the same browser. */
  withSiteLeftAndRevisited(): this;
  withSignedInAsCustomer(): this;
  then(): ThenCartResultStage;
}
