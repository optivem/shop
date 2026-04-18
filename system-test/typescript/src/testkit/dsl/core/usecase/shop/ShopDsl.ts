import type { ShopDriver } from '../../../../driver/port/shop/shop-driver.js';
import type { UseCaseContext } from '../../use-case-context.js';
import { PlaceOrder } from './usecases/PlaceOrder.js';
import { ViewOrder } from './usecases/ViewOrder.js';
import { CancelOrder } from './usecases/CancelOrder.js';
import { DeliverOrder } from './usecases/DeliverOrder.js';
import { BrowseCoupons } from './usecases/BrowseCoupons.js';
import { PublishCoupon } from './usecases/PublishCoupon.js';
import { GoToShop } from './usecases/GoToShop.js';

export class ShopDsl {
  constructor(
    private readonly driver: ShopDriver,
    private readonly context: UseCaseContext,
  ) {}

  goToShop(): GoToShop {
    return new GoToShop(this.driver, this.context);
  }

  placeOrder(): PlaceOrder {
    return new PlaceOrder(this.driver, this.context);
  }

  viewOrder(): ViewOrder {
    return new ViewOrder(this.driver, this.context);
  }

  cancelOrder(): CancelOrder {
    return new CancelOrder(this.driver, this.context);
  }

  deliverOrder(): DeliverOrder {
    return new DeliverOrder(this.driver, this.context);
  }

  browseCoupons(): BrowseCoupons {
    return new BrowseCoupons(this.driver, this.context);
  }

  publishCoupon(): PublishCoupon {
    return new PublishCoupon(this.driver, this.context);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
