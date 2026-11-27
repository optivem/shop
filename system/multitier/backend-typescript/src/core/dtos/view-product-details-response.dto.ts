import type Decimal from 'decimal.js';

/** One catalog product, as served by GET /api/products and GET /api/products/{sku}. */
export interface ViewProductDetailsResponse {
  sku: string;
  title: string | null;
  description: string | null;
  brand: string | null;
  category: string | null;
  /** Serialized as a 2-decimal string (e.g. "12.00") by the DecimalFormatInterceptor. */
  price: Decimal;
  /** The shop's own URL for the product photo (GET /api/products/{sku}/image); null when the ERP has none. */
  imageUrl: string | null;
}
