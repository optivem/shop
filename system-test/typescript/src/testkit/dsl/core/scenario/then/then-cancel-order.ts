import { expect } from '@playwright/test';
import { SystemError } from '../../../../driver/port/myShop/dtos/errors/SystemError.js';
import { ViewOrderResponse } from '../../../../driver/port/myShop/dtos/ViewOrderResponse.js';
import { DEFAULTS } from '../defaults.js';
import { UseCaseContext } from '../../shared/use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';

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

  private async _doExecute(): Promise<void> {
    if (this.ctx.clockConfig) {
      await this.app.clockDriver.returnsTime({ time: this.ctx.clockConfig.time });
    }

    if (this.ctx.countryConfigs.length > 0) {
      for (const countryConfig of this.ctx.countryConfigs) {
        const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(countryConfig.country) as string;
        await this.app.taxDriver.returnsTaxRate({ country: resolvedCountry, taxRate: countryConfig.taxRate });
      }
    } else {
      const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(DEFAULTS.COUNTRY) as string;
      await this.app.taxDriver.returnsTaxRate({ country: resolvedCountry, taxRate: DEFAULTS.TAX_RATE });
    }

    await this.app.erpDriver.returnsPromotion({
      promotionActive: this.ctx.promotionConfig.promotionActive,
      discount: this.ctx.promotionConfig.discount,
    });

    if (this.ctx.hasExplicitProduct) {
      for (const pc of this.ctx.productConfigs) {
        const resolvedSku = this.useCaseContext.getParamValue(pc.sku) as string;
        await this.app.erpDriver.returnsProduct({ sku: resolvedSku, price: pc.price });
      }
    } else {
      const resolvedSku = this.useCaseContext.getParamValue(DEFAULTS.SKU) as string;
      await this.app.erpDriver.returnsProduct({ sku: resolvedSku, price: DEFAULTS.UNIT_PRICE });
    }

    for (const cc of this.ctx.couponConfigs) {
      const resolvedCode = this.useCaseContext.getParamValue(cc.code) as string;
      await this.app.myShop().publishCoupon({
        code: resolvedCode,
        discountRate: cc.discountRate,
        validFrom: cc.validFrom,
        validTo: cc.validTo,
        usageLimit: cc.usageLimit,
      });
    }

    // Place any given orders
    for (const oc of this.ctx.orderConfigs) {
      const resolvedSku = this.useCaseContext.getParamValue(oc.sku) as string;
      const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(oc.country) as string;
      const resolvedCouponCode = this.useCaseContext.getParamValue(oc.couponCode) as string | null;
      const placeResult = await this.app.myShop().placeOrder({
        sku: resolvedSku,
        quantity: oc.quantity,
        country: resolvedCountry,
        couponCode: resolvedCouponCode,
      });
      if (placeResult.success) {
        oc.orderNumber = placeResult.value.orderNumber;
        if (oc.status === 'CANCELLED') {
          await this.app.myShop().cancelOrder(placeResult.value.orderNumber);
        }
        if (oc.status === 'DELIVERED') {
          await this.app.myShop().deliverOrder(placeResult.value.orderNumber);
        }
      }
    }

    const targetOrderNumber = this.ctx.orderConfigs.length > 0 && this.ctx.orderConfigs[0].orderNumber
      ? this.ctx.orderConfigs[0].orderNumber
      : this.orderNumber;

    const result = await this.app.myShop('dynamic').cancelOrder(targetOrderNumber);

    if (this._expectSuccess) {
      expect(result.success).toBe(true);

      if (this._orderAssertions.length > 0) {
        const orderResult = await this.app.myShop().viewOrder(targetOrderNumber);
        expect(orderResult.success).toBe(true);
        if (orderResult.success) {
          for (const fn of this._orderAssertions) fn(orderResult.value);
        }
      }
    } else {
      expect(result.success).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      }

      if (this._orderAssertions.length > 0) {
        const orderResult = await this.app.myShop().viewOrder(targetOrderNumber);
        expect(orderResult.success).toBe(true);
        if (orderResult.success) {
          for (const fn of this._orderAssertions) fn(orderResult.value);
        }
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

  hasStatus(status: string): this {
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
