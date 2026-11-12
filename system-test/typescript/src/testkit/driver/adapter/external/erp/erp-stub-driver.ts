import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from '../../../port/external/erp/dtos/GetProductRequest.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsProductsRequest } from '../../../port/external/erp/dtos/ReturnsProductsRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { BaseErpDriver } from './BaseErpDriver.js';
import { ErpStubClient } from './client/ErpStubClient.js';

export class ErpStubDriver extends BaseErpDriver<ErpStubClient> {
  constructor(baseUrl: string) {
    super(new ErpStubClient(baseUrl));
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configureProduct(BaseErpDriver.toExtProductDetails(request));
  }

  async returnsProducts(request: ReturnsProductsRequest): Promise<Result<void, ErpErrorResponse>> {
    const extProducts = request.products.map((product) => BaseErpDriver.toExtProductDetails(product));
    // The ERP serves each listed product on its own as well (GET /api/products/{sku}), as the real ERP does.
    for (const extProduct of extProducts) {
      const result = await this.client.configureProduct(extProduct);
      if (!result.success) return result;
    }
    return this.client.configureGetProducts(extProducts);
  }

  async returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configurePromotion(request);
  }

  async returnsUnavailable(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configureUnavailable();
  }

  async returnsSlow(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configureSlow();
  }

  async returnsAvailable(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.removeUnavailable();
  }

  async returnsProductNotFound(request: GetProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configureGetProductNotFound(request.sku);
  }

  async close(): Promise<void> {
    await this.client.close();
  }
}
