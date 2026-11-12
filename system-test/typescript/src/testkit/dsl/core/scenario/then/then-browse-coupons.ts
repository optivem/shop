import { expect } from '@playwright/test';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { BrowseCouponsResponse } from '../../../../driver/port/dtos/BrowseCouponsResponse.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { arrangeGiven } from '../given/arrange-given.js';

export class ThenBrowseCouponsResultStage implements PromiseLike<void> {
  private _executionPromise: Promise<void> | null = null;
  private _browseResult: BrowseCouponsResponse | null = null;
  private _expectSuccess = true;
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private readonly _couponsAssertions: ((coupons: BrowseCouponsResponse, useCaseContext: UseCaseContext) => void)[] = [];

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly page: string | null = null,
    private readonly size: string | null = null,
  ) {}

  shouldSucceed(): ThenBrowseCouponsSuccess {
    this._expectSuccess = true;
    return new ThenBrowseCouponsSuccess(this);
  }

  shouldFail(): ThenBrowseCouponsFailure {
    this._expectSuccess = false;
    return new ThenBrowseCouponsFailure(this);
  }

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  _addCouponsAssertion(fn: (coupons: BrowseCouponsResponse, useCaseContext: UseCaseContext) => void): void {
    this._couponsAssertions.push(fn);
  }

  async _getResult(): Promise<BrowseCouponsResponse> {
    await this._execute();
    return this._browseResult!;
  }

  private async _execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    // The declared coupons (with their validity and usage limit) and any given orders, which may use them.
    await arrangeGiven(this.app, this.ctx, this.useCaseContext, { declaredOnly: true });

    const result = await this.app.myShop('dynamic').browseCoupons({ page: this.page, size: this.size });
    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (result.success) {
        this._browseResult = result.value;
        for (const fn of this._couponsAssertions) fn(result.value, this.useCaseContext);
      }
    } else {
      expect(result.success, JSON.stringify(result)).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      }
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this._execute().then(onfulfilled, onrejected);
  }
}

export class ThenBrowseCouponsSuccess implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenBrowseCouponsResultStage,
  ) {}

  and(): this {
    return this;
  }

  /** The coupon list the executed browse showed. */
  couponList(): ThenBrowseCouponsCouponList {
    return new ThenBrowseCouponsCouponList(this.stage);
  }

  /** The coupon known in this scenario by this alias, as the executed browse listed it. */
  coupon(couponCode: string): ThenBrowseCouponsCoupon {
    return new ThenBrowseCouponsCoupon(this.stage, couponCode);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenBrowseCouponsCouponList implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseCouponsResultStage) {}

  and(): this {
    return this;
  }

  /**
   * The list begins with these coupons (aliases), in this order. The coupons table is shared, so coupons published
   * before the scenario may follow them; a scenario that relies on its coupons being the newest must run isolated.
   */
  startsWithCoupons(...couponCodes: string[]): this {
    this.stage._addCouponsAssertion((response, useCaseContext) => {
      const expected = couponCodes.map((code) => useCaseContext.getParamValue(code));
      const listed = response.coupons.map((coupon) => coupon.code);
      expect(listed.slice(0, expected.length), `Coupons listed first, in order; listed: ${JSON.stringify(listed)}`).toEqual(expected);
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

export class ThenBrowseCouponsCoupon implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenBrowseCouponsResultStage,
    private readonly couponCode: string,
  ) {}

  and(): this {
    return this;
  }

  hasUsedCount(expectedUsedCount: number): this {
    this.stage._addCouponsAssertion((response, useCaseContext) => {
      const code = useCaseContext.getParamValue(this.couponCode);
      const coupon = response.coupons.find((listed) => listed.code === code);
      expect(coupon, `Coupon '${code}' should be listed`).toBeDefined();
      expect(coupon?.usedCount, `Used count for coupon '${code}'`).toBe(expectedUsedCount);
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

export class ThenBrowseCouponsFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseCouponsResultStage) {}

  errorMessage(expected: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      expect(error.message).toBe(useCaseContext.expandAliases(expected));
    });
    return this;
  }

  noOrderPersisted(): this {
    this.stage._addErrorAssertion((error) => {
      expect(error, 'Expected the rejected operation to yield an error response (nothing persisted)').toBeDefined();
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      const expandedMessage = useCaseContext.expandAliases(message);
      const fieldError = error.fieldErrors.find((fe) => fe.field === field);
      expect(fieldError?.message, `Expected field error for '${field}' in ${JSON.stringify(error.fieldErrors)}`).toBe(expandedMessage);
    });
    return this;
  }

  and(): this {
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}
