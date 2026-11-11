/** Raises the quantity by 1 with the stepper. */
export const INCREASE = 'increase';
/** Lowers the quantity by 1 with the stepper. */
export const DECREASE = 'decrease';
/** Follows the link back to the catalog. */
export const BACK_TO_CATALOG = 'back-to-catalog';

/**
 * How the visitor views a product, applied in this order: put the cart line in the cart, open the product (by its
 * page address, or from the catalog), type a quantity, use the stepper, add to the cart; then the follow-up, if any.
 * Unset inputs are left untouched. Only the SKU reaches the API: the rest are UI controls.
 */
export interface ViewProductRequest {
  sku: string;
  /** Opens the product from its card in the catalog instead of by its page address. */
  fromCatalog?: boolean | null;
  /** A product already in the cart before the visitor views this one, with cartQuantity. */
  cartSku?: string | null;
  cartQuantity?: string | null;
  /** The quantity typed into the quantity field. */
  quantity?: string | null;
  /** INCREASE or DECREASE. */
  quantityStep?: string | null;
  /** Adds the product to the cart with the quantity in the field. */
  addToCart?: boolean | null;
  /** BACK_TO_CATALOG, run after the other inputs. */
  followUp?: string | null;
}
