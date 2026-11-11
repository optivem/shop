export interface ThenCheckoutResultStage {
  shouldSucceed(): ThenCheckoutSuccess;
  shouldFail(): ThenCheckoutFailure;
}

export interface ThenCheckoutSuccess extends PromiseLike<void> {
  and(): this;
  /** Where the checkout left the customer. */
  checkout(): ThenCheckout;
  /** The order the checkout placed for this product (an alias), as the system shows it now. */
  orderFor(skuAlias: string): ThenCheckedOutOrder;
  /** The customer's profile, as saved now. */
  profile(): ThenCheckedOutProfile;
  /** The coupon (an alias), as an admin browses it now. */
  coupon(couponCode: string): ThenCheckedOutCoupon;
}

export interface ThenCheckoutFailure extends PromiseLike<void> {
  and(): this;
  errorMessage(expectedMessage: string): this;
}

/**
 * Where the checkout left the customer: the checkout, the order confirmation or the cart. SKUs are aliases; messages
 * may name SKU and coupon aliases; prices, tax rates and addresses are literals as shown.
 */
export interface ThenCheckout extends PromiseLike<void> {
  and(): ThenCheckoutSuccess;
  /** The customer is still on (or was shown) the checkout. */
  isShown(): this;
  showsTheCart(): this;
  /** The checkout's order review has exactly these lines, in this order. */
  hasReviewLines(...skuAliases: string[]): this;
  hasReviewLine(skuAlias: string, expectedQuantity: number, expectedLineTotal: string): this;
  hasReviewQuantity(skuAlias: string, expectedQuantity: number): this;
  hasReviewSubtotal(expectedSubtotal: string): this;
  /** The order review's tax country and estimated tax rate (e.g. "19%") when the checkout was opened. */
  hadTaxWhenOpened(expectedCountry: string, expectedRate: string): this;
  /** The order review's tax country and estimated tax rate (e.g. "19%") as the checkout ended. */
  hasTax(expectedCountry: string, expectedRate: string): this;
  saysDiscountAndTaxAreWorkedOutPerOrder(): this;
  /** The saved address, as the checkout shows it, is selected. */
  hasSavedAddressSelected(expectedAddress: string): this;
  showsMessage(expectedMessage: string): this;
  /** One of the lines under the checkout's error message, such as "City is required". */
  showsMessageDetail(expectedDetail: string): this;
  /** The checkout tells the customer this line was rejected ("SKU: reason" under its message). */
  saysRejected(skuAlias: string, expectedReason: string): this;
  thanksForTheOrder(): this;
  /** The confirmation lists exactly one order for each of these products, in this order. */
  confirmsOrdersFor(...skuAliases: string[]): this;
  confirmsNoOrder(): this;
  linksEachOrderToItsDetails(): this;
  offersViewMyOrdersAndContinueShopping(): this;
  /** The confirmation shows this line was rejected, for this reason. */
  confirmsRejected(skuAlias: string, expectedReason: string): this;
  confirmsNothingRejected(): this;
  /** After the order was placed, the cart has exactly these lines, in this order; none for an empty cart. */
  hasCartLines(...skuAliases: string[]): this;
  hasCartLineQuantity(skuAlias: string, expectedQuantity: number): this;
}

/** An order the checkout placed. Prices and rates are literals; the coupon code is an alias. */
export interface ThenCheckedOutOrder extends PromiseLike<void> {
  and(): ThenCheckoutSuccess;
  hasQuantity(expectedQuantity: number): this;
  hasUnitPrice(expectedUnitPrice: string | number): this;
  hasTaxRate(expectedTaxRate: string | number): this;
  hasDiscountRate(expectedDiscountRate: string | number): this;
  hasDiscountAmount(expectedDiscountAmount: string | number): this;
  hasAppliedCoupon(expectedCouponCode?: string | null): this;
  /** The country as a code, such as "DE", not an alias: checkout offers only real shipping countries. */
  hasCountryCode(expectedCountry: string): this;
  /** The shipping address, with its country as a code, not an alias. */
  hasShippingAddressWithCountryCode(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this;
}

/** Profile values are literals (the profile accepts only US, DE or GB). */
export interface ThenCheckedOutProfile extends PromiseLike<void> {
  and(): ThenCheckoutSuccess;
  hasFirstName(expected: string): this;
  hasLastName(expected: string): this;
  hasStreet(expected: string): this;
  hasPostalCode(expected: string): this;
  hasCity(expected: string): this;
  hasCountry(expected: string): this;
}

export interface ThenCheckedOutCoupon extends PromiseLike<void> {
  and(): ThenCheckoutSuccess;
  hasUsedCount(expectedUsedCount: number): this;
}
