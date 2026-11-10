// Prices the cart for display: unit prices come from the current catalog every time (UC-008 BR-5).
// The cart itself keeps only SKUs and quantities; the backend prices each order when it is placed.

import type { CartLine } from '../../contexts/cart-context';
import type { Product } from '../../types/api.types';
import { lineTotalCents } from '../../utils/money';

export interface PricedCartLine {
  sku: string;
  quantity: number;
  /** Undefined when the catalog no longer offers the SKU. */
  product?: Product;
  lineTotalCents: number;
}

export function priceCart(lines: CartLine[], products: Product[]): { lines: PricedCartLine[]; subtotalCents: number } {
  const priced = lines.map((line) => {
    const product = products.find((p) => p.sku === line.sku);
    return {
      sku: line.sku,
      quantity: line.quantity,
      product,
      lineTotalCents: product ? lineTotalCents(product.price, line.quantity) : 0,
    };
  });
  return { lines: priced, subtotalCents: priced.reduce((sum, line) => sum + line.lineTotalCents, 0) };
}
