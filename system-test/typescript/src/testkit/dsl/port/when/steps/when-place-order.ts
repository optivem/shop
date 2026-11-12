import type { ThenPlaceOrderResultStage } from '../../then/then-place-order-result-stage.js';

export interface WhenPlaceOrder {
  withOrderNumber(orderNumber: string): this;
  withSku(sku: string): this;
  withQuantity(quantity: string | number): this;
  withCountry(country: string): this;
  /**
   * Leaves the country as Quick order prefilled it from the saved profile (UC-010 BR-5): the UI keeps the field as
   * shown; the API gets no country.
   */
  withPrefilledCountry(): this;
  withCouponCode(couponCode?: string | null): this;
  withIdempotencyKey(idempotencyKey: string): this;
  /** Any shipping setter switches to address mode: tax country = the address's country, no top-level country sent. */
  withShippingAddress(): this;
  withShippingAddress(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this;
  withShippingFirstName(firstName: string): this;
  withShippingLastName(lastName: string): this;
  withShippingStreet(street: string): this;
  withShippingPostalCode(postalCode: string): this;
  withShippingCity(city: string): this;
  withShippingCountry(country: string): this;
  then(): ThenPlaceOrderResultStage;
}
