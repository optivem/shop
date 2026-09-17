import type { NextRequest } from 'next/server';
import Decimal from 'decimal.js';
import { findCouponByCode } from '@/lib/db';
import { notFoundResponse, internalErrorResponse } from '@/lib/errors';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

function formatTimestamp(date: Date): string {
  return date.toISOString().replace('.000Z', 'Z');
}

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  try {
    const { code } = await params;
    const coupon = await findCouponByCode(code);

    if (!coupon) {
      return notFoundResponse(`Coupon ${code} does not exist.`);
    }

    return jsonResponseWithDecimals({
      code: coupon.code,
      discountRate: new Decimal(coupon.discount_rate),
      validFrom: coupon.valid_from ? formatTimestamp(coupon.valid_from) : null,
      validTo: coupon.valid_to ? formatTimestamp(coupon.valid_to) : null,
      usageLimit: coupon.usage_limit,
      usedCount: coupon.used_count,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
