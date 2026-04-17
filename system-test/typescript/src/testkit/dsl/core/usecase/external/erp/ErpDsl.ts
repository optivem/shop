import type { Result } from '../../../../../common/result.js';
import type { ErpDriver } from '../../../../../driver/port/external/erp/erp-driver.js';
import type { ErpErrorResponse } from '../../../../../driver/port/external/erp/dtos/ErpErrorResponse.js';
import type { GetProductResponse } from '../../../../../driver/port/external/erp/dtos/GetProductResponse.js';
import type { ReturnsProductRequest } from '../../../../../driver/port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../../../driver/port/external/erp/dtos/ReturnsPromotionRequest.js';

export class ErpDsl {
  constructor(private readonly driver: ErpDriver) {}

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    return this.driver.goToErp();
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    return this.driver.getProduct(sku);
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.driver.returnsProduct(request);
  }

  async returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.driver.returnsPromotion(request);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
