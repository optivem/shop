import { type NextRequest, NextResponse } from 'next/server';
import crypto from 'node:crypto';
import Decimal from 'decimal.js';
import { insertOrder, findAllOrders, findCouponByCode, tryIncrementCouponUsage, inTransaction } from '@/lib/db';
import { getCurrentTime, getProductDetails, getPromotionDetails, getTaxDetails } from '@/lib/external';
import { parsePlaceOrderRequest } from '@/lib/validation';
import { badRequestResponse, validationErrorResponse, generalValidationErrorResponse, internalErrorResponse } from '@/lib/errors';
import { isRecord } from '@/lib/type-guards';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

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

    // The usage count is claimed with a conditional UPDATE in the same transaction as the insert, so
    // concurrent orders cannot exceed the limit and a failed insert does not use up the coupon.
    const rejection = await inTransaction(async (db) => {
      if (appliedCouponCode && !(await tryIncrementCouponUsage(appliedCouponCode, db))) {
        return couponErrorResponse(usageLimitExceededMessage(appliedCouponCode));
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
        },
        db
      );
      return null;
    });
    if (rejection) {
      return rejection;
    }

    return NextResponse.json(
      { orderNumber },
      {
        status: 201,
        headers: { Location: `/api/orders/${orderNumber}` },
      }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}

export async function GET(request: NextRequest) {
  try {
    const orderNumberFilter = request.nextUrl.searchParams.get('orderNumber') ?? undefined;
    const orders = await findAllOrders(orderNumberFilter);

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
      })),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
