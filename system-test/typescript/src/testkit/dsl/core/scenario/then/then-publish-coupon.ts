import { expect } from '@playwright/test';
import { ErrorResponse, BrowseCouponItem } from '../../../../common/dtos.js';
import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';

export class ThenPublishCouponResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private _errorAssertions: ((error: ErrorResponse, useCaseContext: UseCaseContext) => void)[] = [];
  private readonly _couponAssertions: { code: string; fns: ((coupon: BrowseCouponItem) => void)[] }[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly code: string,
    private readonly discountRate: number,
    private readonly validFrom?: string,
    private readonly validTo?: string,
    private readonly usageLimit?: number | string,
  ) {}

  shouldSucceed(): ThenPublishCouponSuccess {
    this._expectSuccess = true;
    return new ThenPublishCouponSuccess(this);
  }

  shouldFail(): ThenPublishCouponFailure {
    this._expectSuccess = false;
    return new ThenPublishCouponFailure(this);
  }

  _addErrorAssertion(fn: (error: ErrorResponse, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  _addCouponAssertion(code: string, fn: (coupon: BrowseCouponItem) => void): void {
    let entry = this._couponAssertions.find((e) => e.code === code);
    if (!entry) {
      entry = { code, fns: [] };
      this._couponAssertions.push(entry);
    }
    entry.fns.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    // Set up given coupons first (for duplicate tests)
    for (const cc of this.ctx.couponConfigs) {
      const resolvedCode = this.useCaseContext.getParamValue(cc.code) as string;
      await this.app.shop().publishCoupon({
        code: resolvedCode,
        discountRate: cc.discountRate,
        validFrom: cc.validFrom,
        validTo: cc.validTo,
        usageLimit: cc.usageLimit,
      });
    }

    const resolvedCode = this.useCaseContext.getParamValue(this.code) as string;
    const result = await this.app.shop('static').publishCoupon({
      code: resolvedCode,
      discountRate: this.discountRate,
      validFrom: this.validFrom,
      validTo: this.validTo,
      usageLimit: this.usageLimit,
    });

    if (this._expectSuccess) {
      expect(result.success).toBe(true);

      for (const couponEntry of this._couponAssertions) {
        const resolvedCouponCode = this.useCaseContext.getParamValue(couponEntry.code) as string;
        const browseResult = await this.app.shop().browseCoupons();
        expect(browseResult.success).toBe(true);
        if (browseResult.success) {
          const coupon = browseResult.value.coupons.find((c) => c.code === resolvedCouponCode);
          expect(coupon, `Coupon '${resolvedCouponCode}' not found in browse results`).toBeDefined();
          if (coupon) {
            for (const fn of couponEntry.fns) fn(coupon);
          }
        }
      }
    } else {
      expect(result.success).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      }
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenPublishCouponSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenPublishCouponResultStage) {}

  and(): this {
    return this;
  }

  coupon(code: string): ThenPublishCouponCoupon {
    return new ThenPublishCouponCoupon(this.stage, code);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenPublishCouponCoupon implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenPublishCouponResultStage,
    private readonly code: string,
  ) {}

  hasDiscountRate(rate: number): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.discountRate).toBe(rate);
    });
    return this;
  }

  isValidFrom(validFrom: string): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(new Date(coupon.validFrom!).getTime()).toBe(new Date(validFrom).getTime());
    });
    return this;
  }

  isValidTo(validTo: string): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(new Date(coupon.validTo!).getTime()).toBe(new Date(validTo).getTime());
    });
    return this;
  }

  hasUsageLimit(limit: number): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.usageLimit).toBe(limit);
    });
    return this;
  }

  hasUsedCount(count: number): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.usedCount).toBe(count);
    });
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenPublishCouponFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenPublishCouponResultStage) {}

  and(): this {
    return this;
  }

  errorMessage(expected: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      expect(error.message).toBe(useCaseContext.expandAliases(expected));
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      const expandedMessage = useCaseContext.expandAliases(message);
      const fieldError = error.fieldErrors.find((fe) => fe.field === field);
      expect(fieldError).toBeDefined();
      expect(fieldError!.message).toBe(expandedMessage);
    });
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}
