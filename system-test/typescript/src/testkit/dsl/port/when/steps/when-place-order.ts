import type { ThenResultStage } from '../../then/then-result-stage.js';

export interface WhenPlaceOrder {
  withOrderNumber(orderNumber: string): this;
  withSku(sku: string): this;
  withQuantity(quantity: string | number): this;
  withCountry(country: string): this;
  withCouponCode(couponCode?: string | null): this;
  withIdempotencyKey(idempotencyKey: string): this;
  then(): ThenResultStage;
}
