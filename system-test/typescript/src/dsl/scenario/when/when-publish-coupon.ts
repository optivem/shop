import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { ThenPublishCouponResultStage } from '../then/then-publish-coupon.js';

export class WhenPublishCoupon {
  private code: string = '';
  private discountRate: number = 0;
  private validFrom?: string;
  private validTo?: string;
  private usageLimit?: number | string;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withCode(code: string): WhenPublishCoupon {
    this.code = code;
    return this;
  }

  withCouponCode(code: string): WhenPublishCoupon {
    return this.withCode(code);
  }

  withDiscountRate(discountRate: number): WhenPublishCoupon {
    this.discountRate = discountRate;
    return this;
  }

  withValidFrom(validFrom: string | undefined): WhenPublishCoupon {
    this.validFrom = validFrom;
    return this;
  }

  withValidTo(validTo: string | undefined): WhenPublishCoupon {
    this.validTo = validTo;
    return this;
  }

  withUsageLimit(usageLimit: number | string | undefined): WhenPublishCoupon {
    this.usageLimit = usageLimit;
    return this;
  }

  then(): ThenPublishCouponResultStage {
    return new ThenPublishCouponResultStage(this.app, this.ctx, this.useCaseContext, this.code, this.discountRate, this.validFrom, this.validTo, this.usageLimit);
  }
}
