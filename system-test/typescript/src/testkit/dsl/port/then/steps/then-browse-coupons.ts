import type { ThenStep } from './then-step.js';

export interface ThenBrowseCoupons extends ThenStep<ThenBrowseCoupons> {
  containsCouponWithCode(expectedCode: string): ThenBrowseCoupons;
  couponCount(expectedCount: number): ThenBrowseCoupons;
}
