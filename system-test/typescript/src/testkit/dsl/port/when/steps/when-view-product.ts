import type { ThenProductPageResultStage } from '../../then/steps/then-product-page.js';

/**
 * Views a product as a visitor (or as the logged-in user). The steps run in this order: put the cart line in the
 * cart, open the product (by its page address, or from the catalog), type a quantity, use the stepper, add to the
 * cart; then follow the link back to the catalog. SKUs are aliases.
 */
export interface WhenViewProduct {
  withSku(skuAlias: string): this;
  /** Opens the product from its card in the catalog instead of by its page address. */
  fromCatalog(): this;
  /** Before viewing the product, the visitor's cart already contains this line. */
  withCartContaining(skuAlias: string, quantity: number): this;
  /** Types this quantity into the quantity field. */
  withQuantity(quantity: string): this;
  withQuantityIncreased(): this;
  withQuantityDecreased(): this;
  /** Types this quantity and adds the product to the cart. */
  withAddedToCart(quantity: string): this;
  /** After the other steps, follows the link back to the catalog. */
  withBackToCatalogFollowed(): this;
  then(): ThenProductPageResultStage;
}
