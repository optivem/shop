import { type Result, success, failure } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { ReturnsProductRequest } from '../../../../port/external/erp/dtos/ReturnsProductRequest.js';
import { ErpProductDefaults } from '../erp-product-defaults.js';
import { BaseErpClient } from './BaseErpClient.js';
import type { ExtCreateProductRequest } from './dtos/ExtCreateProductRequest.js';

export class ErpRealClient extends BaseErpClient {
  async createProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>> {
    const body: ExtCreateProductRequest = {
      id: request.sku,
      title: request.title ?? ErpProductDefaults.TITLE,
      description: request.description ?? ErpProductDefaults.DESCRIPTION,
      price: request.price,
      category: request.category ?? ErpProductDefaults.CATEGORY,
      brand: request.brand ?? ErpProductDefaults.BRAND,
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
