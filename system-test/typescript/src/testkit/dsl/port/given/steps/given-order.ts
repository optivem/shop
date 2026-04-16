import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenOrder {
  withOrderNumber(orderNumber: string): GivenOrder;
  withSku(sku: string): GivenOrder;
  withQuantity(quantity: string | number): GivenOrder;
  withCountry(country: string): GivenOrder;
  withCouponCode(couponCode: string | null): GivenOrder;
  withStatus(status: string): GivenOrder;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
