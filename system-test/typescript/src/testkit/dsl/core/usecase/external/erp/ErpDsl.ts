import type { ErpDriver } from '../../../../../driver/port/external/erp/erp-driver.js';
import type { UseCaseContext } from '../../../use-case-context.js';
import { ReturnsProduct } from './usecases/ReturnsProduct.js';
import { ReturnsPromotion } from './usecases/ReturnsPromotion.js';
import { GetProduct } from './usecases/GetProduct.js';
import { GoToErp } from './usecases/GoToErp.js';

export class ErpDsl {
  constructor(
    private readonly driver: ErpDriver,
    private readonly context: UseCaseContext,
  ) {}

  goToErp(): GoToErp {
    return new GoToErp(this.driver, this.context);
  }

  returnsProduct(): ReturnsProduct {
    return new ReturnsProduct(this.driver, this.context);
  }

  returnsPromotion(): ReturnsPromotion {
    return new ReturnsPromotion(this.driver, this.context);
  }

  getProduct(): GetProduct {
    return new GetProduct(this.driver, this.context);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
