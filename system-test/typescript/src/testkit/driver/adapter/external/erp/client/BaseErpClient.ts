import { type Result, success, failure } from '../../../../../common/result.js';
import type { ErpErrorResponse } from '../../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { ExtGetImageResponse } from './dtos/ExtGetImageResponse.js';
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

  /**
   * Fetches a product image by the URL the ERP gave on the product (root-relative, e.g. "/erp/images/HP-15.jpg"),
   * resolved against the base URL the way a browser resolves a link.
   */
  async getImage(imageUrl: string): Promise<Result<ExtGetImageResponse, ErpErrorResponse>> {
    const url = new URL(imageUrl, this.baseUrl);
    const response = await fetch(url);
    if (response.ok) {
      const body = await response.arrayBuffer();
      return success({ contentType: response.headers.get('Content-Type'), sizeInBytes: body.byteLength });
    }
    return failure({ message: `Failed to get image ${url.href}: ${response.status}` });
  }
}
