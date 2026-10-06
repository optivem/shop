import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { WhenPlaceOrder } from './when-place-order.js';
import { WhenCancelOrder } from './when-cancel-order.js';
import { WhenDeliverOrder } from './when-deliver-order.js';
import { WhenBrowseOrderHistory } from './when-browse-order-history.js';
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

  deliverOrder(): WhenDeliverOrder {
    return new WhenDeliverOrder(this.app, this.ctx, this.useCaseContext);
  }

  browseOrderHistory(): WhenBrowseOrderHistory {
    return new WhenBrowseOrderHistory(this.app, this.ctx, this.useCaseContext);
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
