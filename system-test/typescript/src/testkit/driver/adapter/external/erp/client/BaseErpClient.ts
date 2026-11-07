import { type Result, success, failure } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { ExtProductDetailsResponse } from './dtos/ExtProductDetailsResponse.js';

export abstract class BaseErpClient {
  constructor(protected readonly baseUrl: string) {}

  async checkHealth(): Promise<Result<void, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `ERP not available: ${response.status}` });
  }

  async getProduct(sku: string): Promise<Result<ExtProductDetailsResponse, ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products/${sku}`);
    if (response.ok) {
      return success((await response.json()) as ExtProductDetailsResponse);
    }
    return failure({ message: `Product not found: ${sku}` });
  }

  async getProducts(): Promise<Result<ExtProductDetailsResponse[], ErpErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/products`);
    if (response.ok) {
      return success((await response.json()) as ExtProductDetailsResponse[]);
    }
    return failure({ message: `Failed to list products: ${response.status}` });
  }
}
