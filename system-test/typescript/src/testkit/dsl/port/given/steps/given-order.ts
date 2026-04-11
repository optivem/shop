import type { GivenStep } from './given-step.js';

export interface GivenOrder extends GivenStep {
  withOrderNumber(orderNumber: string): GivenOrder;
  withSku(sku: string): GivenOrder;
  withQuantity(quantity: string | number): GivenOrder;
  withCountry(country: string): GivenOrder;
  withCouponCode(couponCode: string): GivenOrder;
  withStatus(status: string): GivenOrder;
}
