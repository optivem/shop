import { UseCaseContext } from '../../shared/use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { ThenPublishCouponResultStage } from '../then/then-publish-coupon.js';

export class WhenPublishCoupon {
  private code: string = '';
  private discountRate: number | string = 0;
  private validFrom?: string;
  private validTo?: string;
  private usageLimit?: number | string;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withCode(code: string): this {
    this.code = code;
    return this;
  }

  withCouponCode(code: string): this {
    return this.withCode(code);
  }

  withDiscountRate(discountRate: number | string): this {
    this.discountRate = discountRate;
    return this;
  }

  withValidFrom(validFrom: string | null | undefined): this {
    this.validFrom = validFrom ?? undefined;
    return this;
  }

  withValidTo(validTo: string | null | undefined): this {
    this.validTo = validTo ?? undefined;
    return this;
  }

  withUsageLimit(usageLimit: number | string | null | undefined): this {
    this.usageLimit = usageLimit ?? undefined;
    return this;
  }

  then(): ThenPublishCouponResultStage {
    return new ThenPublishCouponResultStage(this.app, this.ctx, this.useCaseContext, this.code, this.discountRate, this.validFrom, this.validTo, this.usageLimit);
  }
}
