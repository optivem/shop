// Server only: the storefront pages read the catalog through the service layer directly (storefront-ux §6.2),
// not through their own HTTP API.

import { ErpUnavailableError } from '@/lib/external';
import { logger } from '@/lib/logger';
import { browseProducts, viewProduct, type ViewProductDetailsResponse } from '@/lib/products';
import type { Product } from './types';

export type CatalogLoad = { products: Product[]; unavailable: false } | { products: []; unavailable: true };

export type ProductLoad = { kind: 'found'; product: Product } | { kind: 'not-found' } | { kind: 'unavailable' };

function toProduct(product: ViewProductDetailsResponse): Product {
  return {
    sku: product.sku,
    title: product.title ?? product.sku,
    description: product.description ?? '',
    brand: product.brand ?? '',
    category: product.category ?? '',
    price: product.price.toFixed(2),
  };
}

/** The whole catalog; an ERP failure is reported as unavailable, so the page around it still renders. */
export async function loadCatalog(): Promise<CatalogLoad> {
  try {
    return { products: (await browseProducts()).map(toProduct), unavailable: false };
  } catch (error) {
    if (!(error instanceof ErpUnavailableError)) {
      logger.error('Loading the catalog failed', error);
    }
    return { products: [], unavailable: true };
  }
}

export async function loadProduct(sku: string): Promise<ProductLoad> {
  try {
    const product = await viewProduct(sku);
    return product ? { kind: 'found', product: toProduct(product) } : { kind: 'not-found' };
  } catch (error) {
    if (!(error instanceof ErpUnavailableError)) {
      logger.error(`Loading product ${sku} failed`, error);
    }
    return { kind: 'unavailable' };
  }
}
