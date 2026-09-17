import { expect } from '@playwright/test';
import type { GetTimeResponse } from '../../../../driver/port/external/clock/dtos/GetTimeResponse.js';
import type { GetProductResponse } from '../../../../driver/port/external/erp/dtos/GetProductResponse.js';
import type { GetTaxResponse } from '../../../../driver/port/external/tax/dtos/GetTaxResponse.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';

import type { ThenStage as IThenStage } from '../../../port/then/then-stage.js';

export class ThenContractStage implements PromiseLike<void>, IThenStage {
  private readonly _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private readonly _productAssertions = new Map<string, ((product: GetProductResponse) => void)[]>();
  private readonly _countryAssertions = new Map<string, ((tax: GetTaxResponse) => void)[]>();
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    readonly useCaseContext: UseCaseContext,
  ) {}

  clock(): ThenContractClock {
    return new ThenContractClock(this);
  }

  product(sku: string): ThenContractProduct {
    return new ThenContractProduct(this, sku);
  }

  country(countryCode: string): ThenContractCountry {
    return new ThenContractCountry(this, countryCode);
  }

  _addClockAssertion(fn: (time: GetTimeResponse) => void): void {
    this._clockAssertions.push(fn);
  }

  _addProductAssertion(sku: string, fn: (product: GetProductResponse) => void): void {
    const assertions = this._productAssertions.get(sku) ?? [];
    assertions.push(fn);
    this._productAssertions.set(sku, assertions);
  }

  _addCountryAssertion(country: string, fn: (tax: GetTaxResponse) => void): void {
    const assertions = this._countryAssertions.get(country) ?? [];
    assertions.push(fn);
    this._countryAssertions.set(country, assertions);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _arrange(): Promise<void> {
    if (this.ctx.clockConfig) {
      await this.app.clockDriver.returnsTime({ time: this.ctx.clockConfig.time });
    }

    for (const pc of this.ctx.productConfigs) {
      const resolvedSku = this.useCaseContext.getParamValue(pc.sku);
      await this.app.erpDriver.returnsProduct({ sku: resolvedSku, price: pc.price });
    }

    for (const cc of this.ctx.countryConfigs) {
      const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(cc.country);
      await this.app.taxDriver.returnsTaxRate({ country: resolvedCountry, taxRate: cc.taxRate });
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

  private async _runProductAssertions(): Promise<void> {
    for (const [sku, assertions] of this._productAssertions) {
      const resolvedSku = this.useCaseContext.getParamValue(sku);
      const productResult = await this.app.erpDriver.getProduct({ sku: resolvedSku });
      expect(productResult.success).toBe(true);
      if (productResult.success) {
        for (const fn of assertions) fn(productResult.value);
      }
    }
  }

  private async _runCountryAssertions(): Promise<void> {
    for (const [countryCode, assertions] of this._countryAssertions) {
      const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(countryCode);
      const taxResult = await this.app.taxDriver.getTaxRate({ country: resolvedCountry });
      expect(taxResult.success).toBe(true);
      if (taxResult.success) {
        for (const fn of assertions) fn(taxResult.value);
      }
    }
  }

  private async _doExecute(): Promise<void> {
    await this._arrange();
    await this._runClockAssertions();
    await this._runProductAssertions();
    await this._runCountryAssertions();
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenContractClock implements PromiseLike<void> {
  constructor(private readonly stage: ThenContractStage) {}

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

export class ThenContractProduct implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenContractStage,
    private readonly sku: string,
  ) {}

  hasSku(expectedSku: string): this {
    this.stage._addProductAssertion(this.sku, (p) => {
      const resolved = this.stage.useCaseContext.getParamValue(expectedSku);
      expect(p.sku).toBe(resolved);
    });
    return this;
  }

  hasPrice(price: number): this {
    this.stage._addProductAssertion(this.sku, (p) => {
      expect(p.price).toBe(price);
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

export class ThenContractCountry implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenContractStage,
    private readonly countryCode: string,
  ) {}

  hasCountry(expected: string): this {
    this.stage._addCountryAssertion(this.countryCode, (t) => {
      const resolved = this.stage.useCaseContext.getParamValueOrLiteral(expected);
      expect(t.country).toBe(resolved);
    });
    return this;
  }

  hasTaxRate(rate: number): this {
    this.stage._addCountryAssertion(this.countryCode, (t) => {
      expect(t.taxRate).toBe(rate);
    });
    return this;
  }

  hasTaxRateIsPositive(): this {
    this.stage._addCountryAssertion(this.countryCode, (t) => {
      expect(t.taxRate).toBeGreaterThan(0);
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
