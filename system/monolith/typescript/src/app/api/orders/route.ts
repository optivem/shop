import { type NextRequest, NextResponse } from 'next/server';
import crypto from 'node:crypto';
import Decimal from 'decimal.js';
import { insertOrder, findAllOrders, countOrders, findCouponByCode, tryIncrementCouponUsage } from '@/lib/db';
import { ErpUnavailableError, getCurrentTime, getProductDetails, getPromotionDetails, getTaxDetails } from '@/lib/external';
import { parsePlaceOrderRequest } from '@/lib/validation';
import {
  badRequestResponse,
  validationErrorResponse,
  generalValidationErrorResponse,
  erpUnavailableResponse,
  internalErrorResponse,
  unauthorizedResponse,
} from '@/lib/errors';
import { isRecord } from '@/lib/type-guards';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';
import { resolveCaller } from '@/lib/auth/caller';
import { isPastEnd, pageEnvelope, resolvePage } from '@/lib/paging';
import {
  IDEMPOTENCY_KEY_HEADER,
  IDEMPOTENCY_KEY_REUSED,
  requestFingerprint,
  resolveIdempotencyKey,
} from '@/lib/idempotency';
import {
  findStoredAnswer,
  placeIdempotently,
  type IdempotencyScope,
  type StoredAnswer,
} from '@/lib/idempotent-order';

// Thrown inside the order transaction to roll it back (key row included) and answer with a response.
class OrderRejection extends Error {
  constructor(readonly response: NextResponse) {
    super('order rejected');
  }
}

function orderCreatedResponse(status: number, body: string): NextResponse {
  const { orderNumber } = JSON.parse(body) as { orderNumber: string };
  return new NextResponse(body, {
    status,
    headers: { 'Content-Type': 'application/json', Location: `/api/orders/${orderNumber}` },
  });
}

function storedAnswerResponse(answer: StoredAnswer): NextResponse {
  return answer.kind === 'replay'
    ? orderCreatedResponse(answer.status, answer.body)
    : generalValidationErrorResponse(IDEMPOTENCY_KEY_REUSED);
}

type CouponResolution =
  | { ok: true; discountRate: Decimal; appliedCouponCode: string | null }
  | { ok: false; response: NextResponse };

async function resolveCoupon(couponCode: string | null, now: Date): Promise<CouponResolution> {
  if (!couponCode) {
    return { ok: true, discountRate: new Decimal(0), appliedCouponCode: null };
  }

  const coupon = await findCouponByCode(couponCode);
  if (!coupon) {
    return couponError(`Coupon code ${couponCode} does not exist`);
  }
  if (coupon.valid_from && now < new Date(coupon.valid_from)) {
    return couponError(`Coupon code ${couponCode} is not yet valid`);
  }
  if (coupon.valid_to && now > new Date(coupon.valid_to)) {
    return couponError(`Coupon code ${couponCode} has expired`);
  }
  if (coupon.usage_limit !== null && coupon.used_count >= coupon.usage_limit) {
    return couponError(usageLimitExceededMessage(couponCode));
  }

  return { ok: true, discountRate: new Decimal(coupon.discount_rate), appliedCouponCode: couponCode };
}

function couponError(message: string): CouponResolution {
  return { ok: false, response: couponErrorResponse(message) };
}

function couponErrorResponse(message: string): NextResponse {
  return validationErrorResponse([{ field: 'couponCode', message }]);
}

function usageLimitExceededMessage(couponCode: string): string {
  return `Coupon code ${couponCode} has exceeded its usage limit`;
}

export async function POST(request: NextRequest) {
  try {
    const caller = await resolveCaller(request);
    if (!caller) {
      return unauthorizedResponse('Authentication is required');
    }

    // Malformed JSON and non-object JSON (null, array, primitive) are both an invalid request format.
    const body: unknown = await request.json().catch(() => undefined);
    if (!isRecord(body)) {
      return badRequestResponse('Invalid request format');
    }

    const parsed = parsePlaceOrderRequest(body);
    if (!parsed.ok) {
      return validationErrorResponse(parsed.errors);
    }
    const { sku, quantity, country } = parsed.value;
    const couponCode = parsed.value.couponCode ?? null;

    // With an Idempotency-Key, a repeat of the same request inside 24 hours answers with the stored
    // response instead of placing a second order; the same key with a different request is rejected.
    const keyResolution = resolveIdempotencyKey(request.headers.get(IDEMPOTENCY_KEY_HEADER));
    if (!keyResolution.ok) {
      return validationErrorResponse(keyResolution.errors);
    }
    const scope: IdempotencyScope | null = keyResolution.key === null
      ? null
      : {
        owner: caller.subject ?? '',
        key: keyResolution.key,
        fingerprint: requestFingerprint({ sku, quantity, country, couponCode }),
      };
    if (scope) {
      const stored = await findStoredAnswer(scope);
      if (stored) {
        return storedAnswerResponse(stored);
      }
    }

    const now = await getCurrentTime();

    const month = now.getUTCMonth();
    const day = now.getUTCDate();
    const hour = now.getUTCHours();
    const minute = now.getUTCMinutes();
    if (month === 11 && day === 31 && (hour === 23 && minute >= 59)) {
      return generalValidationErrorResponse('Orders cannot be placed between 23:59 and 00:00 on December 31st');
    }

    const product = await getProductDetails(sku);
    if (!product) {
      return validationErrorResponse([
        { field: 'sku', message: `Product does not exist for SKU: ${sku}` },
      ]);
    }

    // Money stays exact (Decimal) through the whole computation; it is rounded once, on persist.
    const unitPrice = new Decimal(product.price);
    const promotion = await getPromotionDetails();
    const promotionFactor = new Decimal(promotion.promotionActive ? promotion.discount : 1);
    const basePrice = unitPrice.mul(quantity);
    const promotedPrice = basePrice.mul(promotionFactor);

    const couponResolution = await resolveCoupon(couponCode, now);
    if (!couponResolution.ok) {
      return couponResolution.response;
    }
    const { discountRate, appliedCouponCode } = couponResolution;
    const discountAmount = promotedPrice.mul(discountRate);
    const subtotalPrice = promotedPrice.sub(discountAmount);

    const taxDetails = await getTaxDetails(country);
    if (!taxDetails) {
      return validationErrorResponse([
        { field: 'country', message: `Country does not exist: ${country}` },
      ]);
    }
    const taxRate = new Decimal(taxDetails.taxRate);
    const taxAmount = subtotalPrice.mul(taxRate);
    const totalPrice = subtotalPrice.add(taxAmount);

    const orderNumber = `ORD-${crypto.randomUUID().toUpperCase()}`;
    const orderTimestamp = now;

    const responseBody = JSON.stringify({ orderNumber });

    // The key row, the coupon use (a conditional UPDATE, so concurrent orders cannot exceed the limit)
    // and the order commit in one transaction; a rejection rolls all of them back.
    let outcome;
    try {
      outcome = await placeIdempotently(scope, { status: 201, body: responseBody }, async (db) => {
        if (appliedCouponCode && !(await tryIncrementCouponUsage(appliedCouponCode, db))) {
          throw new OrderRejection(couponErrorResponse(usageLimitExceededMessage(appliedCouponCode)));
        }
        await insertOrder(
          {
            orderNumber,
            orderTimestamp,
            country,
            sku,
            quantity,
            unitPrice,
            basePrice,
            discountRate,
            discountAmount,
            subtotalPrice,
            taxRate,
            taxAmount,
            totalPrice,
            appliedCouponCode,
            status: 'PLACED',
            owner: caller.subject,
            ownerName: caller.username,
          },
          db
        );
      });
    } catch (error) {
      if (error instanceof OrderRejection) {
        return error.response;
      }
      throw error;
    }

    return outcome.kind === 'placed' ? orderCreatedResponse(201, responseBody) : storedAnswerResponse(outcome);
  } catch (error) {
    if (error instanceof ErpUnavailableError) {
      return erpUnavailableResponse(error);
    }
    return internalErrorResponse(error);
  }
}

export async function GET(request: NextRequest) {
  try {
    const caller = await resolveCaller(request);
    if (!caller) {
      return unauthorizedResponse('Authentication is required');
    }
    const params = request.nextUrl.searchParams;
    const pageResolution = resolvePage(params.get('page'), params.get('size'));
    if (!pageResolution.ok) {
      return validationErrorResponse(pageResolution.errors);
    }
    const pageSpec = pageResolution.value;
    const orderNumberFilter = params.get('orderNumber') ?? undefined;
    const owner = caller.admin ? null : caller.subject;
    // Paged in the database: a COUNT over the same predicate, then one page of rows.
    const totalElements = await countOrders(orderNumberFilter, owner);
    const orders = isPastEnd(pageSpec, totalElements)
      ? []
      : await findAllOrders(orderNumberFilter, owner, { limit: pageSpec.size, offset: pageSpec.offset });

    return jsonResponseWithDecimals({
      orders: orders.map((o) => ({
        orderNumber: o.order_number,
        orderTimestamp: o.order_timestamp.toISOString(),
        country: o.country,
        sku: o.sku,
        quantity: o.quantity,
        totalPrice: new Decimal(o.total_price),
        appliedCouponCode: o.applied_coupon_code,
        status: o.status,
        customer: caller.admin ? o.owner_name : undefined,
      })),
      ...pageEnvelope(pageSpec, totalElements),
    });
  } catch (error) {
    return internalErrorResponse(error);
  }
}
