import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/ErpErrorResponse.js';
import type { GetProductResponse } from '../../../port/external/erp/dtos/GetProductResponse.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import type { ErpDriver } from '../../../port/external/erp/erp-driver.js';
import { ErpStubClient } from './client/ErpStubClient.js';

export class ErpStubDriver implements ErpDriver {
  private readonly client: ErpStubClient;

  constructor(baseUrl: string) {
    this.client = new ErpStubClient(baseUrl);
  }

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.checkHealth();
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    return this.client.getProduct(sku);
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configureProduct(request);
  }

  async returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configurePromotion(request);
  }

  async close(): Promise<void> {
    await this.client.close();
  }
}
