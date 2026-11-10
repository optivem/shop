// Service layer for the public catalog API (no sign-in needed).

import { fetchJson } from '../common';
import { isProduct, isProductList, type ProductPayload } from '../types/api.guards';
import type { Product } from '../types/api.types';
import type { Result } from '../types/result.types';

function toProduct(payload: ProductPayload): Product {
  return {
    sku: payload.sku,
    title: payload.title,
    description: payload.description ?? '',
    brand: payload.brand,
    category: payload.category,
    price: typeof payload.price === 'number' ? payload.price.toFixed(2) : payload.price,
  };
}

export class ProductGateway {
  private readonly baseUrl: string;

  constructor(baseUrl = '/api/products') {
    this.baseUrl = baseUrl;
  }

  async browseProducts(): Promise<Result<Product[]>> {
    const result = await fetchJson(this.baseUrl, isProductList, { method: 'GET' });
    return result.success ? { success: true, data: result.data.map(toProduct) } : result;
  }

  async viewProduct(sku: string): Promise<Result<Product>> {
    const result = await fetchJson(`${this.baseUrl}/${encodeURIComponent(sku)}`, isProduct, { method: 'GET' });
    return result.success ? { success: true, data: toProduct(result.data) } : result;
  }
}

export const productService = new ProductGateway();
