import { DEFAULTS } from '../defaults.js';
import { UseCaseContext } from '../../shared/use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext, ProductConfig, CouponConfig, CountryConfig, OrderConfig } from '../scenario-context.js';
import { WhenStage } from '../when/when-stage.js';
import { ThenContractStage } from '../then/then-contract.js';
import { GivenClock } from './given-clock.js';
import { GivenProduct } from './given-product.js';
import { GivenPromotion } from './given-promotion.js';
import { GivenCoupon } from './given-coupon.js';
import { GivenCountry } from './given-country.js';
import { GivenOrder } from './given-order.js';
import type { GivenStage as IGivenStage } from '../../../port/given/given-stage.js';

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
    return new GivenOrder(this, config);
  }

  and(): GivenStage {
    return this;
  }

  when(): WhenStage {
    return new WhenStage(this.app, this.ctx, this.useCaseContext);
  }

  then(): ThenContractStage {
    return new ThenContractStage(this.app, this.ctx, this.useCaseContext);
  }
}
