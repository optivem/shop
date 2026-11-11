import type { ThenCheckoutResultStage } from '../../then/steps/then-checkout.js';

/**
 * The customer checks out. The steps run in this order: put the cart lines in the cart (each from its product page,
 * in the order given); change the ERP; open the checkout (by its address, unless proceeding from the cart); enter the
 * shipping address and coupon; place the order when asked. SKUs and coupon codes are aliases; the shipping address
 * is literal, its country a code as the checkout's form offers it.
 */
export interface WhenCheckOut {
  /** Before the checkout, the cart already contains this line. */
  withCartContaining(skuAlias: string, quantity: number): this;
  /** After the cart is filled, the ERP's price of this product changes. */
  withErpPriceChanged(skuAlias: string, unitPrice: string): this;
  /** After the cart is filled, the ERP no longer offers this product. */
  withErpNoLongerOffering(skuAlias: string): this;
  /** Proceeds to checkout from the cart instead of opening the checkout by its address. */
  withProceededFromCart(): this;
  /** Proceeds to checkout from the cart, and signs in as a customer when asked to. */
  withProceededFromCartAndSignedIn(): this;
  /** Chooses to ship to this address instead of the saved one (the form is open anyway on a first order). */
  withShippingAddress(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this;
  withCouponCode(couponCode: string): this;
  /** Places the order; without this, the checkout is only opened (and the address entered). */
  withOrderPlaced(): this;
  then(): ThenCheckoutResultStage;
}
