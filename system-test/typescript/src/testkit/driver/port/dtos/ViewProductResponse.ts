/**
 * The product page as shown. The API channel fills only the product fields; everything else is UI-only and stays
 * unset there.
 */
export interface ViewProductResponse {
  sku?: string;
  title?: string;
  brand?: string;
  category?: string;
  description?: string;
  /** The price as the channel shows it: "$1,299.99" on the UI, the plain amount on the API. */
  price?: string;
  breadcrumbs?: string[];
  /** The quantity field's value. */
  quantity?: string;
  /** The notification, the "Product not found" heading or the ERP outage message, whichever shows. */
  message?: string | null;
  backToCatalogOffered?: boolean;
  addToCartOffered?: boolean;
  /** The header's cart badge; 0 when it is not shown. */
  cartItemCount?: number;
  /** The SKU of the product page the visitor is on, from the page address; null on any other page. */
  pageSku?: string | null;
  /** The cart's lines, read only after adding to the cart. */
  cartLines?: CartLineDto[];
  /** The catalog's products, read only after following the link back to the catalog. */
  catalogSkus?: string[];
}

export interface CartLineDto {
  sku: string;
  quantity: number;
}
