// Runtime guards for API responses: a response is checked against its declared type
// instead of being cast, so a malformed body fails as an error result, not a TypeError while rendering.

import { OrderStatus } from './api.types';
import type {
  BrowseCouponsItemResponse,
  BrowseCouponsResponse,
  BrowseOrderHistoryItemResponse,
  BrowseOrderHistoryResponse,
  PlaceOrderResponse,
  ViewOrderDetailsResponse,
} from './api.types';
import type { ProblemDetail, ValidationError } from './error.types';

export type Guard<T> = (value: unknown) => value is T;

type Shape<T> = { [K in keyof T]-?: (value: unknown) => boolean };

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function hasShape<T>(shape: Shape<T>): Guard<T> {
  const checks = Object.entries<(value: unknown) => boolean>(shape);
  return (value: unknown): value is T => isObject(value) && checks.every(([key, check]) => check(value[key]));
}

function isArrayOf<T>(guard: Guard<T>): Guard<T[]> {
  return (value: unknown): value is T[] => Array.isArray(value) && value.every(guard);
}

const isString = (value: unknown): value is string => typeof value === 'string';
const isNumber = (value: unknown): value is number => typeof value === 'number' && Number.isFinite(value);
const isStringOrNull = (value: unknown) => value === null || isString(value);
const isOptionalString = (value: unknown) => value == null || isString(value);
const isNumberOrNull = (value: unknown) => value === null || isNumber(value);
const isOrderStatus = (value: unknown): value is OrderStatus => Object.values<unknown>(OrderStatus).includes(value);

export const isPlaceOrderResponse = hasShape<PlaceOrderResponse>({ orderNumber: isString });

export const isViewOrderDetailsResponse = hasShape<ViewOrderDetailsResponse>({
  orderNumber: isString,
  orderTimestamp: isString,
  country: isString,
  sku: isString,
  quantity: isNumber,
  unitPrice: isNumber,
  basePrice: isNumber,
  discountRate: isNumber,
  discountAmount: isNumber,
  subtotalPrice: isNumber,
  taxRate: isNumber,
  taxAmount: isNumber,
  totalPrice: isNumber,
  appliedCouponCode: isStringOrNull,
  status: isOrderStatus,
});

export const isBrowseOrderHistoryResponse = hasShape<BrowseOrderHistoryResponse>({
  orders: isArrayOf(
    hasShape<BrowseOrderHistoryItemResponse>({
      orderNumber: isString,
      orderTimestamp: isString,
      country: isString,
      sku: isString,
      quantity: isNumber,
      totalPrice: isNumber,
      appliedCouponCode: isStringOrNull,
      status: isOrderStatus,
    })
  ),
});

export const isBrowseCouponsResponse = hasShape<BrowseCouponsResponse>({
  coupons: isArrayOf(
    hasShape<BrowseCouponsItemResponse>({
      code: isString,
      discountRate: isNumber,
      validFrom: isOptionalString,
      validTo: isOptionalString,
      usageLimit: isNumberOrNull,
      usedCount: isNumber,
    })
  ),
});

const isValidationErrors = isArrayOf(hasShape<ValidationError>({ field: isString, message: isString }));

export const isProblemDetail = hasShape<ProblemDetail>({
  type: isOptionalString,
  title: isOptionalString,
  status: isNumber,
  detail: isOptionalString,
  errors: (value) => value == null || isValidationErrors(value),
  timestamp: isOptionalString,
});
