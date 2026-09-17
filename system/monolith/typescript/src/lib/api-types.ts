import { z } from 'zod';
import { ORDER_STATUSES } from './order-status';

// Response shapes of the monolith's own API, as read by the pages. Responses are parsed, not cast,
// so a malformed body fails with a clear message instead of a TypeError while rendering.

const fieldErrorSchema = z.object({
  field: z.string().nullish(),
  message: z.string(),
});

const errorDataSchema = z.object({
  detail: z.string().nullish(),
  errors: z.array(fieldErrorSchema).nullish(),
});

export const orderDetailSchema = z.object({
  orderNumber: z.string(),
  orderTimestamp: z.string(),
  country: z.string(),
  sku: z.string(),
  quantity: z.number(),
  unitPrice: z.number(),
  basePrice: z.number(),
  discountRate: z.number(),
  discountAmount: z.number(),
  subtotalPrice: z.number(),
  taxRate: z.number(),
  taxAmount: z.number(),
  totalPrice: z.number(),
  appliedCouponCode: z.string().nullable(),
  status: z.enum(ORDER_STATUSES),
});

export const ordersResponseSchema = z.object({
  orders: z.array(
    z.object({
      orderNumber: z.string(),
      orderTimestamp: z.string(),
      sku: z.string(),
      quantity: z.number(),
      totalPrice: z.number(),
      status: z.enum(ORDER_STATUSES),
    })
  ),
});

export const couponsResponseSchema = z.object({
  coupons: z.array(
    z.object({
      code: z.string(),
      discountRate: z.number(),
      validFrom: z.string().nullish(),
      validTo: z.string().nullish(),
      usageLimit: z.number().nullish(),
      usedCount: z.number(),
    })
  ),
});

export const placeOrderResponseSchema = z.object({ orderNumber: z.string() });

export type ErrorData = z.infer<typeof errorDataSchema>;
export type OrderDetail = z.infer<typeof orderDetailSchema>;
export type Order = z.infer<typeof ordersResponseSchema>['orders'][number];
export type Coupon = z.infer<typeof couponsResponseSchema>['coupons'][number];

export async function parseJson<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  const body: unknown = await response.json();
  const result = schema.safeParse(body);
  if (!result.success) {
    throw new Error(`Unexpected response from server: ${z.prettifyError(result.error)}`, { cause: result.error });
  }
  return result.data;
}

// An error body that is not JSON or not the expected shape yields no detail, so callers fall back to their default message.
export async function readErrorData(response: Response): Promise<ErrorData> {
  const body: unknown = await response.json().catch(() => undefined);
  const result = errorDataSchema.safeParse(body);
  return result.success ? result.data : {};
}

export function formatFieldErrors(data: ErrorData): string[] {
  return (data.errors ?? []).map((err) => `${err.field ? `${err.field}: ` : ''}${err.message}`);
}
