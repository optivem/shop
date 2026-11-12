import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from '../../../port/external/erp/dtos/GetProductRequest.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsProductsRequest } from '../../../port/external/erp/dtos/ReturnsProductsRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { BaseErpDriver } from './BaseErpDriver.js';
import { ErpStubClient } from './client/ErpStubClient.js';

/**
 * How long to wait once an ERP outage (unavailable or slow) ends. The system's ERP circuit breaker may have opened
 * during the outage, or reopened on its last slow call just before the test got its answer; until its open time
 * passes (100-500 ms on the stub stacks) it still turns ERP calls away with 503, so without the wait the outage would
 * leak into the next step or test.
 */
const OUTAGE_RECOVERY_MILLISECONDS = 1_000;

export class ErpStubDriver extends BaseErpDriver<ErpStubClient> {
  private outage = false;

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
    this.outage = true;
    return this.client.configureUnavailable();
  }

  async returnsSlow(): Promise<Result<void, ErpErrorResponse>> {
    this.outage = true;
    return this.client.configureSlow();
  }

  async returnsAvailable(): Promise<Result<void, ErpErrorResponse>> {
    const result = await this.client.removeUnavailable();
    await this.awaitRecoveryFromOutage();
    return result;
  }

  async returnsProductNotFound(request: GetProductRequest): Promise<Result<void, ErpErrorResponse>> {
    return this.client.configureGetProductNotFound(request.sku);
  }

  async close(): Promise<void> {
    await this.client.close();
    await this.awaitRecoveryFromOutage();
  }

  private async awaitRecoveryFromOutage(): Promise<void> {
    if (this.outage) {
      this.outage = false;
      await new Promise((resolve) => setTimeout(resolve, OUTAGE_RECOVERY_MILLISECONDS));
    }
  }
}
