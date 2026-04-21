import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { BaseErpDriver } from './BaseErpDriver.js';
import { ErpStubClient } from './client/ErpStubClient.js';

export class ErpStubDriver extends BaseErpDriver<ErpStubClient> {
  constructor(baseUrl: string) {
    super(new ErpStubClient(baseUrl));
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
