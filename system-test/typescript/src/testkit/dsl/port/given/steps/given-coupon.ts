import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenCoupon {
  withCouponCode(couponCode: string): GivenCoupon;
  withDiscountRate(discountRate: string | number): GivenCoupon;
  withValidFrom(validFrom: string): GivenCoupon;
  withValidTo(validTo: string): GivenCoupon;
  withUsageLimit(usageLimit: string | number): GivenCoupon;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
