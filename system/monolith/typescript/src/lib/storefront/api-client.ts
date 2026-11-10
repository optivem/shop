// Browser only: the storefront pages' calls to the monolith's own API (profile, place order).

import {
  formatFieldErrors,
  parseJson,
  placeOrderResponseSchema,
  profileResponseSchema,
  readErrorData,
  type ProfileResponseData,
} from '@/lib/api-types';
import type { PlaceOutcome } from './place-checkout-orders';
import type { ShippingAddress } from './types';

export type ApiResult<T> = { success: true; data: T } | { success: false; message: string; fieldErrors: string[] };

async function failureOf(response: Response, fallback: string): Promise<{ success: false; message: string; fieldErrors: string[] }> {
  const error = await readErrorData(response);
  return { success: false, message: error.detail ?? fallback, fieldErrors: formatFieldErrors(error) };
}

function networkFailure(err: unknown): { success: false; message: string; fieldErrors: string[] } {
  return { success: false, message: `Network error: ${err instanceof Error ? err.message : String(err)}`, fieldErrors: [] };
}

export async function viewProfile(): Promise<ApiResult<ProfileResponseData>> {
  try {
    const response = await fetch('/api/account/profile', { cache: 'no-store' });
    if (!response.ok) return await failureOf(response, 'Failed to load the profile');
    return { success: true, data: await parseJson(response, profileResponseSchema) };
  } catch (err) {
    return networkFailure(err);
  }
}

export async function updateProfile(address: ShippingAddress): Promise<ApiResult<ProfileResponseData>> {
  try {
    const response = await fetch('/api/account/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(address),
    });
    if (!response.ok) return await failureOf(response, 'Failed to save the profile');
    return { success: true, data: await parseJson(response, profileResponseSchema) };
  } catch (err) {
    return networkFailure(err);
  }
}

/** Places one checkout line: the SKU and quantity, shipped (and taxed) to `address`. */
export async function placeOrderTo(sku: string, quantity: number, address: ShippingAddress, couponCode?: string): Promise<PlaceOutcome> {
  try {
    const response = await fetch('/api/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sku, quantity, shippingAddress: address, ...(couponCode ? { couponCode } : {}) }),
    });
    if (!response.ok) return await failureOf(response, 'An error occurred');
    const data = await parseJson(response, placeOrderResponseSchema);
    return { success: true, orderNumber: data.orderNumber };
  } catch (err) {
    return networkFailure(err);
  }
}
