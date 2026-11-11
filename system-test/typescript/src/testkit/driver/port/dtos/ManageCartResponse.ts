/**
 * The site after the visitor's action: the notification and cart badge where the action ended, then the cart as the
 * cart page shows it.
 */
export interface ManageCartResponse {
  /** The action's notification; else the cart page's empty-cart or ERP-outage message; else null. */
  message?: string | null;
  /** The header's cart badge where the action ended; 0 when it is not shown. */
  cartItemCount?: number;
  /** The cart's lines, in the order shown. */
  lines?: ManageCartLineDto[];
  /** The "(n items)" count next to the heading; null when it is not shown. */
  itemCount?: number | null;
  subtotal?: string | null;
  /** The note that tax and discounts are calculated at checkout; null when it is not shown. */
  checkoutNote?: string | null;
  startShoppingOffered?: boolean;
  /** Whether the visitor ended up on the login form. */
  askedToSignIn?: boolean;
}

export interface ManageCartLineDto {
  sku: string;
  title: string;
  unitPrice: string;
  quantity: number;
  lineTotal: string;
}
