export interface ThenOrder extends PromiseLike<void> {
  and(): this;
  hasSku(expectedSku: string): this;
  hasQuantity(expectedQuantity: number): this;
  hasUnitPrice(expectedUnitPrice: number): this;
  hasBasePrice(basePrice: string | number): this;
  hasSubtotalPrice(subtotalPrice: string | number): this;
  hasTotalPrice(totalPrice: string | number): this;
  hasStatus(expectedStatus: string): this;
  hasTotalPriceGreaterThanZero(): this;
  hasOrderNumberPrefix(expectedPrefix: string): this;
  hasDiscountRate(expectedDiscountRate: number): this;
  hasDiscountAmount(expectedDiscountAmount: string | number): this;
  hasAppliedCoupon(expectedCouponCode?: string | null): this;
  hasTaxRate(expectedTaxRate: string | number): this;
  hasTaxAmount(expectedTaxAmount: string | number): this;
}
