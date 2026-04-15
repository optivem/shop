import type { WhenStep } from './when-step.js';

export interface WhenPublishCoupon extends WhenStep {
  withCouponCode(couponCode: string): WhenPublishCoupon;
  withDiscountRate(discountRate: string | number): WhenPublishCoupon;
  withValidFrom(validFrom: string): WhenPublishCoupon;
  withValidTo(validTo: string): WhenPublishCoupon;
  withUsageLimit(usageLimit: string | number): WhenPublishCoupon;
}
