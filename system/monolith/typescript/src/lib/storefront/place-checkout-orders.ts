// Checkout places one order per cart line, in cart order, with the same address and coupon (UC-009 BR-1).

import type { CartLine, ShippingAddress } from './types';

export interface PlacedLine {
  sku: string;
  quantity: number;
  orderNumber: string;
}

export interface RejectedLine {
  sku: string;
  quantity: number;
  reason: string;
}

export interface CheckoutResult {
  placed: PlacedLine[];
  rejected: RejectedLine[];
}

/** The outcome of placing one line: its order number, or the backend's message and field errors. */
export type PlaceOutcome =
  | { success: true; orderNumber: string }
  | { success: false; message: string; fieldErrors: string[] };

export type PlaceOrderFn = (
  sku: string,
  quantity: number,
  address: ShippingAddress,
  couponCode?: string,
) => Promise<PlaceOutcome>;

/**
 * Why the backend rejected a line, in its own words: the field messages when it names fields
 * ("couponCode: Coupon code X does not exist" -> "Coupon code X does not exist"), else its message.
 */
export function reasonOf(message: string, fieldErrors: string[]): string {
  if (fieldErrors.length === 0) return message;
  return fieldErrors.map((fieldError) => fieldError.replace(/^[\w.[\]]+: /, '')).join('; ');
}

/** Places the lines one after another, so the orders are created in cart order; a rejected line doesn't stop the rest. */
export async function placeCheckoutOrders(
  lines: CartLine[],
  address: ShippingAddress,
  couponCode: string | undefined,
  placeOrder: PlaceOrderFn,
): Promise<CheckoutResult> {
  const result: CheckoutResult = { placed: [], rejected: [] };
  for (const line of lines) {
    const outcome = await placeOrder(line.sku, line.quantity, address, couponCode);
    if (outcome.success) {
      result.placed.push({ sku: line.sku, quantity: line.quantity, orderNumber: outcome.orderNumber });
    } else {
      result.rejected.push({ sku: line.sku, quantity: line.quantity, reason: reasonOf(outcome.message, outcome.fieldErrors) });
    }
  }
  return result;
}
