import { type NextRequest, NextResponse } from 'next/server';
import { findByOrderNumber, updateOrderStatus } from '@/lib/db';
import { getCurrentTime } from '@/lib/external';
import { notFoundResponse, generalValidationErrorResponse, internalErrorResponse } from '@/lib/errors';

export async function POST(
  _request: NextRequest,
  { params }: { params: Promise<{ orderNumber: string }> }
) {
  try {
    const { orderNumber } = await params;

    const now = await getCurrentTime();
    const utcMonth = now.getUTCMonth();
    const utcDay = now.getUTCDate();

    if (utcMonth === 11 && utcDay === 31) {
      const blackoutStart = new Date(Date.UTC(now.getUTCFullYear(), 11, 31, 22, 0, 0));
      const blackoutEnd = new Date(Date.UTC(now.getUTCFullYear(), 11, 31, 22, 30, 0));

      if (now >= blackoutStart && now <= blackoutEnd) {
        return generalValidationErrorResponse(
          'Order cancellation is not allowed on December 31st between 22:00 and 23:00'
        );
      }
    }

    const order = await findByOrderNumber(orderNumber);

    if (!order) {
      return notFoundResponse(`Order ${orderNumber} does not exist.`);
    }

    if (order.status === 'CANCELLED') {
      return generalValidationErrorResponse('Order has already been cancelled');
    }

    await updateOrderStatus(orderNumber, 'CANCELLED');

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return internalErrorResponse(message);
  }
}
