import { OrderConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenOrder as IGivenOrder } from '../../../port/given/steps/given-order.js';

export class GivenOrder implements IGivenOrder {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: OrderConfig,
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

  withStatus(status: string): this {
    this.config.status = status;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage {
    return this.stage.then();
  }
}
