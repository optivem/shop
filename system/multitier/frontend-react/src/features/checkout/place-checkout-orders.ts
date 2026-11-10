// Checkout places one order per cart line, in cart order, with the same address and coupon (UC-009 BR-1).

import type { CartLine } from '../../contexts/cart-context';
import type { ShippingAddress } from '../../types/api.types';
import type { ApiError } from '../../types/error.types';
import type { Result } from '../../types/result.types';

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

export type PlaceOrderFn = (
  sku: string,
  quantity: number,
  address: ShippingAddress,
  couponCode?: string,
) => Promise<Result<{ orderNumber: string }>>;

/**
 * Why the backend rejected a line, in its own words: the field messages when it names fields
 * ("couponCode: Coupon code X does not exist" -> "Coupon code X does not exist"), else its message.
 */
export function reasonOf(error: ApiError): string {
  if (!error.fieldErrors?.length) return error.message;
  return error.fieldErrors.map((fieldError) => fieldError.replace(/^[\w.[\]]+: /, '')).join('; ');
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
      result.placed.push({ sku: line.sku, quantity: line.quantity, orderNumber: outcome.data.orderNumber });
    } else {
      result.rejected.push({ sku: line.sku, quantity: line.quantity, reason: reasonOf(outcome.error) });
    }
  }
  return result;
}
