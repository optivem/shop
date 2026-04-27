import { DEFAULTS } from '../defaults.js';
import { UseCaseContext } from '../../shared/use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { ThenResultStage } from '../then/then-place-order.js';

export class WhenPlaceOrder {
  private sku: string = DEFAULTS.SKU;
  private quantity: string | null = DEFAULTS.QUANTITY;
  private country: string = DEFAULTS.COUNTRY;
  private couponCode: string | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withOrderNumber(_orderNumber: string): this {
    return this;
  }

  withSku(sku: string | null): this {
    this.sku = sku as string;
    return this;
  }

  withQuantity(quantity: string | number | null): this {
    this.quantity = quantity === null ? null : String(quantity);
    return this;
  }

  withCountry(country: string | null): this {
    this.country = country as string;
    return this;
  }

  withCouponCode(couponCode?: string | null): this {
    this.couponCode = couponCode === undefined ? DEFAULTS.COUPON_CODE : couponCode;
    return this;
  }

  then(): ThenResultStage {
    return new ThenResultStage(this.app, this.ctx, this.useCaseContext, this.sku, this.quantity, this.country, this.couponCode);
  }
}
