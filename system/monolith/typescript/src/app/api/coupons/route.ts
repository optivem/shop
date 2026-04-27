import { NextRequest, NextResponse } from 'next/server';
import { insertCoupon, findAllCoupons, findCouponByCode } from '@/lib/db';
import { validationErrorResponse, internalErrorResponse } from '@/lib/errors';
import { validatePublishCouponRequest } from '@/lib/validation';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

export async function POST(request: NextRequest) {
  try {
    let body: Record<string, unknown>;
    try {
      body = await request.json();
    } catch {
      return NextResponse.json(
        {
          type: 'https://api.my-company.example/errors/bad-request',
          title: 'Bad Request',
          status: 400,
          detail: 'Invalid request format',
          timestamp: new Date().toISOString(),
        },
        { status: 400 }
      );
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
