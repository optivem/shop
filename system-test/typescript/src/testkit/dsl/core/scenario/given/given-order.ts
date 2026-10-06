import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';
import type { OrderConfig } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { CustomerAliases } from './customer-aliases.js';
import type { GivenOrder as IGivenOrder } from '../../../port/given/steps/given-order.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class GivenOrder implements IGivenOrder {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: OrderConfig,
    private readonly customers: CustomerAliases,
  ) {}

  withOrderNumber(orderNumber: string): this {
    this.config.orderNumber = orderNumber;
    return this;
  }

  withSku(sku: string): this {
    this.config.sku = sku;
    return this;
  }

  withQuantity(quantity: string | number): this {
    this.config.quantity = String(quantity);
    return this;
  }

  withCountry(country: string): this {
    this.config.country = country;
    return this;
  }

  withCouponCode(couponCode: string | null): this {
    this.config.couponCode = couponCode;
    return this;
  }

  /** The order is placed by the customer known in this scenario by the given alias, or by the default customer (customer1) when no alias is given. */
  placedByCustomer(alias?: string): this {
    if (alias === undefined) {
      this.config.placedByAlias = undefined;
    } else {
      this.customers.register(alias);
      this.config.placedByAlias = alias;
    }
    return this;
  }

  withStatus(status: OrderStatus): this {
    this.config.status = status;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage;
  then(...args: unknown[]): ThenContractStage {
    assertNotAwaited(args);
    return this.stage.then();
  }
}
