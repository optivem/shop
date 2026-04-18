import { expect } from '@playwright/test';
import type {
  BrowseCouponsResponse,
  BrowseCouponItem,
} from '../../../../../driver/port/shop/dtos/BrowseCouponsResponse.js';
import { ResponseVerification } from '../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';

export class BrowseCouponsVerification extends ResponseVerification<BrowseCouponsResponse> {
  constructor(response: BrowseCouponsResponse, context: UseCaseContext) {
    super(response, context);
  }

  hasCouponWithCode(couponCodeAlias: string): this {
    this.findCouponByCode(couponCodeAlias);
    return this;
  }

  couponHasDiscountRate(couponCodeAlias: string, expectedDiscountRate: number): this {
    const coupon = this.findCouponByCode(couponCodeAlias);
    expect(coupon.discountRate).toBe(expectedDiscountRate);
    return this;
  }

  couponHasValidFrom(couponCodeAlias: string, expectedValidFrom: string): this {
    const coupon = this.findCouponByCode(couponCodeAlias);
    expect(coupon.validFrom).toBe(expectedValidFrom);
    return this;
  }

  couponHasValidTo(couponCodeAlias: string, expectedValidTo: string): this {
    const coupon = this.findCouponByCode(couponCodeAlias);
    expect(coupon.validTo).toBe(expectedValidTo);
    return this;
  }

  couponHasUsageLimit(couponCodeAlias: string, expectedUsageLimit: number): this {
    const coupon = this.findCouponByCode(couponCodeAlias);
    expect(coupon.usageLimit).toBe(expectedUsageLimit);
    return this;
  }

  couponHasUsedCount(couponCodeAlias: string, expectedUsedCount: number): this {
    const coupon = this.findCouponByCode(couponCodeAlias);
    expect(coupon.usedCount).toBe(expectedUsedCount);
    return this;
  }

  private findCouponByCode(couponCodeAlias: string): BrowseCouponItem {
    expect(couponCodeAlias).not.toBeNull();
    expect(this.getResponse()).not.toBeNull();
    expect(this.getResponse().coupons).not.toBeNull();

    const couponCode = this.getContext().getParamValue(couponCodeAlias);

    const coupon = this.getResponse().coupons.find((c) => c.code === couponCode);
    if (!coupon) {
      const available = this.getResponse().coupons.map((c) => c.code);
      throw new Error(
        `Coupon with code '${couponCode}' not found. Available coupons: ${JSON.stringify(available)}`,
      );
    }
    return coupon;
  }
}
