import { NextRequest, NextResponse } from 'next/server';
import crypto from 'crypto';
import { insertOrder, findAllOrders } from '@/lib/db';
import { getCurrentTime, getProductDetails } from '@/lib/external';
import { validatePlaceOrderRequest } from '@/lib/validation';
import { validationErrorResponse, generalValidationErrorResponse, internalErrorResponse, FieldError } from '@/lib/errors';

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

    const fieldErrors = validatePlaceOrderRequest(body);
    if (fieldErrors.length > 0) {
      return validationErrorResponse(fieldErrors);
    }

    const sku = body.sku as string;
    const quantity = typeof body.quantity === 'string' ? Number(body.quantity) : body.quantity as number;

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
    const dayOfWeek = now.getUTCDay();
    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
    const discountFactor = isWeekend ? 0.5 : 1.0;
    const totalPrice = unitPrice * quantity * discountFactor;

    const orderNumber = `ORD-${crypto.randomUUID().toUpperCase()}`;
    const orderTimestamp = now;

    await insertOrder({
      orderNumber,
      orderTimestamp,
      sku,
      quantity,
      unitPrice,
      totalPrice,
      status: 'PLACED',
    });

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
    const orderNumberFilter = request.nextUrl.searchParams.get('orderNumber') || undefined;
    const orders = await findAllOrders(orderNumberFilter);

    return NextResponse.json({
      orders: orders.map((o) => ({
        orderNumber: o.order_number,
        orderTimestamp: o.order_timestamp.toISOString(),
        sku: o.sku,
        quantity: o.quantity,
        totalPrice: parseFloat(o.total_price),
        status: o.status,
      })),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
