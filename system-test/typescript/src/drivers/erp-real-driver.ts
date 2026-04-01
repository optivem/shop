import { Result, success, failure } from '../common/result';
import { ErrorResponse, GetProductResponse, ReturnsProductRequest } from '../common/dtos';
import { ErpDriver } from './types';

export class ErpRealDriver implements ErpDriver {
  constructor(private baseUrl: string) {}

  async goToErp(): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `ERP not available: ${response.status}`, fieldErrors: [] });
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
    return failure({ message: `Failed to create product: ${response.status}`, fieldErrors: [] });
  }

  async close(): Promise<void> {}
}
