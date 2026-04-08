import { expect } from '@playwright/test';
import {
  ErrorResponse,
  ViewOrderResponse,
  ViewCouponResponse,
  GetTimeResponse,
} from '../../../common/dtos.js';
import { DEFAULTS } from '../../defaults.js';
import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';

export class ThenResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private _orderAssertions: ((order: ViewOrderResponse) => void)[] = [];
  private readonly _couponAssertions: { code: string; fns: ((coupon: ViewCouponResponse) => void)[] }[] = [];
  private _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private _errorAssertions: ((error: ErrorResponse, useCaseContext: UseCaseContext) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    readonly useCaseContext: UseCaseContext,
    private readonly sku: string,
    private readonly quantity: string | null,
    private readonly country: string = DEFAULTS.COUNTRY,
    private readonly couponCode: string | null = null,
  ) {}

  shouldSucceed(): ThenSuccess {
    this._expectSuccess = true;
    return new ThenSuccess(this);
  }

  shouldFail(): ThenFailure {
    this._expectSuccess = false;
    return new ThenFailure(this);
  }

  _addOrderAssertion(fn: (order: ViewOrderResponse) => void): void {
    this._orderAssertions.push(fn);
  }

  _addCouponAssertion(code: string, fn: (coupon: ViewCouponResponse) => void): void {
    let entry = this._couponAssertions.find((e) => e.code === code);
    if (!entry) {
      entry = { code, fns: [] };
      this._couponAssertions.push(entry);
    }
    entry.fns.push(fn);
  }

  _addClockAssertion(fn: (time: GetTimeResponse) => void): void {
    this._clockAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: ErrorResponse, useCaseContext: UseCaseContext) => void): void {
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
      await this.app.shop().publishCoupon({
        code: resolvedCode,
        discountRate: cc.discountRate,
        validFrom: cc.validFrom,
        validTo: cc.validTo,
        usageLimit: cc.usageLimit,
      });
    }

    // Execute given orders (e.g., to exhaust coupon usage limits)
    for (const oc of this.ctx.orderConfigs) {
      const orderSku = this.useCaseContext.getParamValue(oc.sku) as string;
      const orderCountry = this.useCaseContext.getParamValueOrLiteral(oc.country) as string;
      const orderCouponCode = this.useCaseContext.getParamValue(oc.couponCode) as string | null;
      const orderResult = await this.app.shop().placeOrder({
        sku: orderSku,
        quantity: oc.quantity,
        country: orderCountry,
        couponCode: orderCouponCode,
      });
      expect(orderResult.success).toBe(true);
    }

    const resolvedSku = this.useCaseContext.getParamValue(this.sku) as string;
    const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(this.country) as string;
    const resolvedCouponCode = this.useCaseContext.getParamValue(this.couponCode) as string | null;

    const result = await this.app.shop('dynamic').placeOrder({
      sku: resolvedSku,
      quantity: this.quantity,
      country: resolvedCountry,
      couponCode: resolvedCouponCode,
    });

    if (this._expectSuccess) {
      expect(result.success).toBe(true);
      if (!result.success) return;

      if (this._orderAssertions.length > 0) {
        const orderResult = await this.app.shop().viewOrder(result.value.orderNumber);
        expect(orderResult.success).toBe(true);
        if (orderResult.success) {
          for (const fn of this._orderAssertions) fn(orderResult.value);
        }
      }

      for (const couponEntry of this._couponAssertions) {
        const couponResult = await this.app.shop().viewCoupon(couponEntry.code);
        expect(couponResult.success).toBe(true);
        if (couponResult.success) {
          for (const fn of couponEntry.fns) fn(couponResult.value);
        }
      }

      if (this._clockAssertions.length > 0) {
        const timeResult = await this.app.clockDriver.getTime();
        expect(timeResult.success).toBe(true);
        if (timeResult.success) {
          for (const fn of this._clockAssertions) fn(timeResult.value);
        }
      }
    } else {
      expect(result.success).toBe(false);
      if (result.success) return;
      for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

  and(): ThenSuccessAnd {
    return new ThenSuccessAnd(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenSuccessAnd implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

  order(): ThenOrder {
    return new ThenOrder(this.stage);
  }

  coupon(code: string): ThenCoupon {
    return new ThenCoupon(this.stage, code);
  }

  clock(): ThenClock {
    return new ThenClock(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenOrder implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

  hasOrderNumberPrefix(prefix: string): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.orderNumber.startsWith(prefix)).toBe(true);
    });
    return this;
  }

  hasStatus(status: string): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.status).toBe(status);
    });
    return this;
  }

  hasTotalPrice(price: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.totalPrice).toBe(price);
    });
    return this;
  }

  hasSubtotalPrice(price: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.subtotalPrice).toBe(price);
    });
    return this;
  }

  hasTaxRate(rate: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.taxRate).toBe(rate);
    });
    return this;
  }

  hasDiscountRate(rate: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.discountRate).toBe(rate);
    });
    return this;
  }

  hasAppliedCouponCode(code: string | null): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      const resolvedCode = this.stage.useCaseContext.getParamValue(code);
      expect(order.appliedCouponCode).toBe(resolvedCode);
    });
    return this;
  }

  hasBasePrice(price: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.basePrice).toBe(price);
    });
    return this;
  }

  hasDiscountAmount(amount: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.discountAmount).toBe(amount);
    });
    return this;
  }

  hasTaxAmount(amount: number): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.taxAmount).toBe(amount);
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

export class ThenClock implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

  hasTime(time?: string): ThenClock {
    this.stage._addClockAssertion((t) => {
      if (time) {
        expect(t.time).toContain(time);
      } else {
        expect(t.time).toBeTruthy();
      }
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

export class ThenFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

  errorMessage(expected: string): ThenFailure {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      expect(error.message).toBe(useCaseContext.expandAliases(expected));
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): ThenFailure {
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

export class ThenCoupon implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenResultStage,
    private readonly code: string,
  ) {}

  hasDiscountRate(rate: number): ThenCoupon {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.discountRate).toBe(rate);
    });
    return this;
  }

  isValidFrom(validFrom: string): ThenCoupon {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.validFrom).toBe(validFrom);
    });
    return this;
  }

  isValidTo(validTo: string): ThenCoupon {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.validTo).toBe(validTo);
    });
    return this;
  }

  hasUsageLimit(limit: number): ThenCoupon {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.usageLimit).toBe(limit);
    });
    return this;
  }

  hasUsedCount(count: number): ThenCoupon {
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
