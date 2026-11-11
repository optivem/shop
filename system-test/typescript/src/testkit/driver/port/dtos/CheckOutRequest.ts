import type { ShippingAddress } from './ShippingAddress.js';

/** Opens the checkout by its page address. */
export const OPEN = 'open';
/** Opens the cart and follows its "Proceed to checkout". */
export const PROCEED_FROM_CART = 'proceed-from-cart';

/**
 * How the customer checks out: a fresh browser session first gets the cart lines, each added from its product page
 * in the order given (with `continueSession` the previous call's session is kept instead). Then the checkout is
 * opened, by its address or from the cart; with `shippingAddress` the customer ships to that address instead of the
 * saved one; with `placeOrder` the order is placed.
 */
export interface CheckOutRequest {
  cartLines?: CheckOutRequestLineDto[] | null;
  continueSession?: boolean | null;
  /** OPEN or PROCEED_FROM_CART; unset means OPEN. */
  entry?: string | null;
  /** After proceeding to the login form, signs in as the customer there. */
  signIn?: boolean | null;
  /** Ship to this address instead of the saved one; the country is a code as the form offers it. */
  shippingAddress?: ShippingAddress | null;
  couponCode?: string | null;
  placeOrder?: boolean | null;
}

export interface CheckOutRequestLineDto {
  sku: string;
  quantity: string;
}
