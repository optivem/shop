import Decimal from 'decimal.js';
import { jsonResponseWithDecimals } from '../lib/decimal-format';
import { resetExternalCallPolicies } from '../lib/resilience';
import { ErpUnavailableError } from '../lib/external';
import { browseProducts, productNotFoundDetail, viewProduct } from '../lib/products';

function mockFetch(status: number, body: unknown) {
  jest.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

const erpProduct = {
  id: 'BOOK-123',
  title: 'Clean Code',
  description: 'A handbook of agile software craftsmanship',
  price: 12,
  category: 'Books',
  brand: 'Prentice Hall',
};

const expectedProduct = {
  sku: 'BOOK-123',
  title: 'Clean Code',
  description: 'A handbook of agile software craftsmanship',
  brand: 'Prentice Hall',
  category: 'Books',
  price: new Decimal(12),
};

afterEach(() => {
  jest.restoreAllMocks();
  resetExternalCallPolicies();
});

describe('browseProducts', () => {
  it('proxies the ERP catalog', async () => {
    mockFetch(200, [erpProduct, { ...erpProduct, id: 'BOOK-456', price: 15.5 }]);
    await expect(browseProducts()).resolves.toEqual([expectedProduct, { ...expectedProduct, sku: 'BOOK-456', price: new Decimal(15.5) }]);
  });

  it('returns an empty list when the ERP has no products', async () => {
    mockFetch(200, []);
    await expect(browseProducts()).resolves.toEqual([]);
  });

  it('reports the ERP as unavailable when it fails', async () => {
    mockFetch(503, {});
    await expect(browseProducts()).rejects.toThrow(ErpUnavailableError);
  });
});

describe('viewProduct', () => {
  it('proxies the ERP product', async () => {
    mockFetch(200, erpProduct);
    await expect(viewProduct('BOOK-123')).resolves.toEqual(expectedProduct);
  });

  it('returns null when the ERP does not know the SKU', async () => {
    mockFetch(404, {});
    await expect(viewProduct('UNKNOWN')).resolves.toBeNull();
  });

  it('names the unknown SKU in the not-found detail', () => {
    expect(productNotFoundDetail('UNKNOWN')).toBe('Product UNKNOWN does not exist.');
  });
});

describe('product JSON', () => {
  it('serializes the price as a 2-decimal string', async () => {
    mockFetch(200, [erpProduct]);
    const response = jsonResponseWithDecimals(await browseProducts());
    await expect(response.json()).resolves.toEqual([{ ...expectedProduct, price: '12.00' }]);
  });
});
