import { z } from 'zod';
import { envOrDefault } from './env';

const ERP_API_URL = () => envOrDefault('ERP_API_URL', 'http://localhost:9001/erp');
const CLOCK_API_URL = () => envOrDefault('CLOCK_API_URL', 'http://localhost:9001/clock');
const TAX_API_URL = () => envOrDefault('TAX_API_URL', 'http://localhost:9001/tax');
const EXTERNAL_SYSTEM_MODE = () => envOrDefault('EXTERNAL_SYSTEM_MODE', 'real');

// Only the fields the monolith uses are required; anything else in the response is ignored.
const timeSchema = z.object({ time: z.iso.datetime({ offset: true }) });

const productDetailsSchema = z.object({ price: z.number() });

const promotionDetailsSchema = z.object({ promotionActive: z.boolean(), discount: z.number() });

const taxDetailsSchema = z.object({ taxRate: z.number() });

export type ErpProductDetails = z.infer<typeof productDetailsSchema>;
export type ErpPromotionDetails = z.infer<typeof promotionDetailsSchema>;
export type TaxDetails = z.infer<typeof taxDetailsSchema>;

/**
 * The ERP could not answer: it is unreachable, timed out, returned an error status or a malformed
 * response. A product the ERP reports as missing (404) is not this; that is a validation failure.
 */
export class ErpUnavailableError extends Error {
  constructor(message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = 'ErpUnavailableError';
  }
}

export async function getCurrentTime(): Promise<Date> {
  const mode = EXTERNAL_SYSTEM_MODE();
  if (mode === 'real') {
    return new Date();
  } else if (mode === 'stub') {
    return getStubTime();
  } else {
    throw new Error(`Unknown external system mode: ${mode}`);
  }
}

async function getStubTime(): Promise<Date> {
  const response = await fetchExternal(`${CLOCK_API_URL()}/api/time`);
  if (!response.ok) {
    throw new Error(`Failed to fetch current time: ${response.status}`);
  }
  const data = await parseResponse(response, timeSchema, 'current time');
  return new Date(data.time);
}

export async function getPromotionDetails(): Promise<ErpPromotionDetails> {
  return fromErp(async () => {
    const response = await fetchExternal(`${ERP_API_URL()}/api/promotion`);
    if (!response.ok) {
      throw new Error(`Failed to fetch promotion details: ${response.status}`);
    }
    return parseResponse(response, promotionDetailsSchema, 'promotion details');
  });
}

export async function getProductDetails(sku: string): Promise<ErpProductDetails | null> {
  return fromErp(async () => {
    const response = await fetchExternal(`${ERP_API_URL()}/api/products/${encodeURIComponent(sku)}`);
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new Error(`Failed to fetch product details: ${response.status}`);
    }
    return parseResponse(response, productDetailsSchema, 'product details');
  });
}

// Any failure to get an answer from the ERP (network error, timeout, error status, malformed body)
// surfaces as ErpUnavailableError, keeping the original error as its cause.
async function fromErp<T>(call: () => Promise<T>): Promise<T> {
  try {
    return await call();
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    throw new ErpUnavailableError(`ERP unavailable: ${reason}`, { cause: error });
  }
}

export async function getTaxDetails(country: string): Promise<TaxDetails | null> {
  const response = await fetchExternal(`${TAX_API_URL()}/api/countries/${encodeURIComponent(country)}`);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to fetch tax details for country ${country}: ${response.status}`);
  }
  return parseResponse(response, taxDetailsSchema, `tax details (country ${country})`);
}

function fetchExternal(url: string): Promise<Response> {
  return fetch(url, { cache: 'no-store', signal: AbortSignal.timeout(10000) });
}

// A malformed response fails here, naming what was fetched, instead of as a TypeError further down.
async function parseResponse<T>(response: Response, schema: z.ZodType<T>, what: string): Promise<T> {
  const body: unknown = await response.json();
  const result = schema.safeParse(body);
  if (!result.success) {
    throw new Error(`Malformed ${what} response: ${z.prettifyError(result.error)}`, { cause: result.error });
  }
  return result.data;
}
