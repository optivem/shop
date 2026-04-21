import type { Result } from '../../../../common/result.js';
import { success } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { BaseErpDriver } from './BaseErpDriver.js';
import { ErpRealClient } from './client/ErpRealClient.js';

export class ErpRealDriver extends BaseErpDriver<ErpRealClient> {
  constructor(baseUrl: string) {
    super(new ErpRealClient(baseUrl));
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.createProduct(request);
  }

  async returnsPromotion(_request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    return success(undefined);
  }

  async close(): Promise<void> {}
}
