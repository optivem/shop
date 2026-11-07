import { type Result, success } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { ReturnsPromotionRequest } from '../../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { JsonWireMockClient } from '../../../shared/client/wiremock/json-wiremock-client.js';
import { BaseErpClient } from './BaseErpClient.js';
import type { ExtProductDetailsResponse } from './dtos/ExtProductDetailsResponse.js';
import type { ExtGetPromotionResponse } from './dtos/ExtGetPromotionResponse.js';

export class ErpStubClient extends BaseErpClient {
  private readonly wireMock: JsonWireMockClient;

  constructor(baseUrl: string) {
    super(baseUrl);
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async configureProduct(product: ExtProductDetailsResponse): Promise<Result<void, ErpErrorResponse>> {
    await this.wireMock.stubGet(`/erp/api/products/${product.id}`, product);
    return success(undefined);
  }

  async configureGetProducts(products: ExtProductDetailsResponse[]): Promise<Result<void, ErpErrorResponse>> {
    await this.wireMock.stubGet('/erp/api/products', products);
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

  /** Every ERP API call answers 503, outranking any product or promotion stubs. */
  async configureUnavailable(): Promise<Result<void, ErpErrorResponse>> {
    await this.wireMock.stubGetMatching('/erp/api/.*', 503);
    return success(undefined);
  }

  async close(): Promise<void> {
    await this.wireMock.removeStubs();
  }
}
