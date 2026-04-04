import { NextRequest, NextResponse } from 'next/server';
import { insertCoupon, findAllCoupons, findCouponByCode } from '@/lib/db';
import { validationErrorResponse, internalErrorResponse } from '@/lib/errors';

export async function POST(request: NextRequest) {
  try {
    let body: Record<string, unknown>;
    try {
      body = await request.json();
    } catch {
      return NextResponse.json(
        {
          type: 'https://api.optivem.com/errors/bad-request',
          title: 'Bad Request',
          status: 400,
          detail: 'Invalid request format',
          timestamp: new Date().toISOString(),
        },
        { status: 400 }
      );
    }

    const errors: { field: string; message: string }[] = [];

    const code = body.code;
    if (code === undefined || code === null || (typeof code === 'string' && code.trim() === '')) {
      errors.push({ field: 'code', message: 'Code must not be empty' });
    }

    const rawDiscountRate = body.discountRate;
    if (rawDiscountRate === undefined || rawDiscountRate === null) {
      errors.push({ field: 'discountRate', message: 'Discount rate must not be empty' });
    } else {
      const dr = typeof rawDiscountRate === 'string' ? Number(rawDiscountRate) : rawDiscountRate as number;
      if (Number.isNaN(dr) || dr <= 0 || dr > 1) {
        errors.push({ field: 'discountRate', message: 'Discount rate must be between 0 and 1' });
      }
    }

    if (errors.length > 0) {
      return validationErrorResponse(errors);
    }

    const codeStr = (code as string).trim();
    const discountRate = typeof rawDiscountRate === 'string' ? Number(rawDiscountRate) : rawDiscountRate as number;

    const existing = await findCouponByCode(codeStr);
    if (existing) {
      return validationErrorResponse([{ field: 'code', message: `Coupon code already exists: ${codeStr}` }]);
    }

    const validFrom = body.validFrom ? new Date(body.validFrom as string) : null;
    const validTo = body.validTo ? new Date(body.validTo as string) : null;
    const usageLimit = body.usageLimit != null ? Number(body.usageLimit) : null;

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
    return NextResponse.json({
      coupons: coupons.map((c) => ({
        code: c.code,
        discountRate: Number.parseFloat(c.discount_rate),
        usageLimit: c.usage_limit,
        usedCount: c.used_count,
      })),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
