import { z } from 'zod';
import { envOrDefault } from './env';
import { HttpStatusError, callIdempotent } from './resilience';

const ERP_API_URL = () => envOrDefault('ERP_API_URL', 'http://localhost:9001/erp');
const CLOCK_API_URL = () => envOrDefault('CLOCK_API_URL', 'http://localhost:9001/clock');
const TAX_API_URL = () => envOrDefault('TAX_API_URL', 'http://localhost:9001/tax');
const EXTERNAL_SYSTEM_MODE = () => envOrDefault('EXTERNAL_SYSTEM_MODE', 'real');

// Only the fields the monolith uses are required; anything else in the response is ignored.
const timeSchema = z.object({ time: z.iso.datetime({ offset: true }) });

// ERP product JSON: {id, title, description, price, category, brand}. Order placement needs only the price;
// the catalog endpoints pass the descriptive fields through.
const productDetailsSchema = z.object({
  id: z.string().optional(),
  title: z.string().optional(),
  description: z.string().optional(),
  price: z.number(),
  category: z.string().optional(),
  brand: z.string().optional(),
});

const productListSchema = z.array(productDetailsSchema);

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
  const data = await callIdempotent('clock', async (signal) => {
    const response = await fetchExternal(`${CLOCK_API_URL()}/api/time`, signal);
    if (!response.ok) {
      throw new HttpStatusError(`Failed to fetch current time: ${response.status}`, response.status);
    }
    return parseResponse(response, timeSchema, 'current time');
  });
  return new Date(data.time);
}

export async function getPromotionDetails(): Promise<ErpPromotionDetails> {
  return fromErp(async (signal) => {
    const response = await fetchExternal(`${ERP_API_URL()}/api/promotion`, signal);
    if (!response.ok) {
      throw new HttpStatusError(`Failed to fetch promotion details: ${response.status}`, response.status);
    }
    return parseResponse(response, promotionDetailsSchema, 'promotion details');
  });
}

export async function getProductDetails(sku: string): Promise<ErpProductDetails | null> {
  return fromErp(async (signal) => {
    const response = await fetchExternal(`${ERP_API_URL()}/api/products/${encodeURIComponent(sku)}`, signal);
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new HttpStatusError(`Failed to fetch product details: ${response.status}`, response.status);
    }
    return parseResponse(response, productDetailsSchema, 'product details');
  });
}

export async function getProducts(): Promise<ErpProductDetails[]> {
  return fromErp(async (signal) => {
    const response = await fetchExternal(`${ERP_API_URL()}/api/products`, signal);
    if (!response.ok) {
      throw new HttpStatusError(`Failed to fetch products: ${response.status}`, response.status);
    }
    return parseResponse(response, productListSchema, 'product list');
  });
}

// Every ERP call runs through the ERP's timeout/retry/breaker policy. Any failure to get an answer
// (network error, timeout, error status, malformed body, open breaker) surfaces as
// ErpUnavailableError, keeping the original error as its cause.
async function fromErp<T>(call: (signal: AbortSignal) => Promise<T>): Promise<T> {
  try {
    return await callIdempotent('erp', call);
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    throw new ErpUnavailableError(`ERP unavailable: ${reason}`, { cause: error });
  }
}

export async function getTaxDetails(country: string): Promise<TaxDetails | null> {
  return callIdempotent('tax', async (signal) => {
    const response = await fetchExternal(`${TAX_API_URL()}/api/countries/${encodeURIComponent(country)}`, signal);
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new HttpStatusError(
        `Failed to fetch tax details for country ${country}: ${response.status}`,
        response.status
      );
    }
    return parseResponse(response, taxDetailsSchema, `tax details (country ${country})`);
  });
}

function fetchExternal(url: string, signal: AbortSignal): Promise<Response> {
  return fetch(url, { cache: 'no-store', signal });
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
