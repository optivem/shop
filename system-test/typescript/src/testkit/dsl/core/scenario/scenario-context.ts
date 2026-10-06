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
  status: OrderStatus;
  orderNumber?: string;
  /** Alias of the customer who places the order; undefined means the default customer. */
  placedByAlias?: string;
}

export class ScenarioContext {
  clockConfig: ClockConfig | null = null;
  productConfigs: ProductConfig[] = [];
  couponConfigs: CouponConfig[] = [];
  countryConfigs: CountryConfig[] = [];
  orderConfigs: OrderConfig[] = [];
  hasExplicitProduct = false;
  promotionConfig: PromotionConfig = { promotionActive: DEFAULTS.PROMOTION_ACTIVE, discount: DEFAULTS.PROMOTION_DISCOUNT };
  hasExplicitPromotion = false;
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
    if (!this.loggedInChosen || this.orderConfigs.some((order) => order.placedByAlias === undefined)) {
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

  /** Signals that the scenario's action has been initiated (called from each When-step's `then()`). */
  markExecuted(): void {
    this.onExecuted?.();
  }
}
