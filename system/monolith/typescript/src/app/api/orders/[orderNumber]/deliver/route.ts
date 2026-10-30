import { type NextRequest, NextResponse } from 'next/server';
import { findByOrderNumber, updateOrderStatus } from '@/lib/db';
import { conflictResponse, notFoundResponse, generalValidationErrorResponse, internalErrorResponse } from '@/lib/errors';

export async function POST(
  _request: NextRequest,
  { params }: { params: Promise<{ orderNumber: string }> }
) {
  try {
    const { orderNumber } = await params;

    const order = await findByOrderNumber(orderNumber);

    if (!order) {
      return notFoundResponse(`Order ${orderNumber} does not exist.`);
    }

    if (order.status !== 'PLACED') {
      return generalValidationErrorResponse('Order cannot be delivered in its current status');
    }

    if (!(await updateOrderStatus(order, 'DELIVERED'))) {
      return conflictResponse();
    }

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
