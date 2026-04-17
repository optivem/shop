import type { Result } from '../../../../../common/result.js';
import type { ErpDriver } from '../../../../../driver/port/external/erp/erp-driver.js';
import type { ErpErrorResponse } from '../../../../../driver/port/external/erp/dtos/ErpErrorResponse.js';
import type { UseCaseContext } from '../../../use-case-context.js';
import { ReturnsProduct } from './usecases/ReturnsProduct.js';

export class ErpDsl {
  constructor(
    private readonly driver: ErpDriver,
    private readonly context: UseCaseContext,
  ) {}

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    return this.driver.goToErp();
  }

  returnsProduct(): ReturnsProduct {
    return new ReturnsProduct(this.driver, this.context);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
