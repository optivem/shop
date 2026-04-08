import { OrderConfig } from '../scenario-context.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';

export class GivenOrder {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: OrderConfig,
  ) {}

  withOrderNumber(orderNumber: string): GivenOrder {
    this.config.orderNumber = orderNumber;
    return this;
  }

  withSku(sku: string): GivenOrder {
    this.config.sku = sku;
    return this;
  }

  withQuantity(quantity: string | number): GivenOrder {
    this.config.quantity = String(quantity);
    return this;
  }

  withCountry(country: string): GivenOrder {
    this.config.country = country;
    return this;
  }

  withCouponCode(couponCode: string | null): GivenOrder {
    this.config.couponCode = couponCode;
    return this;
  }

  withStatus(status: string): GivenOrder {
    this.config.status = status;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }
}
