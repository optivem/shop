import type { CartLineDto } from './ViewProductResponse.js';

export const CHECKOUT = 'checkout';
export const CONFIRMATION = 'confirmation';
export const CART = 'cart';

/**
 * Where the checkout left the customer: the checkout as it was opened and as it ended, the order confirmation, or the
 * cart; and, once the order was placed, the cart.
 */
export interface CheckOutResponse {
  /** CHECKOUT, CONFIRMATION or CART (or the path of any other page). */
  shownPage?: string;
  /** The error notification's message on the checkout, or the cart's message; else null. */
  message?: string | null;
  /** The error notification's lines under its message, as shown. */
  messageDetails?: string[];

  // The checkout
  reviewLines?: CheckOutReviewLineDto[];
  reviewSubtotal?: string;
  /** The tax country and estimated tax rate when the checkout was opened, before any change. */
  openedTaxCountry?: string;
  openedEstimatedTaxRate?: string;
  taxCountry?: string;
  estimatedTaxRate?: string;
  reviewNote?: string | null;
  savedAddressSelected?: boolean;
  /** The saved address as the checkout shows it; null when there is none. */
  savedAddress?: string | null;

  // The confirmation
  /** The confirmation's heading; null when it thanks for no order. */
  thanks?: string | null;
  /** The confirmed order numbers, in the order shown. */
  confirmedOrders?: CheckOutConfirmedOrderDto[];
  rejectedLines?: CheckOutRejectedLineDto[];
  viewMyOrdersOffered?: boolean;
  continueShoppingOffered?: boolean;

  /** The cart after the order was placed (or rejected); unset when no order was placed. */
  cartLines?: CartLineDto[];
}

export interface CheckOutReviewLineDto {
  sku: string;
  quantity: number;
  lineTotal: string;
}

export interface CheckOutConfirmedOrderDto {
  orderNumber: string;
  /** Where the order number links to. */
  link: string | null;
}

export interface CheckOutRejectedLineDto {
  sku: string;
  reason: string;
}
