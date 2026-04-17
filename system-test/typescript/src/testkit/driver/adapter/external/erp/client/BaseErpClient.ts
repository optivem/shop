import { Result, success, failure } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/ErpErrorResponse.js';
import type { GetProductResponse } from '../../../../port/external/erp/dtos/GetProductResponse.js';
import type { ExtProductDetailsResponse } from './dtos/ExtProductDetailsResponse.js';

export abstract class BaseErpClient {
  constructor(protected readonly baseUrl: string) {}

  async checkHealth(): Promise<Result<void, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `ERP not available: ${response.status}` });
  }

  async getProduct(sku: string): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products/${sku}`);
    if (response.ok) {
      const data = (await response.json()) as ExtProductDetailsResponse;
      return success({ sku: data.id || data.sku || sku, price: Number.parseFloat(String(data.price)) });
    }
    return failure({ message: `Product not found: ${sku}` });
  }
}
