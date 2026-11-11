import { resetExternalCallPolicies } from '../lib/resilience';
import { ErpUnavailableError, getProductDetails, getProducts, getPromotionDetails, getTaxDetails } from '../lib/external';

function mockFetch(status: number, body: unknown) {
  jest.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

afterEach(() => {
  jest.restoreAllMocks();
  resetExternalCallPolicies();
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

// A fetch that only settles when the caller aborts it, like an ERP that never answers.
function hangingFetch(_input: string | URL | Request, init?: RequestInit): Promise<Response> {
  return new Promise((_resolve, reject) => {
    init?.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
  });
}

describe('ERP resilience', () => {
  const originalTimeout = process.env.EXTERNAL_TIMEOUT_MS;

  beforeEach(() => {
    process.env.EXTERNAL_TIMEOUT_MS = '100';
  });

  afterEach(() => {
    if (originalTimeout === undefined) {
      delete process.env.EXTERNAL_TIMEOUT_MS;
    } else {
      process.env.EXTERNAL_TIMEOUT_MS = originalTimeout;
    }
  });

  it('does not retry a missing product', async () => {
    mockFetch(404, {});
    await expect(getProductDetails('UNKNOWN')).resolves.toBeNull();
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('retries a 5xx and succeeds when the ERP recovers', async () => {
    jest
      .spyOn(global, 'fetch')
      .mockResolvedValueOnce(new Response('{}', { status: 503 }))
      .mockResolvedValueOnce(new Response('{}', { status: 503 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 'HP-15', price: 699.99 }), { status: 200 }));
    await expect(getProductDetails('HP-15')).resolves.toEqual({ id: 'HP-15', price: 699.99 });
    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('gives up after 3 attempts', async () => {
    mockFetch(503, {});
    await expect(getProductDetails('HP-15')).rejects.toThrow(ErpUnavailableError);
    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('times out a slow ERP and reports it as unavailable', async () => {
    const fetchSpy = jest.spyOn(global, 'fetch').mockImplementation(hangingFetch);
    const started = Date.now();
    await expect(getProductDetails('HP-15')).rejects.toThrow(ErpUnavailableError);
    expect(fetchSpy).toHaveBeenCalledTimes(3);
    expect(fetchSpy.mock.calls.every(([, init]) => init?.signal?.aborted)).toBe(true);
    expect(Date.now() - started).toBeLessThan(3000);
  });

  it('opens the breaker and then fails fast without calling the ERP', async () => {
    mockFetch(503, {});
    await expect(getProducts()).rejects.toThrow(ErpUnavailableError);
    await expect(getProducts()).rejects.toThrow(ErpUnavailableError);
    const callsWhenOpened = jest.mocked(fetch).mock.calls.length;

    await expect(getProducts()).rejects.toThrow(ErpUnavailableError);
    expect(fetch).toHaveBeenCalledTimes(callsWhenOpened);
  });
});
