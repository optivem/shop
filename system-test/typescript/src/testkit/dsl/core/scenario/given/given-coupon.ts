import { CouponConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenCoupon as IGivenCoupon } from '../../../port/given/steps/given-coupon.js';

export class GivenCoupon implements IGivenCoupon {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: CouponConfig,
  ) {}

  withCode(code: string): this {
    this.config.code = code;
    return this;
  }

  withCouponCode(code: string): this {
    return this.withCode(code);
  }

  withDiscountRate(discountRate: number): this {
    this.config.discountRate = discountRate;
    return this;
  }

  withValidFrom(validFrom: string): this {
    this.config.validFrom = validFrom;
    return this;
  }

  withValidTo(validTo: string): this {
    this.config.validTo = validTo;
    return this;
  }

  withUsageLimit(usageLimit: number | string): this {
    this.config.usageLimit = usageLimit;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage {
    return this.stage.then();
  }
}
