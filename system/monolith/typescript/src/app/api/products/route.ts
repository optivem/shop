import { ErpUnavailableError } from '@/lib/external';
import { jsonResponseWithDecimals } from '@/lib/decimal-format';
import { erpUnavailableResponse, internalErrorResponse } from '@/lib/errors';
import { browseProducts } from '@/lib/products';

// Every call proxies the ERP's current catalog, so the response must never be prerendered or cached.
export const dynamic = 'force-dynamic';

/** Public catalog: anonymous callers may browse (see PUBLIC_RULES in lib/auth/policy). */
export async function GET() {
  try {
    return jsonResponseWithDecimals(await browseProducts());
  } catch (error) {
    if (error instanceof ErpUnavailableError) {
      return erpUnavailableResponse(error);
    }
    return internalErrorResponse(error);
  }
}
