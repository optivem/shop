import type { GivenStep } from './given-step.js';

export interface GivenCoupon extends GivenStep {
  withCouponCode(couponCode: string): GivenCoupon;
  withDiscountRate(discountRate: string | number): GivenCoupon;
  withValidFrom(validFrom: string): GivenCoupon;
  withValidTo(validTo: string): GivenCoupon;
  withUsageLimit(usageLimit: string | number): GivenCoupon;
}
