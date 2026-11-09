export interface ThenCoupon extends PromiseLike<void> {
  and(): this;
  hasDiscountRate(discountRate: number): this;
  isValidFrom(validFrom: string): this;
  isValidTo(validTo: string): this;
  hasUsageLimit(usageLimit: number): this;
  /** The coupon has no valid-from time, so it is valid immediately. */
  isValidImmediately(): this;
  /** The coupon has no valid-to time, so it never expires. */
  neverExpires(): this;
  /** The coupon has no usage limit. */
  hasUnlimitedUsage(): this;
  hasUsedCount(expectedUsedCount: number): this;
}
