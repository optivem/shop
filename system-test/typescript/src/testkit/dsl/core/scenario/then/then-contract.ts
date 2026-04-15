import { expect } from '@playwright/test';
import { GetTimeResponse, GetProductResponse, GetTaxResponse } from '../../../../common/dtos.js';
import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';

export class ThenContractStage implements PromiseLike<void> {
  private _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private _productAssertions: Map<string, ((product: GetProductResponse) => void)[]> = new Map();
  private _countryAssertions: Map<string, ((tax: GetTaxResponse) => void)[]> = new Map();
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
    if (!this._productAssertions.has(sku)) this._productAssertions.set(sku, []);
    this._productAssertions.get(sku)!.push(fn);
  }

  _addCountryAssertion(country: string, fn: (tax: GetTaxResponse) => void): void {
    if (!this._countryAssertions.has(country)) this._countryAssertions.set(country, []);
    this._countryAssertions.get(country)!.push(fn);
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

    for (const pc of this.ctx.productConfigs) {
      const resolvedSku = this.useCaseContext.getParamValue(pc.sku) as string;
      await this.app.erpDriver.returnsProduct({ sku: resolvedSku, price: pc.price });
    }

    for (const cc of this.ctx.countryConfigs) {
      const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(cc.country) as string;
      await this.app.taxDriver.returnsTaxRate({ country: resolvedCountry, taxRate: cc.taxRate });
    }

    if (this._clockAssertions.length > 0) {
      const timeResult = await this.app.clockDriver.getTime();
      expect(timeResult.success).toBe(true);
      if (timeResult.success) {
        for (const fn of this._clockAssertions) fn(timeResult.value);
      }
    }

    for (const [sku, assertions] of this._productAssertions) {
      const resolvedSku = this.useCaseContext.getParamValue(sku) as string;
      const productResult = await this.app.erpDriver.getProduct(resolvedSku);
      expect(productResult.success).toBe(true);
      if (productResult.success) {
        for (const fn of assertions) fn(productResult.value);
      }
    }

    for (const [countryCode, assertions] of this._countryAssertions) {
      const resolvedCountry = this.useCaseContext.getParamValueOrLiteral(countryCode) as string;
      const taxResult = await this.app.taxDriver.getTaxRate(resolvedCountry);
      expect(taxResult.success).toBe(true);
      if (taxResult.success) {
        for (const fn of assertions) fn(taxResult.value);
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

export class ThenContractClock implements PromiseLike<void> {
  constructor(private readonly stage: ThenContractStage) {}

  hasTime(time?: string): ThenContractClock {
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

  hasSku(expectedSku: string): ThenContractProduct {
    this.stage._addProductAssertion(this.sku, (p) => {
      const resolved = this.stage.useCaseContext.getParamValue(expectedSku) as string;
      expect(p.sku).toBe(resolved);
    });
    return this;
  }

  hasPrice(price: number): ThenContractProduct {
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

  hasCountry(expected: string): ThenContractCountry {
    this.stage._addCountryAssertion(this.countryCode, (t) => {
      const resolved = this.stage.useCaseContext.getParamValueOrLiteral(expected) as string;
      expect(t.country).toBe(resolved);
    });
    return this;
  }

  hasTaxRate(rate: number): ThenContractCountry {
    this.stage._addCountryAssertion(this.countryCode, (t) => {
      expect(t.taxRate).toBe(rate);
    });
    return this;
  }

  hasTaxRateIsPositive(): ThenContractCountry {
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
