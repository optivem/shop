import Decimal from 'decimal.js';
import { type ErpProductDetails, type ErpProductImage, getProductDetails, getProductImage, getProducts } from './external';

/** One catalog product, as served by GET /api/products and GET /api/products/{sku}. */
export interface ViewProductDetailsResponse {
  sku: string;
  title: string | null;
  description: string | null;
  brand: string | null;
  category: string | null;
  /** Serialized as a 2-decimal string (e.g. "12.00") by jsonResponseWithDecimals. */
  price: Decimal;
  /** The shop's own URL for the product photo (GET /api/products/{sku}/image); null when the ERP has none. */
  imageUrl: string | null;
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

export type ProductImageLoad = { kind: 'found'; image: ErpProductImage } | { kind: 'not-found'; detail: string };

/** The product photo, proxied from the ERP because the ERP is not reachable from the browser. */
export async function viewProductImage(sku: string): Promise<ProductImageLoad> {
  const product = await getProductDetails(sku);
  if (!product) {
    return { kind: 'not-found', detail: productNotFoundDetail(sku) };
  }
  if (!product.imageUrl?.trim()) {
    return { kind: 'not-found', detail: `Product ${sku} has no image.` };
  }
  const image = await getProductImage(product.imageUrl);
  return image ? { kind: 'found', image } : { kind: 'not-found', detail: `Image of product ${sku} does not exist.` };
}

export function productNotFoundDetail(sku: string): string {
  return `Product ${sku} does not exist.`;
}

function toResponse(product: ErpProductDetails, fallbackSku: string): ViewProductDetailsResponse {
  const sku = product.id ?? fallbackSku;
  return {
    sku,
    title: product.title ?? null,
    description: product.description ?? null,
    brand: product.brand ?? null,
    category: product.category ?? null,
    price: new Decimal(product.price),
    imageUrl: product.imageUrl?.trim() ? `/api/products/${encodeURIComponent(sku)}/image` : null,
  };
}
