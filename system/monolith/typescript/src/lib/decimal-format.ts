import { NextResponse } from 'next/server';
import Decimal from 'decimal.js';

const DECIMAL_FIELDS: Record<string, number> = {
  unitPrice: 2,
  basePrice: 2,
  discountRate: 4,
  discountAmount: 2,
  subtotalPrice: 2,
  taxRate: 4,
  taxAmount: 2,
  totalPrice: 2,
};

function markDecimals(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(markDecimals);
  }

  if (typeof obj === 'object') {
    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      const scale = DECIMAL_FIELDS[key];
      if (scale !== undefined && Decimal.isDecimal(value)) {
        // Format straight from the exact Decimal (never via a float) into a fixed-scale JSON string.
        result[key] = value.toFixed(scale, Decimal.ROUND_HALF_UP);
      } else {
        result[key] = markDecimals(value);
      }
    }
    return result;
  }

  return obj;
}

export function jsonResponseWithDecimals(
  data: unknown,
  init?: { status?: number; headers?: Record<string, string> },
): NextResponse {
  const json = JSON.stringify(markDecimals(data));

  return new NextResponse(json, {
    status: init?.status ?? 200,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...init?.headers,
    },
  });
}
