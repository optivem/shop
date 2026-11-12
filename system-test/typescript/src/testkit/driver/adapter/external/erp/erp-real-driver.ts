import type { Result } from '../../../../common/result.js';
import { success } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from '../../../port/external/erp/dtos/GetProductRequest.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsProductsRequest } from '../../../port/external/erp/dtos/ReturnsProductsRequest.js';
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

  /** The real ERP lists every product it holds, so staging a list means creating each product in it. */
  async returnsProducts(request: ReturnsProductsRequest): Promise<Result<void, ErpErrorResponse>> {
    for (const product of request.products) {
      const result = await this.returnsProduct(product);
      if (!result.success) return result;
    }
    return success(undefined);
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

  /** The real ERP cannot be slowed down on demand, so this throws for the same reason as returnsUnavailable(). */
  returnsSlow(): Promise<Result<void, ErpErrorResponse>> {
    throw new Error('ErpRealDriver cannot make the ERP slow; run this scenario against the ERP stub');
  }

  /** The real ERP is never taken down by returnsUnavailable(), so it cannot be brought back either. */
  returnsAvailable(): Promise<Result<void, ErpErrorResponse>> {
    throw new Error('ErpRealDriver cannot end an ERP outage; run this scenario against the ERP stub');
  }

  returnsProductNotFound(_request: GetProductRequest): Promise<Result<void, ErpErrorResponse>> {
    throw new Error('ErpRealDriver cannot withdraw a product; run this scenario against the ERP stub');
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}
