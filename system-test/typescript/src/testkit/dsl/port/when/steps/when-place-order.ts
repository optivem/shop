import type { WhenStep } from './when-step.js';

export interface WhenPlaceOrder extends WhenStep {
  withOrderNumber(orderNumber: string): WhenPlaceOrder;
  withSku(sku: string): WhenPlaceOrder;
  withQuantity(quantity: string | number): WhenPlaceOrder;
  withCountry(country: string): WhenPlaceOrder;
  withCouponCode(couponCode?: string): WhenPlaceOrder;
}
