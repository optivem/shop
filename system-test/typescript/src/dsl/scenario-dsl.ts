import { ShopDriver, ErpDriver, ClockDriver } from '../drivers/types';
import {
  ErrorResponse,
  ViewOrderResponse,
  GetTimeResponse,
  GetProductResponse,
  BrowseCouponsResponse,
} from '../common/dtos';
import { DEFAULTS } from './defaults';

// --- App Context ---

export type ChannelMode = 'dynamic' | 'static';

const STATIC_CHANNEL = 'api';

export class AppContext {
  private readonly shops = new Map<string, ShopDriver>();
  private readonly channelMode: ChannelMode;
  private readonly channel: string;
  private readonly shopDriverFactory: (channel: string) => ShopDriver;
  readonly erpDriver: ErpDriver;
  readonly clockDriver: ClockDriver;

  constructor(opts: {
    channelMode: ChannelMode;
    channel: string;
    shopDriverFactory: (channel: string) => ShopDriver;
    erpDriver: ErpDriver;
    clockDriver: ClockDriver;
  }) {
    this.channelMode = opts.channelMode;
    this.channel = opts.channel;
    this.shopDriverFactory = opts.shopDriverFactory;
    this.erpDriver = opts.erpDriver;
    this.clockDriver = opts.clockDriver;
  }

  shop(mode?: ChannelMode): ShopDriver {
    const resolvedMode = mode ?? this.channelMode;
    const channel = resolvedMode === 'static' ? STATIC_CHANNEL : this.channel;
    console.log(`[ChannelMode] mode=${resolvedMode} → channel=${channel}`);
    if (!this.shops.has(channel)) {
      this.shops.set(channel, this.shopDriverFactory(channel));
    }
    return this.shops.get(channel)!;
  }

  async closeAll(): Promise<void> {
    for (const driver of this.shops.values()) {
      await driver.close();
    }
    await this.erpDriver.close();
    await this.clockDriver.close();
  }
}

// --- Scenario Context (shared state across stages) ---

interface ClockConfig {
  time: string;
}

interface ProductConfig {
  sku: string;
  price: string;
}

interface PromotionConfig {
  promotionActive: boolean;
  discount: string;
}

interface CouponConfig {
  code: string;
  discountRate: number;
}

class ScenarioContext {
  clockConfig: ClockConfig | null = null;
  productConfigs: ProductConfig[] = [];
  couponConfigs: CouponConfig[] = [];
  hasExplicitProduct = false;
  promotionConfig: PromotionConfig = { promotionActive: DEFAULTS.PROMOTION_ACTIVE, discount: DEFAULTS.PROMOTION_DISCOUNT };
  hasExplicitPromotion = false;
}

// === Main Entry Point ===

export class ScenarioDsl {
  constructor(private readonly app: AppContext) {}

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
    await this.app.closeAll();
  }
}

// === ASSUME STAGE ===

class AssumeStage {
  constructor(private readonly app: AppContext) {}

  shop(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.shop().goToShop();
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
  constructor(private readonly checkFn: () => Promise<void>) {}

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
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
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

  promotion(): GivenPromotion {
    this.ctx.hasExplicitPromotion = true;
    return new GivenPromotion(this, this.ctx.promotionConfig);
  }

  coupon(): GivenCoupon {
    const config: CouponConfig = { code: '', discountRate: 0.10 };
    this.ctx.couponConfigs.push(config);
    return new GivenCoupon(this, config);
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
    private readonly stage: GivenStage,
    private readonly config: ClockConfig,
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

class GivenPromotion {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: PromotionConfig,
  ) {}

  withActive(promotionActive: boolean): GivenPromotion {
    this.config.promotionActive = promotionActive;
    return this;
  }

  withDiscount(discount: number | string): GivenPromotion {
    this.config.discount = typeof discount === 'number' ? discount.toFixed(2) : discount;
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
    private readonly stage: GivenStage,
    private readonly config: ProductConfig,
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

class GivenCoupon {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: CouponConfig,
  ) {}

  withCode(code: string): GivenCoupon {
    this.config.code = code;
    return this;
  }

  withDiscountRate(discountRate: number): GivenCoupon {
    this.config.discountRate = discountRate;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }
}

// === WHEN STAGE ===

class WhenStage {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
  ) {}

  placeOrder(): WhenPlaceOrder {
    return new WhenPlaceOrder(this.app, this.ctx);
  }

  publishCoupon(): WhenPublishCoupon {
    return new WhenPublishCoupon(this.app);
  }

  browseCoupons(): WhenBrowseCoupons {
    return new WhenBrowseCoupons(this.app, this.ctx);
  }
}

class WhenPlaceOrder {
  private sku: string = DEFAULTS.SKU;
  private quantity: string | null = DEFAULTS.QUANTITY;
  private country: string = DEFAULTS.COUNTRY;
  private couponCode: string | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
  ) {}

  withSku(sku: string): WhenPlaceOrder {
    this.sku = sku;
    return this;
  }

  withQuantity(quantity: string | number | null): WhenPlaceOrder {
    this.quantity = quantity === null ? null : String(quantity);
    return this;
  }

  withCountry(country: string): WhenPlaceOrder {
    this.country = country;
    return this;
  }

  withCouponCode(couponCode: string): WhenPlaceOrder {
    this.couponCode = couponCode;
    return this;
  }

  then(): ThenResultStage {
    return new ThenResultStage(this.app, this.ctx, this.sku, this.quantity, this.country, this.couponCode);
  }
}

class WhenPublishCoupon {
  private code: string = '';
  private discountRate: number = 0;

  constructor(private readonly app: AppContext) {}

  withCode(code: string): WhenPublishCoupon {
    this.code = code;
    return this;
  }

  withDiscountRate(discountRate: number): WhenPublishCoupon {
    this.discountRate = discountRate;
    return this;
  }

  then(): ThenPublishCouponResultStage {
    return new ThenPublishCouponResultStage(this.app, this.code, this.discountRate);
  }
}

class WhenBrowseCoupons {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
  ) {}

  then(): ThenBrowseCouponsResultStage {
    return new ThenBrowseCouponsResultStage(this.app, this.ctx);
  }
}

// === THEN RESULT STAGE (after PlaceOrder) ===

class ThenResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private _orderAssertions: ((order: ViewOrderResponse) => void)[] = [];
  private _clockAssertions: ((time: GetTimeResponse) => void)[] = [];
  private _errorAssertions: ((error: ErrorResponse) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
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

  _addClockAssertion(fn: (time: GetTimeResponse) => void): void {
    this._clockAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: ErrorResponse) => void): void {
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

    await this.app.erpDriver.returnsPromotion({
      promotionActive: this.ctx.promotionConfig.promotionActive,
      discount: this.ctx.promotionConfig.discount,
    });

    if (this.ctx.hasExplicitProduct) {
      for (const pc of this.ctx.productConfigs) {
        await this.app.erpDriver.returnsProduct({ sku: pc.sku, price: pc.price });
      }
    } else if (this._expectSuccess) {
      await this.app.erpDriver.returnsProduct({ sku: this.sku, price: DEFAULTS.UNIT_PRICE });
    }

    for (const cc of this.ctx.couponConfigs) {
      await this.app.shop().publishCoupon({ code: cc.code, discountRate: cc.discountRate });
    }

    const result = await this.app.shop('dynamic').placeOrder({
      sku: this.sku,
      quantity: this.quantity,
      country: this.country,
      couponCode: this.couponCode,
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

// === THEN PUBLISH COUPON RESULT STAGE ===

class ThenPublishCouponResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private _errorAssertions: ((error: ErrorResponse) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly code: string,
    private readonly discountRate: number,
  ) {}

  shouldSucceed(): ThenPublishCouponSuccess {
    this._expectSuccess = true;
    return new ThenPublishCouponSuccess(this);
  }

  shouldFail(): ThenPublishCouponFailure {
    this._expectSuccess = false;
    return new ThenPublishCouponFailure(this);
  }

  _addErrorAssertion(fn: (error: ErrorResponse) => void): void {
    this._errorAssertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    const result = await this.app.shop('dynamic').publishCoupon({ code: this.code, discountRate: this.discountRate });

    if (this._expectSuccess) {
      expect(result.success).toBe(true);
    } else {
      expect(result.success).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error);
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

class ThenPublishCouponSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenPublishCouponResultStage) {}

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

class ThenPublishCouponFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenPublishCouponResultStage) {}

  errorMessage(expected: string): ThenPublishCouponFailure {
    this.stage._addErrorAssertion((error) => {
      expect(error.message).toBe(expected);
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): ThenPublishCouponFailure {
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

// === THEN BROWSE COUPONS RESULT STAGE ===

class ThenBrowseCouponsResultStage implements PromiseLike<void> {
  private _executionPromise: Promise<void> | null = null;
  private _browseResult: BrowseCouponsResponse | null = null;
  private _expectSuccess = true;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
  ) {}

  shouldSucceed(): ThenBrowseCouponsSuccess {
    this._expectSuccess = true;
    return new ThenBrowseCouponsSuccess(this);
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
      await this.app.shop().publishCoupon({ code: cc.code, discountRate: cc.discountRate });
    }

    const result = await this.app.shop('dynamic').browseCoupons();
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

class ThenBrowseCouponsSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseCouponsResultStage) {}

  and(): ThenBrowseCouponsSuccess {
    return this;
  }

  coupons(): ThenBrowseCoupons {
    return new ThenBrowseCoupons(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

class ThenBrowseCoupons implements PromiseLike<void> {
  private _assertions: ((result: BrowseCouponsResponse) => void)[] = [];

  constructor(private readonly stage: ThenBrowseCouponsResultStage) {}

  containsCouponWithCode(expectedCode: string): ThenBrowseCoupons {
    this._assertions.push((result) => {
      const found = result.coupons.some((c) => c.code === expectedCode);
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

// === SUCCESS PATH ===

class ThenSuccess implements PromiseLike<void> {
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

class ThenSuccessAnd implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

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

  hasAppliedCouponCode(code: string): ThenOrder {
    this.stage._addOrderAssertion((order) => {
      expect(order.appliedCouponCode).toBe(code);
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

// === FAILURE PATH ===

class ThenFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenResultStage) {}

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
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
  ) {}

  clock(): ThenContractClock {
    return new ThenContractClock(this);
  }

  product(sku: string): ThenContractProduct {
    return new ThenContractProduct(this, sku);
  }

  _addClockAssertion(fn: (time: GetTimeResponse) => void): void {
    this._clockAssertions.push(fn);
  }

  _addProductAssertion(sku: string, fn: (product: GetProductResponse) => void): void {
    if (!this._productAssertions.has(sku)) this._productAssertions.set(sku, []);
    this._productAssertions.get(sku)!.push(fn);
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
      await this.app.erpDriver.returnsProduct({ sku: pc.sku, price: pc.price });
    }

    if (this._clockAssertions.length > 0) {
      const timeResult = await this.app.clockDriver.getTime();
      expect(timeResult.success).toBe(true);
      if (timeResult.success) {
        for (const fn of this._clockAssertions) fn(timeResult.value);
      }
    }

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

class ThenContractProduct implements PromiseLike<void> {
  constructor(
    private readonly stage: ThenContractStage,
    private readonly sku: string,
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
