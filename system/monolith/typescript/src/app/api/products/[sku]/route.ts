import type { NextRequest } from 'next/server';
import { ErpUnavailableError } from '@/lib/external';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';
import { erpUnavailableResponse, internalErrorResponse, notFoundResponse } from '@/lib/errors';
import { productNotFoundDetail, viewProduct } from '@/lib/products';

/** Public catalog: anonymous callers may view a product (see PUBLIC_RULES in lib/auth/policy). */
export async function GET(_request: NextRequest, { params }: { params: Promise<{ sku: string }> }) {
  try {
    const { sku } = await params;
    const product = await viewProduct(sku);
    if (!product) {
      return notFoundResponse(productNotFoundDetail(sku));
    }
    return jsonResponseWithDecimals(product);
  } catch (error) {
    if (error instanceof ErpUnavailableError) {
      return erpUnavailableResponse(error);
    }
    return internalErrorResponse(error);
  }
}
