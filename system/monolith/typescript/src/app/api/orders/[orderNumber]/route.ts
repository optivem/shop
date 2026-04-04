import { NextRequest, NextResponse } from 'next/server';
import { findByOrderNumber } from '@/lib/db';
import { notFoundResponse, internalErrorResponse } from '@/lib/errors';

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

    return NextResponse.json({
      orderNumber: order.order_number,
      orderTimestamp: order.order_timestamp.toISOString(),
      country: order.country,
      sku: order.sku,
      quantity: order.quantity,
      unitPrice: Number.parseFloat(order.unit_price),
      basePrice: Number.parseFloat(order.base_price),
      discountRate: Number.parseFloat(order.discount_rate),
      discountAmount: Number.parseFloat(order.discount_amount),
      subtotalPrice: Number.parseFloat(order.subtotal_price),
      taxRate: Number.parseFloat(order.tax_rate),
      taxAmount: Number.parseFloat(order.tax_amount),
      totalPrice: Number.parseFloat(order.total_price),
      appliedCouponCode: order.applied_coupon_code,
      status: order.status,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
