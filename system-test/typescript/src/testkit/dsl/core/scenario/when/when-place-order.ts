import { DEFAULTS } from '../defaults.js';
import type { ShippingAddressConfig } from '../scenario-context.js';
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
  private idempotencyKey: string | null = null;
  private shippingAddress: ShippingAddressConfig | null = null;

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

  withPrefilledCountry(): this {
    return this.withCountry(null);
  }

  withCouponCode(couponCode?: string | null): this {
    this.couponCode = couponCode === undefined ? DEFAULTS.COUPON_CODE : couponCode;
    return this;
  }

  withIdempotencyKey(idempotencyKey: string): this {
    this.idempotencyKey = idempotencyKey;
    return this;
  }

  /**
   * Places the order with a shipping address: the default one, or the given one. Any shipping setter switches the
   * request to address mode (tax country = the address's country; no top-level country is sent).
   */
  withShippingAddress(): this;
  withShippingAddress(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this;
  withShippingAddress(firstName?: string, lastName?: string, street?: string, postalCode?: string, city?: string, country?: string): this {
    const address = this.address();
    if (firstName !== undefined) address.firstName = firstName;
    if (lastName !== undefined) address.lastName = lastName;
    if (street !== undefined) address.street = street;
    if (postalCode !== undefined) address.postalCode = postalCode;
    if (city !== undefined) address.city = city;
    if (country !== undefined) address.country = country;
    return this;
  }

  withShippingFirstName(firstName: string): this {
    this.address().firstName = firstName;
    return this;
  }

  withShippingLastName(lastName: string): this {
    this.address().lastName = lastName;
    return this;
  }

  withShippingStreet(street: string): this {
    this.address().street = street;
    return this;
  }

  withShippingPostalCode(postalCode: string): this {
    this.address().postalCode = postalCode;
    return this;
  }

  withShippingCity(city: string): this {
    this.address().city = city;
    return this;
  }

  withShippingCountry(country: string): this {
    this.address().country = country;
    return this;
  }

  private address(): ShippingAddressConfig {
    this.shippingAddress ??= {
      firstName: DEFAULTS.FIRST_NAME,
      lastName: DEFAULTS.LAST_NAME,
      street: DEFAULTS.STREET,
      postalCode: DEFAULTS.POSTAL_CODE,
      city: DEFAULTS.CITY,
      country: DEFAULTS.COUNTRY,
    };
    return this.shippingAddress;
  }

  then(): ThenResultStage;
  then(...args: unknown[]): ThenResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenResultStage(
      this.app,
      this.ctx,
      this.useCaseContext,
      this.sku,
      this.quantity,
      this.country,
      this.couponCode,
      this.idempotencyKey,
      this.shippingAddress,
    );
  }
}
