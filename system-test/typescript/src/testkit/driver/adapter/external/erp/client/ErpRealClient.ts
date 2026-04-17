import { Result, success, failure } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/ErpErrorResponse.js';
import type { ReturnsProductRequest } from '../../../../port/external/erp/dtos/ReturnsProductRequest.js';
import { BaseErpClient } from './BaseErpClient.js';
import type { ExtCreateProductRequest } from './dtos/ExtCreateProductRequest.js';

export class ErpRealClient extends BaseErpClient {
  async createProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    const body: ExtCreateProductRequest = {
      id: request.sku,
      title: 'Test Product',
      description: 'Test Product Description',
      price: request.price,
      category: 'Test',
      brand: 'Test',
    };
    const response = await fetch(`${this.baseUrl}/api/products`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (response.ok) return success(undefined);
    return failure({ message: `Failed to create product: ${response.status}` });
  }
}
