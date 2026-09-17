import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenResultStage } from '../then/then-place-order.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenPlaceOrder {
  private sku: string | null = DEFAULTS.SKU;
  private quantity: string | null = DEFAULTS.QUANTITY;
  private country: string | null = DEFAULTS.COUNTRY;
  private couponCode: string | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  // Accepted so scenarios read the same as their Java/.NET twins, where the order number is the
  // alias the placed order is stored under. Here Then reads the number off the place-order result,
  // so there is nothing to store.
  withOrderNumber(_orderNumber: string): this {
    return this;
  }

  withSku(sku: string | null): this {
    this.sku = sku;
    return this;
  }

  withQuantity(quantity: string | number | null): this {
    this.quantity = quantity === null ? null : String(quantity);
    return this;
  }

  withCountry(country: string | null): this {
    this.country = country;
    return this;
  }

  withCouponCode(couponCode?: string | null): this {
    this.couponCode = couponCode === undefined ? DEFAULTS.COUPON_CODE : couponCode;
    return this;
  }

  then(): ThenResultStage;
  then(...args: unknown[]): ThenResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenResultStage(this.app, this.ctx, this.useCaseContext, this.sku, this.quantity, this.country, this.couponCode);
  }
}
