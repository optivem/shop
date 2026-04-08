import { expect } from '@playwright/test';
import { BrowseCouponsResponse } from '../../../common/dtos.js';
import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';

export class ThenBrowseCouponsResultStage implements PromiseLike<void> {
  private _executionPromise: Promise<void> | null = null;
  private _browseResult: BrowseCouponsResponse | null = null;
  private _expectSuccess = true;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  shouldSucceed(): ThenBrowseCouponsSuccess {
    this._expectSuccess = true;
    return new ThenBrowseCouponsSuccess(this, this.useCaseContext);
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
    for (const cc of this.ctx.couponConfigs) {
      const resolvedCode = this.useCaseContext.getParamValue(cc.code) as string;
      await this.app.shop().publishCoupon({ code: resolvedCode, discountRate: cc.discountRate });
    }

    const result = await this.app.shop('static').browseCoupons();
    expect(result.success).toBe(true);
    if (result.success) {
      this._browseResult = result.value;
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
    private readonly useCaseContext: UseCaseContext,
  ) {}

  and(): ThenBrowseCouponsSuccess {
    return this;
  }

  coupons(): ThenBrowseCoupons {
    return new ThenBrowseCoupons(this.stage, this.useCaseContext);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenBrowseCoupons implements PromiseLike<void> {
  private _assertions: ((result: BrowseCouponsResponse) => void)[] = [];

  constructor(
    private readonly stage: ThenBrowseCouponsResultStage,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  containsCouponWithCode(expectedCode: string): ThenBrowseCoupons {
    this._assertions.push((result) => {
      const resolvedCode = this.useCaseContext.getParamValue(expectedCode) as string;
      const found = result.coupons.some((c) => c.code === resolvedCode);
      expect(found).toBe(true);
    });
    return this;
  }

  couponCount(expectedCount: number): ThenBrowseCoupons {
    this._assertions.push((result) => {
      expect(result.coupons.length).toBe(expectedCount);
    });
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage
      ._getResult()
      .then((result) => {
        for (const fn of this._assertions) fn(result);
      })
      .then(onfulfilled, onrejected);
  }
}
