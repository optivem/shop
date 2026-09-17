import { getProductDetails, getTaxDetails } from '../lib/external';

function mockFetch(status: number, body: unknown) {
  jest.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

afterEach(() => {
  jest.restoreAllMocks();
});

describe('getProductDetails', () => {
  it('returns the parsed product', async () => {
    mockFetch(200, { id: 'HP-15', title: 'HP Laptop 15', price: 699.99 });
    await expect(getProductDetails('HP-15')).resolves.toEqual({ price: 699.99 });
  });

  it('returns null when the product does not exist', async () => {
    mockFetch(404, {});
    await expect(getProductDetails('UNKNOWN')).resolves.toBeNull();
  });

  it('fails with a clear message when the response is malformed', async () => {
    mockFetch(200, { id: 'HP-15', price: '699.99' });
    await expect(getProductDetails('HP-15')).rejects.toThrow('Malformed product details response');
  });
});

describe('getTaxDetails', () => {
  it('fails with a clear message when the tax rate is missing', async () => {
    mockFetch(200, { id: 'US', countryName: 'United States' });
    await expect(getTaxDetails('US')).rejects.toThrow('Malformed tax details (country US) response');
  });
});
