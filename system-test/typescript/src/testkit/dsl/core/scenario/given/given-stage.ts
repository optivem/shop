import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext, ProductConfig, CouponConfig, CountryConfig, OrderConfig } from '../scenario-context.js';
import { WhenStage } from '../when/when-stage.js';
import { ThenContractStage } from '../then/then-contract.js';
import { GivenClock } from './given-clock.js';
import { GivenProduct } from './given-product.js';
import { GivenPromotion } from './given-promotion.js';
import { GivenCoupon } from './given-coupon.js';
import { GivenCountry } from './given-country.js';
import { GivenOrder } from './given-order.js';
import type { GivenStage as IGivenStage } from '../../../port/given/given-stage.js';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class GivenStage implements IGivenStage {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  clock(): GivenClock {
    this.ctx.clockConfig = { time: DEFAULTS.CLOCK_TIME };
    return new GivenClock(this, this.ctx.clockConfig);
  }

  product(): GivenProduct {
    const config: ProductConfig = { sku: DEFAULTS.SKU, price: DEFAULTS.UNIT_PRICE };
    this.ctx.productConfigs.push(config);
    this.ctx.hasExplicitProduct = true;
    return new GivenProduct(this, config);
  }

  promotion(): GivenPromotion {
    this.ctx.hasExplicitPromotion = true;
    return new GivenPromotion(this, this.ctx.promotionConfig);
  }

  coupon(): GivenCoupon {
    const config: CouponConfig = { code: DEFAULTS.COUPON_CODE, discountRate: 0.1 };
    this.ctx.couponConfigs.push(config);
    return new GivenCoupon(this, config);
  }

  country(): GivenCountry {
    const config: CountryConfig = { country: DEFAULTS.COUNTRY, taxRate: DEFAULTS.TAX_RATE };
    this.ctx.countryConfigs.push(config);
    return new GivenCountry(this, config);
  }

  order(): GivenOrder {
    const config: OrderConfig = {
      sku: DEFAULTS.SKU,
      quantity: DEFAULTS.QUANTITY,
      country: DEFAULTS.COUNTRY,
      couponCode: null,
      status: DEFAULTS.ORDER_STATUS,
    };
    this.ctx.orderConfigs.push(config);
    return new GivenOrder(this, config, this.ctx.customers);
  }

  /** Logs in as the customer known in this scenario by the given alias, or as the default customer (customer1) when no alias is given. */
  loggedInAsCustomer(alias?: string): this {
    const customers = this.ctx.customers;
    if (alias === undefined) {
      customers.reserveDefaultCustomer();
      this.ctx.logInAs(() => customers.defaultCustomer());
    } else {
      customers.register(alias);
      this.ctx.logInAs(() => customers.resolve(alias));
    }
    return this;
  }

  loggedInAsAdmin(): this {
    this.ctx.logInAs(() => UserIdentity.ADMIN);
    return this;
  }

  notLoggedIn(): this {
    this.ctx.logInAs(() => UserIdentity.ANONYMOUS);
    return this;
  }

  and(): this {
    return this;
  }

  when(): WhenStage {
    return new WhenStage(this.app, this.ctx, this.useCaseContext);
  }

  then(): ThenContractStage;
  then(...args: unknown[]): ThenContractStage {
    assertNotAwaited(args);
    return new ThenContractStage(this.app, this.ctx, this.useCaseContext);
  }
}
