// Together with `app-context.ts`, this file fills the role that Java/.NET collapse
// into a single `ExecutionResultContext`. Here the split is deliberate:
// `scenario-context.ts` holds Given-stage scenario data (products, coupons, orders, etc.),
// and `app-context.ts` holds the active channel and driver registry.

import type { OrderStatus } from '../../../common/domain/OrderStatus.js';
import { UserIdentity } from '../../../driver/port/user-identity.js';
import { DEFAULTS } from './defaults.js';
import { CustomerAliases } from './given/customer-aliases.js';

export interface ClockConfig {
  time: string;
}

export interface ProductConfig {
  sku: string;
  price: string;
  /** Unset → the ERP driver's default. */
  title?: string;
  description?: string;
  category?: string;
  brand?: string;
}

export interface PromotionConfig {
  promotionActive: boolean;
  discount: string;
}

export interface CouponConfig {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number | string;
}

export interface CountryConfig {
  country: string;
  taxRate: string;
}

export interface OrderConfig {
  sku: string;
  quantity: string;
  country: string;
  couponCode: string | null;
  /** Alias of the Idempotency-Key the order is placed with; undefined means no key. */
  idempotencyKey?: string;
  status: OrderStatus;
  orderNumber?: string;
  /** Alias of the customer who places the order; undefined means the default customer. */
  placedByAlias?: string;
}

/** A shipping address as the scenario states it; its country is an alias, resolved like an order's country. */
export interface ShippingAddressConfig {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface ProfileConfig {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
  /** Alias of the customer who saves the profile; undefined means the default customer. */
  savedByAlias?: string;
}

export class ScenarioContext {
  clockConfig: ClockConfig | null = null;
  productConfigs: ProductConfig[] = [];
  /** Each given().products() call stages one ERP product list. */
  productListConfigs: ProductConfig[][] = [];
  couponConfigs: CouponConfig[] = [];
  countryConfigs: CountryConfig[] = [];
  orderConfigs: OrderConfig[] = [];
  profileConfigs: ProfileConfig[] = [];
  hasExplicitProduct = false;
  promotionConfig: PromotionConfig = { promotionActive: DEFAULTS.PROMOTION_ACTIVE, discount: DEFAULTS.PROMOTION_DISCOUNT };
  hasExplicitPromotion = false;
  /** When set, every ERP call fails, whatever products or promotion are staged. */
  erpUnavailable = false;
  /** When set, every ERP call answers only after the system's outbound timeout, whatever is staged. */
  erpSlow = false;
  readonly customers = new CustomerAliases();
  private loggedIn: () => UserIdentity = () => UserIdentity.DEFAULT;
  private loggedInChosen = false;

  constructor(private readonly onExecuted?: () => void) {}

  /** Chooses who the scenario's action is performed as; resolved lazily so aliases are assigned in order of first use. */
  logInAs(identity: () => UserIdentity): void {
    this.loggedIn = identity;
    this.loggedInChosen = true;
  }

  /** Operations with no explicit customer run as the default customer, so customer1 must stay theirs. */
  reserveDefaultCustomerIfUsed(): void {
    if (
      !this.loggedInChosen ||
      this.orderConfigs.some((order) => order.placedByAlias === undefined) ||
      this.profileConfigs.some((profile) => profile.savedByAlias === undefined)
    ) {
      this.customers.reserveDefaultCustomer();
    }
  }

  /** The identity the scenario's action runs as. */
  loggedInIdentity(): UserIdentity {
    return this.loggedIn();
  }

  /** The identity that places the given order. */
  placingIdentity(order: OrderConfig): UserIdentity {
    return order.placedByAlias === undefined ? this.customers.defaultCustomer() : this.customers.resolve(order.placedByAlias);
  }

  /** The identity that saves the given profile. */
  savingIdentity(profile: ProfileConfig): UserIdentity {
    return profile.savedByAlias === undefined ? this.customers.defaultCustomer() : this.customers.resolve(profile.savedByAlias);
  }

  /** Signals that the scenario's action has been initiated (called from each When-step's `then()`). */
  markExecuted(): void {
    this.onExecuted?.();
  }
}
