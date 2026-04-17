import { Result, success, failure } from '../../../../common/result.js';
import { ErpErrorResponse } from '../../../port/external/erp/dtos/ErpErrorResponse.js';
import { GetProductResponse } from '../../../port/external/erp/dtos/GetProductResponse.js';
import { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import { ErpDriver } from '../../../port/external/erp/erp-driver.js';
import { JsonWireMockClient } from '../../shared/wiremock/wiremock-client.js';

export class ErpStubDriver implements ErpDriver {
  private wireMock: JsonWireMockClient;

  constructor(private baseUrl: string) {
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `ERP stub not available: ${response.status}` });
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products/${sku}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; sku?: string; price: number };
      return success({ sku: data.id || data.sku || sku, price: parseFloat(String(data.price)) });
    }
    return failure({ message: `Product not found: ${sku}` });
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    await this.wireMock.stubGet(`/erp/api/products/${request.sku}`, {
      id: request.sku,
      title: 'Test Product',
      description: 'Test Product Description',
      price: parseFloat(request.price),
      category: 'Test',
      brand: 'Test',
    });
    return success(undefined);
  }

  async returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>> {
    await this.wireMock.stubGet('/erp/api/promotion', {
      promotionActive: request.promotionActive,
      discount: Number.parseFloat(request.discount),
    });
    return success(undefined);
  }

  async close(): Promise<void> {
    await this.wireMock.removeStubs();
  }
}
