import { Result, success, failure } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/ErpErrorResponse.js';
import type { GetProductResponse } from '../../../../port/external/erp/dtos/GetProductResponse.js';
import type { ReturnsProductRequest } from '../../../../port/external/erp/dtos/ReturnsProductRequest.js';

export class ErpRealClient {
  constructor(private baseUrl: string) {}

  async checkHealth(): Promise<Result<void, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `ERP not available: ${response.status}` });
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products/${sku}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; sku?: string; price: number };
      return success({ sku: data.id || data.sku || sku, price: parseFloat(String(data.price)) });
    }
    return failure({ message: `Product not found: ${sku}` });
  }

  async createProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: request.sku,
        title: 'Test Product',
        description: 'Test Product Description',
        price: request.price,
        category: 'Test',
        brand: 'Test',
      }),
    });
    if (response.ok) return success(undefined);
    return failure({ message: `Failed to create product: ${response.status}` });
  }
}
