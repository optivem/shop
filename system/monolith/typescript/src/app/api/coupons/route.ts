import { type NextRequest, NextResponse } from 'next/server';
import Decimal from 'decimal.js';
import { insertCoupon, findAllCoupons, countCoupons, findCouponByCode } from '@/lib/db';
import { isPastEnd, pageEnvelope, resolvePage } from '@/lib/paging';
import { badRequestResponse, validationErrorResponse, internalErrorResponse } from '@/lib/errors';
import { isRecord } from '@/lib/type-guards';
import { parsePublishCouponRequest } from '@/lib/validation';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

export async function POST(request: NextRequest) {
  try {
    // Malformed JSON and non-object JSON (null, array, primitive) are both an invalid request format.
    const body: unknown = await request.json().catch(() => undefined);
    if (!isRecord(body)) {
      return badRequestResponse('Invalid request format');
    }

    const parsed = parsePublishCouponRequest(body);
    if (!parsed.ok) {
      return validationErrorResponse(parsed.errors);
    }
    const { code, discountRate, validFrom, validTo, usageLimit } = parsed.value;

    const existing = await findCouponByCode(code);
    if (existing) {
      return validationErrorResponse([{ field: 'couponCode', message: `Coupon code ${code} already exists` }]);
    }

    await insertCoupon({ code, discountRate, validFrom, validTo, usageLimit });

    return NextResponse.json({ code }, { status: 201 });
  } catch (error) {
    return internalErrorResponse(error);
  }
}

export async function GET(request: NextRequest) {
  try {
    const params = request.nextUrl.searchParams;
    const pageResolution = resolvePage(params.get('page'), params.get('size'));
    if (!pageResolution.ok) {
      return validationErrorResponse(pageResolution.errors);
    }
    const pageSpec = pageResolution.value;
    const totalElements = await countCoupons();
    const coupons = isPastEnd(pageSpec, totalElements)
      ? []
      : await findAllCoupons({ limit: pageSpec.size, offset: pageSpec.offset });
    return jsonResponseWithDecimals({
      coupons: coupons.map((c) => ({
        code: c.code,
        discountRate: new Decimal(c.discount_rate),
        validFrom: c.valid_from ? c.valid_from.toISOString() : null,
        validTo: c.valid_to ? c.valid_to.toISOString() : null,
        usageLimit: c.usage_limit,
        usedCount: c.used_count,
      })),
      ...pageEnvelope(pageSpec, totalElements),
    });
  } catch (error) {
    return internalErrorResponse(error);
  }
}
