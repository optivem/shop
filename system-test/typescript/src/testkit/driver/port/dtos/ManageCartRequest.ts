/** Opens the cart. */
export const VIEW_CART = 'view-cart';
/** Opens the catalog. */
export const BROWSE_CATALOG = 'browse-catalog';
/** Adds `sku` with its card's "Add to cart" in the catalog. */
export const ADD_FROM_CATALOG = 'add-from-catalog';
/** Adds `quantity` of `sku` from its product page. */
export const ADD_FROM_PRODUCT_PAGE = 'add-from-product-page';
/** Types `quantity` into the cart line of `sku`. */
export const CHANGE_QUANTITY = 'change-quantity';
/** Raises the cart line of `sku` by 1 with the stepper. */
export const INCREASE_QUANTITY = 'increase-quantity';
/** Lowers the cart line of `sku` by 1 with the stepper. */
export const DECREASE_QUANTITY = 'decrease-quantity';
/** Removes the cart line of `sku`. */
export const REMOVE_LINE = 'remove-line';
/** Leaves the site and comes back in the same browser. */
export const REVISIT = 'revisit';
/** Signs in as the customer from the header. */
export const SIGN_IN = 'sign-in';

/**
 * How the visitor manages the cart: a fresh browser session first gets the cart lines, each added from its product
 * page in the order given; then the action runs. With `continueSession` the previous call's browser session is kept
 * instead, so only the action runs. Without an action, the call only fills the cart.
 */
export interface ManageCartRequest {
  cartLines?: ManageCartRequestLineDto[] | null;
  continueSession?: boolean | null;
  /** One of the actions above. */
  action?: string | null;
  sku?: string | null;
  quantity?: string | null;
}

export interface ManageCartRequestLineDto {
  sku: string;
  quantity: string;
}
