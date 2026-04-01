import { ShopDriver, ErpDriver, ClockDriver } from '../drivers/types';
import {
  ErrorResponse,
  PlaceOrderResponse,
  ViewOrderResponse,
  GetTimeResponse,
  GetProductResponse,
} from '../common/dtos';
import { Result } from '../common/result';
import { DEFAULTS } from './defaults';

// --- App Context ---

export interface AppContext {
  shopDriver: ShopDriver;
  erpDriver: ErpDriver;
  clockDriver: ClockDriver;
}

// --- Scenario Context (shared state across stages) ---

interface ClockConfig {
  time: string;
}

interface ProductConfig {
  sku: string;
  price: string;
}

class ScenarioContext {
  clockConfig: ClockConfig | null = null;
  productConfigs: ProductConfig[] = [];
  hasExplicitProduct = false;
}

// === Main Entry Point ===

export class ScenarioDsl {
  constructor(private app: AppContext) {}

  assume(): AssumeStage {
    return new AssumeStage(this.app);
  }

  given(): GivenStage {
    return new GivenStage(this.app, new ScenarioContext());
  }

  when(): WhenStage {
    return new WhenStage(this.app, new ScenarioContext());
  }

  async close(): Promise<void> {
    await this.app.shopDriver.close();
    await this.app.erpDriver.close();
    await this.app.clockDriver.close();
  }
}

// === ASSUME STAGE ===

class AssumeStage {
  constructor(private app: AppContext) {}

  shop(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.shopDriver.goToShop();
      expect(result.success).toBe(true);
    });
  }

  erp(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.erpDriver.goToErp();
      expect(result.success).toBe(true);
    });
  }

  clock(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.clockDriver.goToClock();
      expect(result.success).toBe(true);
    });
  }
}

class AssumeRunning implements PromiseLike<void> {
  constructor(private checkFn: () => Promise<void>) {}

  shouldBeRunning(): AssumeRunning {
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.checkFn().then(onfulfilled, onrejected);
  }
}

// === GIVEN STAGE ===

class GivenStage {
  constructor(
    private app: AppContext,
    private ctx: ScenarioContext,
  ) {}

  clock(): GivenClock {
    this.ctx.clockConfig = { time: DEFAULTS.CLOCK_TIME };
    return new GivenClock(this, this.ctx.clockConfig);
  }

  product(): GivenProduct {
    const config: ProductConfig = { sku: DEFAULTS.SKU, price: DEFAULTS.UNIT_PRICE };
    this.ctx.productConfigs.push(config);
    this.ctx.hasExplicitProduct = true;
    return new GivenProduct(this, config);
  }

  and(): GivenStage {
    return this;
  }

  when(): WhenStage {
    return new WhenStage(this.app, this.ctx);
  }

  then(): ThenContractStage {
    return new ThenContractStage(this.app, this.ctx);
  }
}

class GivenClock {
  constructor(
    private stage: GivenStage,
    private config: ClockConfig,
  ) {}

  withTime(time?: string): GivenClock {
    this.config.time = time || DEFAULTS.CLOCK_TIME;
    return this;
  }

  withWeekday(): GivenClock {
    this.config.time = DEFAULTS.WEEKDAY_TIME;
    return this;
  }

  withWeekend(): GivenClock {
    this.config.time = DEFAULTS.WEEKEND_TIME;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage {
    return this.stage.then();
  }
}

class GivenProduct {
  constructor(
    private stage: GivenStage,
    private config: ProductConfig,
  ) {}

  withSku(sku: string): GivenProduct {
    this.config.sku = sku;
    return this;
  }

  withUnitPrice(price: number | string): GivenProduct {
    this.config.price = typeof price === 'number' ? price.toFixed(2) : price;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage {
    return this.stage.then();
  }
}

// === WHEN STAGE ===

class WhenStage {
  constructor(
    private app: AppContext,
    private ctx: ScenarioContext,
  ) {}

  placeOrder(): WhenPlaceOrder {
    return new WhenPlaceOrder(this.app, this.ctx);
  }
}

class WhenPlaceOrder {
  private sku: string = DEFAULTS.SKU;
  private quantity: string | null = DEFAULTS.QUANTITY;

  constructor(
    private app: AppContext,
    private ctx: ScenarioContext,
  ) {}

  withSku(sku: string): WhenPlaceOrder {
    this.sku = sku;
    return this;
  }

  withQuantity(quantity: string | number | null): WhenPlaceOrder {
    this.quantity = quantity === null ? null : String(quantity);
    return this;
  }

  then(): ThenResultStage {
    return new ThenResultStage(this.app, this.ctx, this.sku, this.quantity);
  }
}

// === THEN RESULT STAGE (after When) ===

class ThenResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private _orderAssertions: ((order: ViewOrderResponse) => void)[] = [];
  private _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private _errorAssertions: ((error: ErrorResponse) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private app: AppContext,
    private ctx: ScenarioContext,
    private sku: string,
    private quantity: string | null,
  ) {}

  shouldSucceed(): ThenSuccess {
    this._expectSuccess = true;
    return new ThenSuccess(this);
  }

  shouldFail(): ThenFailure {
    this._expectSuccess = false;
    return new ThenFailure(this);
  }

  // --- Internal methods for child stages ---

  _addOrderAssertion(fn: (order: ViewOrderResponse) => void): void {
    this._orderAssertions.push(fn);
  }

  _addClockAssertion(fn: (time: GetTimeResponse) => void): void {
    this._clockAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: ErrorResponse) => void): void {
    this._errorAssertions.push(fn);
  }

  // --- Execution ---

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    // 1. Setup given: clock
    if (this.ctx.clockConfig) {
      await this.app.clockDriver.returnsTime({ time: this.ctx.clockConfig.time });
    }

    // 2. Setup given: products (explicit or default)
    if (this.ctx.hasExplicitProduct) {
      for (const pc of this.ctx.productConfigs) {
        await this.app.erpDriver.returnsProduct({ sku: pc.sku, price: pc.price });
      }
    } else {
      await this.app.erpDriver.returnsProduct({ sku: this.sku, price: DEFAULTS.UNIT_PRICE });
    }

    // 3. Execute action
    const result = await this.app.shopDriver.placeOrder({ sku: this.sku, quantity: this.quantity });

    // 4. Assert
    if (this._expectSuccess) {
      expect(result.success).toBe(true);
      if (!result.success) return;

      // View order if needed
      if (this._orderAssertions.length > 0) {
        const orderResult = await this.app.shopDriver.viewOrder(result.value.orderNumber);
        expect(orderResult.success).toBe(true);
        if (orderResult.success) {
          for (const fn of this._orderAssertions) fn(orderResult.value);
        }
      }

      // Get time if needed
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
      for (const fn of this._errorAssertions) fn(result.error);
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

// === SUCCESS PATH ===

class ThenSuccess implements PromiseLike<void> {
  constructor(private stage: ThenResultStage) {}

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

class ThenSuccessAnd implements PromiseLike<void> {
  constructor(private stage: ThenResultStage) {}

  order(): ThenOrder {
    return new ThenOrder(this.stage);
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

class ThenOrder implements PromiseLike<void> {
  constructor(private stage: ThenResultStage) {}

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

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

class ThenClock implements PromiseLike<void> {
  constructor(private stage: ThenResultStage) {}

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

// === FAILURE PATH ===

class ThenFailure implements PromiseLike<void> {
  constructor(private stage: ThenResultStage) {}

  errorMessage(expected: string): ThenFailure {
    this.stage._addErrorAssertion((error) => {
      expect(error.message).toBe(expected);
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): ThenFailure {
    this.stage._addErrorAssertion((error) => {
      const fieldError = error.fieldErrors.find((fe) => fe.field === field);
      expect(fieldError).toBeDefined();
      expect(fieldError!.message).toBe(message);
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

// === CONTRACT THEN STAGE (Given -> Then, no When) ===

class ThenContractStage implements PromiseLike<void> {
  private _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private _productAssertions: Map<string, ((product: GetProductResponse) => void)[]> = new Map();
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private app: AppContext,
    private ctx: ScenarioContext,
  ) {}

  clock(): ThenContractClock {
    return new ThenContractClock(this);
  }

  product(sku: string): ThenContractProduct {
    return new ThenContractProduct(this, sku);
  }

  // --- Internal ---

  _addClockAssertion(fn: (time: GetTimeResponse) => void): void {
    this._clockAssertions.push(fn);
  }

  _addProductAssertion(sku: string, fn: (product: GetProductResponse) => void): void {
    if (!this._productAssertions.has(sku)) this._productAssertions.set(sku, []);
    this._productAssertions.get(sku)!.push(fn);
  }

  // --- Execution ---

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    // Execute given: clock
    if (this.ctx.clockConfig) {
      await this.app.clockDriver.returnsTime({ time: this.ctx.clockConfig.time });
    }

    // Execute given: products
    for (const pc of this.ctx.productConfigs) {
      await this.app.erpDriver.returnsProduct({ sku: pc.sku, price: pc.price });
    }

    // Execute then: clock assertions
    if (this._clockAssertions.length > 0) {
      const timeResult = await this.app.clockDriver.getTime();
      expect(timeResult.success).toBe(true);
      if (timeResult.success) {
        for (const fn of this._clockAssertions) fn(timeResult.value);
      }
    }

    // Execute then: product assertions
    for (const [sku, assertions] of this._productAssertions) {
      const productResult = await this.app.erpDriver.getProduct(sku);
      expect(productResult.success).toBe(true);
      if (productResult.success) {
        for (const fn of assertions) fn(productResult.value);
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

class ThenContractClock implements PromiseLike<void> {
  constructor(private stage: ThenContractStage) {}

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

class ThenContractProduct implements PromiseLike<void> {
  constructor(
    private stage: ThenContractStage,
    private sku: string,
  ) {}

  hasSku(expectedSku: string): ThenContractProduct {
    this.stage._addProductAssertion(this.sku, (p) => {
      expect(p.sku).toBe(expectedSku);
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
