import { Result, success } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/ErpErrorResponse.js';
import type { ReturnsProductRequest } from '../../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from '../../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { JsonWireMockClient } from '../../../shared/wiremock/wiremock-client.js';
import { BaseErpClient } from './BaseErpClient.js';
import type { ExtProductDetailsResponse } from './dtos/ExtProductDetailsResponse.js';
import type { ExtGetPromotionResponse } from './dtos/ExtGetPromotionResponse.js';

export class ErpStubClient extends BaseErpClient {
  private readonly wireMock: JsonWireMockClient;

  constructor(baseUrl: string) {
    super(baseUrl);
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async configureProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    const stubBody: ExtProductDetailsResponse = {
      id: request.sku,
      title: 'Test Product',
      description: 'Test Product Description',
      price: Number.parseFloat(request.price),
      category: 'Test',
      brand: 'Test',
    };
    await this.wireMock.stubGet(`/erp/api/products/${request.sku}`, stubBody);
    return success(undefined);
  }

  async configurePromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    const stubBody: ExtGetPromotionResponse = {
      promotionActive: request.promotionActive,
      discount: Number.parseFloat(request.discount),
    };
    await this.wireMock.stubGet('/erp/api/promotion', stubBody);
    return success(undefined);
  }

  async close(): Promise<void> {
    await this.wireMock.removeStubs();
  }
}
