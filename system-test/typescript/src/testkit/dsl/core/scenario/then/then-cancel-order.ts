import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';
import { expect } from '@playwright/test';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { ViewOrderResponse } from '../../../../driver/port/dtos/ViewOrderResponse.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { arrangeGiven } from '../given/arrange-given.js';

export class ThenCancelOrderResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _orderAssertions: ((order: ViewOrderResponse) => void)[] = [];
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly orderNumber: string,
  ) {}

  shouldSucceed(): ThenCancelOrderSuccess {
    this._expectSuccess = true;
    return new ThenCancelOrderSuccess(this);
  }

  shouldFail(): ThenCancelOrderFailure {
    this._expectSuccess = false;
    return new ThenCancelOrderFailure(this);
  }

  _addOrderAssertion(fn: (order: ViewOrderResponse) => void): void {
    this._orderAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _runOrderAssertions(targetOrderNumber: string): Promise<void> {
    if (this._orderAssertions.length === 0) return;
    const orderResult = await this.app.myShop().viewOrder({ orderNumber: targetOrderNumber });
    expect(orderResult.success).toBe(true);
    if (orderResult.success) {
      for (const fn of this._orderAssertions) fn(orderResult.value);
    }
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext);

    const targetOrderNumber = this.ctx.orderConfigs[0]?.orderNumber ?? this.orderNumber;

    const result = await this.app.myShop('dynamic').cancelOrder({ orderNumber: targetOrderNumber });

    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
    } else {
      expect(result.success, JSON.stringify(result)).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      }
    }

    await this._runOrderAssertions(targetOrderNumber);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenCancelOrderSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenCancelOrderResultStage) {}

  and(): this {
    return this;
  }

  order(): ThenCancelOrderOrder {
    return new ThenCancelOrderOrder(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenCancelOrderOrder implements PromiseLike<void> {
  constructor(private readonly stage: ThenCancelOrderResultStage) {}

  hasStatus(status: OrderStatus): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.status).toBe(status);
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

export class ThenCancelOrderFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenCancelOrderResultStage) {}

  and(): this {
    return this;
  }

  order(): ThenCancelOrderOrder {
    return new ThenCancelOrderOrder(this.stage);
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
