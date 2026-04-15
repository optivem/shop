import { DEFAULTS } from '../../defaults.js';
import { UseCaseContext } from '../../use-case-context.js';
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

  withOrderNumber(_orderNumber: string): WhenPlaceOrder {
    return this;
  }

  withSku(sku: string | null): WhenPlaceOrder {
    this.sku = sku as string;
    return this;
  }

  withQuantity(quantity: string | number | null): WhenPlaceOrder {
    this.quantity = quantity === null ? null : String(quantity);
    return this;
  }

  withCountry(country: string | null): WhenPlaceOrder {
    this.country = country as string;
    return this;
  }

  withCouponCode(couponCode: string): WhenPlaceOrder {
    this.couponCode = couponCode;
    return this;
  }

  then(): ThenResultStage {
    return new ThenResultStage(this.app, this.ctx, this.useCaseContext, this.sku, this.quantity, this.country, this.couponCode);
  }
}
