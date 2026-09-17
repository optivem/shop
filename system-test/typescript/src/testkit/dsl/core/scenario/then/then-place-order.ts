import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';
import { expect } from '@playwright/test';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { ViewOrderResponse } from '../../../../driver/port/dtos/ViewOrderResponse.js';
import type { BrowseCouponItem } from '../../../../driver/port/dtos/BrowseCouponsResponse.js';
import type { GetTimeResponse } from '../../../../driver/port/external/clock/dtos/GetTimeResponse.js';
import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';

export class ThenResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _orderAssertions: ((order: ViewOrderResponse) => void)[] = [];
  private readonly _couponAssertions: { code: string; fns: ((coupon: BrowseCouponItem) => void)[] }[] = [];
  private readonly _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    readonly useCaseContext: UseCaseContext,
    private readonly sku: string | null,
    private readonly quantity: string | null,
    private readonly country: string | null = DEFAULTS.COUNTRY,
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

  _addCouponAssertion(code: string, fn: (coupon: BrowseCouponItem) => void): void {
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

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _arrangeClock(): Promise<void> {
    if (this.ctx.clockConfig) {
      await this.app.clockDriver.returnsTime({ time: this.ctx.clockConfig.time });
    }
  }

  private async _arrangeTax(): Promise<void> {
    if (this.ctx.countryConfigs.length > 0) {
      for (const countryConfig of this.ctx.countryConfigs) {
        const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(countryConfig.country);
        await this.app.taxDriver.returnsTaxRate({ country: resolvedCountry, taxRate: countryConfig.taxRate });
      }
      return;
    }
    const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(DEFAULTS.COUNTRY);
    await this.app.taxDriver.returnsTaxRate({ country: resolvedCountry, taxRate: DEFAULTS.TAX_RATE });
  }

  private async _arrangeProducts(): Promise<void> {
    if (this.ctx.hasExplicitProduct) {
      for (const pc of this.ctx.productConfigs) {
        const resolvedSku = this.useCaseContext.getParamValue(pc.sku);
        await this.app.erpDriver.returnsProduct({ sku: resolvedSku, price: pc.price });
      }
      return;
    }
    const resolvedSku = this.useCaseContext.getParamValue(DEFAULTS.SKU);
    await this.app.erpDriver.returnsProduct({ sku: resolvedSku, price: DEFAULTS.UNIT_PRICE });
  }

  private async _arrangeCoupons(): Promise<void> {
    for (const cc of this.ctx.couponConfigs) {
      const resolvedCode = this.useCaseContext.getParamValue(cc.code);
      await this.app.myShop().publishCoupon({
        code: resolvedCode,
        discountRate: String(cc.discountRate),
        validFrom: cc.validFrom,
        validTo: cc.validTo,
        usageLimit: cc.usageLimit === undefined ? undefined : String(cc.usageLimit),
      });
    }
  }

  private async _placeGivenOrders(): Promise<void> {
    // Execute given orders (e.g., to exhaust coupon usage limits)
    for (const oc of this.ctx.orderConfigs) {
      const orderSku = this.useCaseContext.getParamValue(oc.sku);
      const orderCountry = this.useCaseContext.getParamValueOrLiteral(oc.country);
      const orderCouponCode = this.useCaseContext.getParamValue(oc.couponCode);
      const orderResult = await this.app.myShop().placeOrder({
        sku: orderSku,
        quantity: oc.quantity,
        country: orderCountry,
        couponCode: orderCouponCode,
      });
      expect(orderResult.success).toBe(true);
    }
  }

  private async _runOrderAssertions(orderNumber: string): Promise<void> {
    if (this._orderAssertions.length === 0) return;
    const orderResult = await this.app.myShop().viewOrder({ orderNumber });
    expect(orderResult.success).toBe(true);
    if (orderResult.success) {
      for (const fn of this._orderAssertions) fn(orderResult.value);
    }
  }

  private async _runCouponAssertions(): Promise<void> {
    for (const couponEntry of this._couponAssertions) {
      const resolvedCouponCode = this.useCaseContext.getParamValue(couponEntry.code);
      const browseResult = await this.app.myShop().browseCoupons({});
      expect(browseResult.success).toBe(true);
      if (!browseResult.success) continue;
      const coupon = browseResult.value.coupons.find((c) => c.code === resolvedCouponCode);
      expect(coupon, `Coupon '${resolvedCouponCode}' not found in browse results`).toBeDefined();
      if (coupon) {
        for (const fn of couponEntry.fns) fn(coupon);
      }
    }
  }

  private async _runClockAssertions(): Promise<void> {
    if (this._clockAssertions.length === 0) return;
    const timeResult = await this.app.clockDriver.getTime();
    expect(timeResult.success).toBe(true);
    if (timeResult.success) {
      for (const fn of this._clockAssertions) fn(timeResult.value);
    }
  }

  private async _doExecute(): Promise<void> {
    await this._arrangeClock();
    await this._arrangeTax();

    await this.app.erpDriver.returnsPromotion({
      promotionActive: this.ctx.promotionConfig.promotionActive,
      discount: this.ctx.promotionConfig.discount,
    });

    await this._arrangeProducts();
    await this._arrangeCoupons();
    await this._placeGivenOrders();

    const resolvedSku = this.useCaseContext.getParamValue(this.sku);
    const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(this.country);
    const resolvedCouponCode = this.useCaseContext.getParamValue(this.couponCode);

    const result = await this.app.myShop('dynamic').placeOrder({
      sku: resolvedSku,
      quantity: this.quantity,
      country: resolvedCountry,
      couponCode: resolvedCouponCode,
    });

    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (!result.success) return;
      await this._runOrderAssertions(result.value.orderNumber);
      await this._runCouponAssertions();
      await this._runClockAssertions();
    } else {
      expect(result.success, JSON.stringify(result)).toBe(false);
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

  and(): this {
    return this;
  }

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

  and(): this {
    return this;
  }

  hasSku(expectedSku: string): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.sku).toBe(expectedSku);
    });
    return this;
  }

  hasQuantity(expectedQuantity: number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.quantity).toBe(expectedQuantity);
    });
    return this;
  }

  hasUnitPrice(expectedUnitPrice: number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.unitPrice).toBe(expectedUnitPrice);
    });
    return this;
  }

  hasOrderNumberPrefix(prefix: string): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.orderNumber.startsWith(prefix)).toBe(true);
    });
    return this;
  }

  hasStatus(status: OrderStatus): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.status).toBe(status);
    });
    return this;
  }

  hasTotalPrice(totalPrice: string | number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.totalPrice).toBe(Number(totalPrice));
    });
    return this;
  }

  hasTotalPriceGreaterThanZero(): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.totalPrice).toBeGreaterThan(0);
    });
    return this;
  }

  hasSubtotalPrice(subtotalPrice: string | number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.subtotalPrice).toBe(Number(subtotalPrice));
    });
    return this;
  }

  hasTaxRate(taxRate: string | number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.taxRate).toBe(Number(taxRate));
    });
    return this;
  }

  hasTaxAmount(taxAmount: string | number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.taxAmount).toBe(Number(taxAmount));
    });
    return this;
  }

  hasDiscountRate(rate: number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.discountRate).toBe(rate);
    });
    return this;
  }

  hasDiscountAmount(discountAmount: string | number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.discountAmount).toBe(Number(discountAmount));
    });
    return this;
  }

  hasAppliedCoupon(expectedCouponCode?: string | null): this {
    this.stage._addOrderAssertion((order) => {
      if (expectedCouponCode === undefined) {
        expect(order.appliedCouponCode).toBeDefined();
      } else {
        const resolvedCode = this.stage.useCaseContext.getParamValue(expectedCouponCode);
        expect(order.appliedCouponCode).toBe(resolvedCode);
      }
    });
    return this;
  }

  hasBasePrice(basePrice: string | number): this {
    this.stage._addOrderAssertion((order) => {
      expect(order.basePrice).toBe(Number(basePrice));
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

  hasTime(time?: string): this {
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

export class ThenCoupon implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenResultStage,
    private readonly code: string,
  ) {}

  and(): this {
    return this;
  }

  hasDiscountRate(rate: number): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.discountRate).toBe(rate);
    });
    return this;
  }

  isValidFrom(validFrom: string): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.validFrom ? new Date(coupon.validFrom).getTime() : coupon.validFrom, 'validFrom').toBe(new Date(validFrom).getTime());
    });
    return this;
  }

  isValidTo(validTo: string): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.validTo ? new Date(coupon.validTo).getTime() : coupon.validTo, 'validTo').toBe(new Date(validTo).getTime());
    });
    return this;
  }

  hasUsageLimit(limit: number): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.usageLimit).toBe(limit);
    });
    return this;
  }

  hasUsedCount(expectedUsedCount: number): this {
    this.stage._addCouponAssertion(this.code, (coupon) => {
      expect(coupon.usedCount).toBe(expectedUsedCount);
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
