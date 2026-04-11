import type { ThenStep } from './then-step.js';

export interface ThenCoupon extends ThenStep<ThenCoupon> {
  hasDiscountRate(discountRate: number): ThenCoupon;
  isValidFrom(validFrom: string): ThenCoupon;
  isValidTo(validTo: string): ThenCoupon;
  hasUsageLimit(usageLimit: number): ThenCoupon;
  hasUsedCount(expectedUsedCount: number): ThenCoupon;
}
