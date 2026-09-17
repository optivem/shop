import { NextRequest } from 'next/server';
import Decimal from 'decimal.js';
import { findByOrderNumber } from '@/lib/db';
import { notFoundResponse, internalErrorResponse } from '@/lib/errors';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ orderNumber: string }> }
) {
  try {
    const { orderNumber } = await params;
    const order = await findByOrderNumber(orderNumber);

    if (!order) {
      return notFoundResponse(`Order ${orderNumber} does not exist.`);
    }

    return jsonResponseWithDecimals({
      orderNumber: order.order_number,
      orderTimestamp: order.order_timestamp.toISOString(),
      country: order.country,
      sku: order.sku,
      quantity: order.quantity,
      unitPrice: new Decimal(order.unit_price),
      basePrice: new Decimal(order.base_price),
      discountRate: new Decimal(order.discount_rate),
      discountAmount: new Decimal(order.discount_amount),
      subtotalPrice: new Decimal(order.subtotal_price),
      taxRate: new Decimal(order.tax_rate),
      taxAmount: new Decimal(order.tax_amount),
      totalPrice: new Decimal(order.total_price),
      appliedCouponCode: order.applied_coupon_code,
      status: order.status,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
