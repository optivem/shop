const ERP_API_URL = () => process.env.ERP_API_URL || 'http://localhost:9001/erp';
const CLOCK_API_URL = () => process.env.CLOCK_API_URL || 'http://localhost:9001/clock';

export async function getCurrentTime(): Promise<Date> {
  const url = `${CLOCK_API_URL()}/api/time`;
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    throw new Error(`Failed to fetch current time: ${response.status}`);
  }
  const data = await response.json() as { time: string };
  return new Date(data.time);
}

export interface ProductDetails {
  price: number;
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
  const data = await response.json() as { price: number };
  return { price: data.price };
}
