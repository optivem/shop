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

  returnsPromotion(_request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    return Promise.resolve(success(undefined));
  }

  /**
   * The real ERP cannot be taken down on demand, so this throws rather than silently succeeding,
   * which would let an ERP-unavailable test pass for the wrong reason.
   */
  returnsUnavailable(): Promise<Result<void, ErpErrorResponse>> {
    throw new Error('ErpRealDriver cannot make the ERP unavailable; run this scenario against the ERP stub');
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}
