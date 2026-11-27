import Decimal from 'decimal.js';
import { jsonResponseWithDecimals } from '../lib/decimal-format';
import { resetExternalCallPolicies } from '../lib/resilience';
import { ErpUnavailableError } from '../lib/external';
import { browseProducts, productNotFoundDetail, viewProduct, viewProductImage } from '../lib/products';

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
  imageUrl: '/erp/images/BOOK-123.jpg',
};

const expectedProduct = {
  sku: 'BOOK-123',
  title: 'Clean Code',
  description: 'A handbook of agile software craftsmanship',
  brand: 'Prentice Hall',
  category: 'Books',
  price: new Decimal(12),
  imageUrl: '/api/products/BOOK-123/image',
};

afterEach(() => {
  jest.restoreAllMocks();
  resetExternalCallPolicies();
});

describe('browseProducts', () => {
  it('proxies the ERP catalog', async () => {
    mockFetch(200, [erpProduct, { ...erpProduct, id: 'BOOK-456', price: 15.5 }]);
    await expect(browseProducts()).resolves.toEqual([expectedProduct, { ...expectedProduct, sku: 'BOOK-456', price: new Decimal(15.5), imageUrl: '/api/products/BOOK-456/image' }]);
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

describe('product image URL', () => {
  it('is null when the ERP has no image', async () => {
    mockFetch(200, { ...erpProduct, imageUrl: undefined });
    await expect(viewProduct('BOOK-123')).resolves.toEqual({ ...expectedProduct, imageUrl: null });
  });

  it('is null when the ERP image reference is blank', async () => {
    mockFetch(200, { ...erpProduct, imageUrl: '  ' });
    await expect(viewProduct('BOOK-123')).resolves.toEqual({ ...expectedProduct, imageUrl: null });
  });
});

describe('viewProductImage', () => {
  const imageResponse = () => new Response(new Uint8Array([1, 2, 3]), { status: 200, headers: { 'content-type': 'image/jpeg' } });

  it('proxies the ERP image, resolved against the ERP base URL', async () => {
    const fetchMock = jest
      .spyOn(global, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify(erpProduct), { status: 200 }))
      .mockResolvedValueOnce(imageResponse());
    const result = await viewProductImage('BOOK-123');
    expect(result).toEqual({ kind: 'found', image: { content: Buffer.from([1, 2, 3]), contentType: 'image/jpeg' } });
    expect(String(fetchMock.mock.calls[1]?.[0] as string)).toBe('http://localhost:9001/erp/images/BOOK-123.jpg');
  });

  it('is not found when the product is unknown', async () => {
    mockFetch(404, {});
    await expect(viewProductImage('UNKNOWN')).resolves.toEqual({ kind: 'not-found', detail: 'Product UNKNOWN does not exist.' });
  });

  it('is not found when the product has no image', async () => {
    mockFetch(200, { ...erpProduct, imageUrl: undefined });
    await expect(viewProductImage('BOOK-123')).resolves.toEqual({ kind: 'not-found', detail: 'Product BOOK-123 has no image.' });
  });

  it('is not found when the ERP no longer has the image', async () => {
    jest
      .spyOn(global, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify(erpProduct), { status: 200 }))
      .mockResolvedValueOnce(new Response('', { status: 404 }));
    await expect(viewProductImage('BOOK-123')).resolves.toEqual({
      kind: 'not-found',
      detail: 'Image of product BOOK-123 does not exist.',
    });
  });

  it('reports the ERP as unavailable when the image fetch fails', async () => {
    jest
      .spyOn(global, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify(erpProduct), { status: 200 }))
      .mockResolvedValue(new Response('', { status: 503 }));
    await expect(viewProductImage('BOOK-123')).rejects.toThrow(ErpUnavailableError);
  });
});

describe('product JSON', () => {
  it('serializes the price as a 2-decimal string', async () => {
    mockFetch(200, [erpProduct]);
    const response = jsonResponseWithDecimals(await browseProducts());
    await expect(response.json()).resolves.toEqual([{ ...expectedProduct, price: '12.00' }]);
  });
});
