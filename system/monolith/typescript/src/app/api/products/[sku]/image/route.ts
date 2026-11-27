import type { NextRequest } from 'next/server';
import { ErpUnavailableError } from '@/lib/external';
import { erpUnavailableResponse, internalErrorResponse, notFoundResponse } from '@/lib/errors';
import { viewProductImage } from '@/lib/products';

/** Public catalog: anonymous callers may view a product photo (see PUBLIC_RULES in lib/auth/policy). */
export async function GET(_request: NextRequest, { params }: { params: Promise<{ sku: string }> }) {
  try {
    const { sku } = await params;
    const result = await viewProductImage(sku);
    if (result.kind === 'not-found') {
      return notFoundResponse(result.detail);
    }
    return new Response(new Uint8Array(result.image.content), {
      status: 200,
      headers: { 'content-type': result.image.contentType },
    });
  } catch (error) {
    if (error instanceof ErpUnavailableError) {
      return erpUnavailableResponse(error);
    }
    return internalErrorResponse(error);
  }
}
