import type { Result } from '../../../../common/result.js';
import type { ShopDriver } from '../../../../driver/port/shop/shop-driver.js';
import type { SystemError } from '../../../../driver/port/shop/dtos/SystemError.js';
import type { UseCaseContext } from '../../use-case-context.js';
import { PlaceOrder } from './usecases/PlaceOrder.js';
import { ViewOrder } from './usecases/ViewOrder.js';

export class ShopDsl {
  constructor(
    private readonly driver: ShopDriver,
    private readonly context: UseCaseContext,
  ) {}

  async goToShop(): Promise<Result<void, SystemError>> {
    return this.driver.goToShop();
  }

  placeOrder(): PlaceOrder {
    return new PlaceOrder(this.driver, this.context);
  }

  viewOrder(): ViewOrder {
    return new ViewOrder(this.driver, this.context);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
