import { NextRequest, NextResponse } from 'next/server';
import crypto from 'node:crypto';
import Decimal from 'decimal.js';
import { insertOrder, findAllOrders, findCouponByCode, incrementCouponUsage } from '@/lib/db';
import { getCurrentTime, getProductDetails, getPromotionDetails, getTaxDetails } from '@/lib/external';
import { validatePlaceOrderRequest } from '@/lib/validation';
import { badRequestResponse, validationErrorResponse, generalValidationErrorResponse, internalErrorResponse } from '@/lib/errors';
import { isRecord } from '@/lib/type-guards';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

type CouponResolution =
  | { ok: true; discountRate: number; appliedCouponCode: string | null }
  | { ok: false; response: NextResponse };

async function resolveCoupon(couponCode: string | null, now: Date): Promise<CouponResolution> {
  if (!couponCode) {
    return { ok: true, discountRate: 0, appliedCouponCode: null };
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
    return couponError(`Coupon code ${couponCode} has exceeded its usage limit`);
  }

  return { ok: true, discountRate: Number(coupon.discount_rate), appliedCouponCode: couponCode };
}

function couponError(message: string): CouponResolution {
  return { ok: false, response: validationErrorResponse([{ field: 'couponCode', message }]) };
}

export async function POST(request: NextRequest) {
  try {
    // Malformed JSON and non-object JSON (null, array, primitive) are both an invalid request format.
    const body: unknown = await request.json().catch(() => undefined);
    if (!isRecord(body)) {
      return badRequestResponse('Invalid request format');
    }

    const fieldErrors = validatePlaceOrderRequest(body);
    if (fieldErrors.length > 0) {
      return validationErrorResponse(fieldErrors);
    }

    const sku = body.sku as string;
    // The validator guarantees quantity is an integer or an integer-valued string.
    const quantity = Number(body.quantity);
    const country = (body.country as string).trim();
    const couponCode = typeof body.couponCode === 'string' && body.couponCode.trim() !== '' ? body.couponCode : null;

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

    const unitPrice = product.price;
    const promotion = await getPromotionDetails();
    const promotionFactor = promotion.promotionActive ? promotion.discount : 1;
    const basePrice = new Decimal(unitPrice).mul(quantity).toNumber();
    const promotedPrice = new Decimal(basePrice).mul(promotionFactor).toNumber();

    const couponResolution = await resolveCoupon(couponCode, now);
    if (!couponResolution.ok) {
      return couponResolution.response;
    }
    const { discountRate, appliedCouponCode } = couponResolution;
    const discountAmount = new Decimal(promotedPrice).mul(discountRate).toNumber();
    const subtotalPrice = new Decimal(promotedPrice).sub(discountAmount).toNumber();

    const taxDetails = await getTaxDetails(country);
    if (!taxDetails) {
      return validationErrorResponse([
        { field: 'country', message: `Country does not exist: ${country}` },
      ]);
    }
    const taxRate = taxDetails.taxRate;
    const taxAmount = new Decimal(subtotalPrice).mul(taxRate).toNumber();
    const totalPrice = new Decimal(subtotalPrice).add(taxAmount).toNumber();

    const orderNumber = `ORD-${crypto.randomUUID().toUpperCase()}`;
    const orderTimestamp = now;

    await insertOrder({
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
    });

    if (appliedCouponCode) {
      await incrementCouponUsage(appliedCouponCode);
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
        totalPrice: Number.parseFloat(o.total_price),
        appliedCouponCode: o.applied_coupon_code,
        status: o.status,
      })),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
