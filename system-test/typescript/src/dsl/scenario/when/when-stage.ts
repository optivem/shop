import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { WhenPlaceOrder } from './when-place-order.js';
import { WhenCancelOrder } from './when-cancel-order.js';
import { WhenViewOrder } from './when-view-order.js';
import { WhenPublishCoupon } from './when-publish-coupon.js';
import { WhenBrowseCoupons } from './when-browse-coupons.js';

export class WhenStage {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  placeOrder(): WhenPlaceOrder {
    return new WhenPlaceOrder(this.app, this.ctx, this.useCaseContext);
  }

  cancelOrder(): WhenCancelOrder {
    return new WhenCancelOrder(this.app, this.ctx, this.useCaseContext);
  }

  viewOrder(): WhenViewOrder {
    return new WhenViewOrder(this.app, this.ctx, this.useCaseContext);
  }

  publishCoupon(): WhenPublishCoupon {
    return new WhenPublishCoupon(this.app, this.ctx, this.useCaseContext);
  }

  browseCoupons(): WhenBrowseCoupons {
    return new WhenBrowseCoupons(this.app, this.ctx, this.useCaseContext);
  }
}
