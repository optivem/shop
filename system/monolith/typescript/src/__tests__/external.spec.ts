import { ErpUnavailableError, getProductDetails, getProducts, getPromotionDetails, getTaxDetails } from '../lib/external';

function mockFetch(status: number, body: unknown) {
  jest.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

afterEach(() => {
  jest.restoreAllMocks();
});

describe('getProductDetails', () => {
  it('returns the parsed product', async () => {
    mockFetch(200, { id: 'HP-15', title: 'HP Laptop 15', price: 699.99 });
    await expect(getProductDetails('HP-15')).resolves.toEqual({ id: 'HP-15', title: 'HP Laptop 15', price: 699.99 });
  });

  it('returns null when the product does not exist', async () => {
    mockFetch(404, {});
    await expect(getProductDetails('UNKNOWN')).resolves.toBeNull();
  });

  it('fails with a clear message when the response is malformed', async () => {
    mockFetch(200, { id: 'HP-15', price: '699.99' });
    await expect(getProductDetails('HP-15')).rejects.toThrow('Malformed product details response');
  });

  it('reports the ERP as unavailable when it answers with an error status', async () => {
    mockFetch(503, {});
    await expect(getProductDetails('HP-15')).rejects.toThrow(ErpUnavailableError);
  });

  it('reports the ERP as unavailable when it cannot be reached', async () => {
    jest.spyOn(global, 'fetch').mockRejectedValue(new TypeError('fetch failed'));
    await expect(getProductDetails('HP-15')).rejects.toThrow(ErpUnavailableError);
  });
});

describe('getProducts', () => {
  it('returns the parsed product list', async () => {
    const product = { id: 'BOOK-123', title: 'Clean Code', description: 'Craftsmanship', price: 12, category: 'Books', brand: 'Prentice Hall' };
    mockFetch(200, [product]);
    await expect(getProducts()).resolves.toEqual([product]);
  });

  it('reports the ERP as unavailable when it answers with an error status', async () => {
    mockFetch(503, {});
    await expect(getProducts()).rejects.toThrow(ErpUnavailableError);
  });

  it('reports the ERP as unavailable when it cannot be reached', async () => {
    jest.spyOn(global, 'fetch').mockRejectedValue(new TypeError('fetch failed'));
    await expect(getProducts()).rejects.toThrow(ErpUnavailableError);
  });
});

describe('getPromotionDetails', () => {
  it('reports the ERP as unavailable when it answers with an error status', async () => {
    mockFetch(503, {});
    await expect(getPromotionDetails()).rejects.toThrow(ErpUnavailableError);
  });
});

describe('getTaxDetails', () => {
  it('fails with a clear message when the tax rate is missing', async () => {
    mockFetch(200, { id: 'US', countryName: 'United States' });
    await expect(getTaxDetails('US')).rejects.toThrow('Malformed tax details (country US) response');
  });
});
