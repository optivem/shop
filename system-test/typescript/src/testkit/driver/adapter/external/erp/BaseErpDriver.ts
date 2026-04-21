import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from '../../../port/external/erp/dtos/GetProductRequest.js';
import type { GetProductResponse } from '../../../port/external/erp/dtos/GetProductResponse.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import type { ErpDriver } from '../../../port/external/erp/erp-driver.js';
import type { BaseErpClient } from './client/BaseErpClient.js';

export abstract class BaseErpDriver<TClient extends BaseErpClient> implements ErpDriver {
  protected constructor(protected readonly client: TClient) {}

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.checkHealth();
  }

  async getProduct(request: GetProductRequest): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    return this.client.getProduct(request.sku);
  }

  abstract returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>>;
  abstract returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>>;
  abstract close(): Promise<void>;
}
