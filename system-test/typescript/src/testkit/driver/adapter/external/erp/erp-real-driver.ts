import type { Result } from '../../../../common/result.js';
import { success } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/ErpErrorResponse.js';
import type { GetProductResponse } from '../../../port/external/erp/dtos/GetProductResponse.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import type { ErpDriver } from '../../../port/external/erp/erp-driver.js';
import { ErpRealClient } from './client/ErpRealClient.js';

export class ErpRealDriver implements ErpDriver {
  private readonly client: ErpRealClient;

  constructor(baseUrl: string) {
    this.client = new ErpRealClient(baseUrl);
  }

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.checkHealth();
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    return this.client.getProduct(sku);
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.createProduct(request);
  }

  async returnsPromotion(_request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    return success(undefined);
  }

  async close(): Promise<void> {}
}
