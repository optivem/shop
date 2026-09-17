import { NextRequest, NextResponse } from 'next/server';
import { insertCoupon, findAllCoupons, findCouponByCode } from '@/lib/db';
import { badRequestResponse, validationErrorResponse, internalErrorResponse } from '@/lib/errors';
import { isRecord } from '@/lib/type-guards';
import { validatePublishCouponRequest } from '@/lib/validation';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

export async function POST(request: NextRequest) {
  try {
    // Malformed JSON and non-object JSON (null, array, primitive) are both an invalid request format.
    const body: unknown = await request.json().catch(() => undefined);
    if (!isRecord(body)) {
      return badRequestResponse('Invalid request format');
    }

    const fieldErrors = validatePublishCouponRequest(body);
    if (fieldErrors.length > 0) {
      return validationErrorResponse(fieldErrors);
    }

    const codeStr = (body.code as string).trim();
    const discountRate = typeof body.discountRate === 'string' ? Number(body.discountRate) : body.discountRate as number;

    const existing = await findCouponByCode(codeStr);
    if (existing) {
      return validationErrorResponse([{ field: 'couponCode', message: `Coupon code ${codeStr} already exists` }]);
    }

    const validFrom = body.validFrom ? new Date(body.validFrom as string) : null;
    const validTo = body.validTo ? new Date(body.validTo as string) : null;
    const usageLimit = body.usageLimit == null ? null : Number(body.usageLimit);

    await insertCoupon({ code: codeStr, discountRate, validFrom, validTo, usageLimit });

    return NextResponse.json({ code: codeStr }, { status: 201 });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}

export async function GET() {
  try {
    const coupons = await findAllCoupons();
    return jsonResponseWithDecimals({
      coupons: coupons.map((c) => ({
        code: c.code,
        discountRate: Number.parseFloat(c.discount_rate),
        validFrom: c.valid_from ? c.valid_from.toISOString() : null,
        validTo: c.valid_to ? c.valid_to.toISOString() : null,
        usageLimit: c.usage_limit,
        usedCount: c.used_count,
      })),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
