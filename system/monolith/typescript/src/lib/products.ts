import Decimal from 'decimal.js';
import { type ErpProductDetails, getProductDetails, getProducts } from './external';

/** One catalog product, as served by GET /api/products and GET /api/products/{sku}. */
export interface ViewProductDetailsResponse {
  sku: string;
  title: string | null;
  description: string | null;
  brand: string | null;
  category: string | null;
  /** Serialized as a 2-decimal string (e.g. "12.00") by jsonResponseWithDecimals. */
  price: Decimal;
}

/** Products are owned by the ERP; MyShop only proxies its catalog. ERP failures surface as ErpUnavailableError. */
export async function browseProducts(): Promise<ViewProductDetailsResponse[]> {
  const products = await getProducts();
  return products.map((product) => toResponse(product, product.id ?? ''));
}

/** The product, or null when the ERP does not know the SKU. */
export async function viewProduct(sku: string): Promise<ViewProductDetailsResponse | null> {
  const product = await getProductDetails(sku);
  return product ? toResponse(product, sku) : null;
}

export function productNotFoundDetail(sku: string): string {
  return `Product ${sku} does not exist.`;
}

function toResponse(product: ErpProductDetails, fallbackSku: string): ViewProductDetailsResponse {
  return {
    sku: product.id ?? fallbackSku,
    title: product.title ?? null,
    description: product.description ?? null,
    brand: product.brand ?? null,
    category: product.category ?? null,
    price: new Decimal(product.price),
  };
}
