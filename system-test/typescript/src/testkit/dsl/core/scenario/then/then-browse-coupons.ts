import { expect } from '@playwright/test';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { BrowseCouponsResponse } from '../../../../driver/port/dtos/BrowseCouponsResponse.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';

export class ThenBrowseCouponsResultStage implements PromiseLike<void> {
  private _executionPromise: Promise<void> | null = null;
  private _browseResult: BrowseCouponsResponse | null = null;
  private _expectSuccess = true;
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
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
    this.ctx.reserveDefaultCustomerIfUsed();
    for (const cc of this.ctx.couponConfigs) {
      const resolvedCode = this.useCaseContext.getParamValue(cc.code);
      await this.app.myShop().publishCoupon({ code: resolvedCode, discountRate: String(cc.discountRate) });
    }

    this.app.actAs(this.ctx.loggedInIdentity());

    const result = await this.app.myShop('static').browseCoupons({});
    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (result.success) {
        this._browseResult = result.value;
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
