import { expect } from '@playwright/test';
import type { BrowseOrderHistoryResponse } from '../../../../driver/port/dtos/BrowseOrderHistoryResponse.js';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import type { ScenarioContext } from '../scenario-context.js';

export class ThenBrowseOrderHistoryResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _historyAssertions: ((history: BrowseOrderHistoryResponse, useCaseContext: UseCaseContext) => void)[] = [];
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly orderNumber: string,
  ) {}

  shouldSucceed(): ThenBrowseOrderHistorySuccess {
    this._expectSuccess = true;
    return new ThenBrowseOrderHistorySuccess(this);
  }

  shouldFail(): ThenBrowseOrderHistoryFailure {
    this._expectSuccess = false;
    return new ThenBrowseOrderHistoryFailure(this);
  }

  _addHistoryAssertion(fn: (history: BrowseOrderHistoryResponse, useCaseContext: UseCaseContext) => void): void {
    this._historyAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext);

    const orderNumber = this.useCaseContext.getResultValue(this.orderNumber);
    const result = await this.app.myShop('dynamic').browseOrderHistory({ orderNumber });

    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (result.success) {
        for (const fn of this._historyAssertions) fn(result.value, this.useCaseContext);
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
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenBrowseOrderHistorySuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseOrderHistoryResultStage) {}

  and(): this {
    return this;
  }

  orderHistory(): ThenBrowseOrderHistoryOrderHistory {
    return new ThenBrowseOrderHistoryOrderHistory(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenBrowseOrderHistoryOrderHistory implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseOrderHistoryResultStage) {}

  and(): this {
    return this;
  }

  containsOrder(orderNumber: string): this {
    this.stage._addHistoryAssertion((history, useCaseContext) => {
      const expected = useCaseContext.getResultValue(orderNumber);
      expect(listedOrderNumbers(history), `Order history should contain order '${expected}'`).toContain(expected);
    });
    return this;
  }

  doesNotContainOrder(orderNumber: string): this {
    this.stage._addHistoryAssertion((history, useCaseContext) => {
      const unexpected = useCaseContext.getResultValue(orderNumber);
      expect(listedOrderNumbers(history), `Order history should not contain order '${unexpected}'`).not.toContain(unexpected);
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

export class ThenBrowseOrderHistoryFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseOrderHistoryResultStage) {}

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
      expect(fieldError?.message, `Expected field error for '${field}' in ${JSON.stringify(error.fieldErrors)}`).toBe(expandedMessage);
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

function listedOrderNumbers(history: BrowseOrderHistoryResponse): string[] {
  expect(history.orders, 'Orders list should not be null').toBeDefined();
  return history.orders.map((order) => order.orderNumber);
}
