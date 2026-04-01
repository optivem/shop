import { Result, success, failure } from '../common/result';
import { ErrorResponse, GetProductResponse, ReturnsProductRequest } from '../common/dtos';
import { ErpDriver } from './types';
import { JsonWireMockClient } from '../clients/wiremock-client';

export class ErpStubDriver implements ErpDriver {
  private wireMock: JsonWireMockClient;

  constructor(private baseUrl: string) {
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async goToErp(): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `ERP stub not available: ${response.status}`, fieldErrors: [] });
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products/${sku}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; sku?: string; price: number };
      return success({ sku: data.id || data.sku || sku, price: parseFloat(String(data.price)) });
    }
    return failure({ message: `Product not found: ${sku}`, fieldErrors: [] });
  }

  async returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErrorResponse>> {
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

  async close(): Promise<void> {}
}
