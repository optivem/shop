const ERP_API_URL = () => process.env.ERP_API_URL || 'http://localhost:9001/erp';
const CLOCK_API_URL = () => process.env.CLOCK_API_URL || 'http://localhost:9001/clock';
const TAX_API_URL = () => process.env.TAX_API_URL || 'http://localhost:9001/tax';
const EXTERNAL_SYSTEM_MODE = () => process.env.EXTERNAL_SYSTEM_MODE || 'real';

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
  const url = `${CLOCK_API_URL()}/api/time`;
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    throw new Error(`Failed to fetch current time: ${response.status}`);
  }
  const data = await response.json() as { time: string };
  return new Date(data.time);
}

export interface ProductDetails {
  id: string;
  price: number;
}

export interface PromotionDetails {
  promotionActive: boolean;
  discount: number;
}

export async function getPromotionDetails(): Promise<PromotionDetails> {
  const url = `${ERP_API_URL()}/api/promotion`;
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    throw new Error(`Failed to fetch promotion details: ${response.status}`);
  }
  const data = await response.json() as { promotionActive: boolean; discount: number };
  return { promotionActive: data.promotionActive, discount: data.discount };
}

export async function getProductDetails(sku: string): Promise<ProductDetails | null> {
  const url = `${ERP_API_URL()}/api/products/${encodeURIComponent(sku)}`;
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to fetch product details: ${response.status}`);
  }
  const data = await response.json() as { id: string; price: number };
  return { id: data.id, price: data.price };
}

export interface TaxDetails {
  id: string;
  countryName: string;
  taxRate: number;
}

export async function getTaxDetails(country: string): Promise<TaxDetails | null> {
  const url = `${TAX_API_URL()}/api/countries/${encodeURIComponent(country)}`;
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Failed to fetch tax details for country ${country}: ${response.status}`);
  }
  return await response.json() as TaxDetails;
}
