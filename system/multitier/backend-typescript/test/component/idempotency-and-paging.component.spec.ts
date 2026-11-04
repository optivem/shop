import { ComponentHarness } from '../support/component-harness';

/**
 * Idempotent POST /api/orders and paged GET /api/orders, /api/coupons over real HTTP.
 * Mirrors the .NET IdempotencyAndPagingComponentTests.
 */
describe('Idempotency and paging (component)', () => {
  const harness = new ComponentHarness();
  const ORDER = { sku: 'BOOK-123', quantity: 2, country: 'US' };

  beforeAll(async () => {
    await harness.start();
  }, 120_000);

  afterAll(async () => {
    await harness.stop();
  }, 60_000);

  beforeEach(async () => {
    await harness.resetState();
    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProduct('BOOK-123', 10.0);
    harness.stubPromotion(false, 1.0);
    harness.stubTax('US', 0.1);
  });

  const place = async (body: object, key?: string) => {
    const api = await harness.customerApi('customer-a');
    const call = api.post('/api/orders');
    if (key !== undefined) {
      void call.set('Idempotency-Key', key);
    }
    return call.send(body);
  };

  it('replays the original response for the same key and body', async () => {
    const first = await place(ORDER, 'key-replay');
    const second = await place(ORDER, 'key-replay');

    expect(first.status).toBe(201);
    expect(second.status).toBe(201);
    expect(second.text).toBe(first.text);
    expect(second.headers.location).toBe(first.headers.location);
    expect(await harness.orderRepo.count()).toBe(1);
  });

  it('rejects the same key with a different body', async () => {
    await place(ORDER, 'key-conflict');
    const second = await place({ ...ORDER, quantity: 3 }, 'key-conflict');

    expect(second.status).toBe(422);
    expect(second.body).toMatchObject({
      detail: 'Idempotency-Key has already been used with a different request',
    });
    expect(await harness.orderRepo.count()).toBe(1);
  });

  it('places two orders when no key is sent', async () => {
    const first = await place(ORDER);
    const second = await place(ORDER);

    expect(first.status).toBe(201);
    expect(second.status).toBe(201);
    expect(second.body).not.toEqual(first.body);
    expect(await harness.orderRepo.count()).toBe(2);
  });

  it('scopes a key to the caller', async () => {
    await place(ORDER, 'shared-key');
    const other = await (
      await harness.customerApi('customer-b')
    )
      .post('/api/orders')
      .set('Idempotency-Key', 'shared-key')
      .send({ ...ORDER, quantity: 5 });

    expect(other.status).toBe(201);
    expect(await harness.orderRepo.count()).toBe(2);
  });

  it('rejects a key longer than 255 characters on the header field', async () => {
    const response = await place(ORDER, 'k'.repeat(256));

    expect(response.status).toBe(422);
    expect(response.body).toMatchObject({
      errors: [
        {
          field: 'Idempotency-Key',
          message: 'Idempotency-Key must be at most 255 characters',
        },
      ],
    });
  });

  it('reports the paging envelope with defaults, newest order first', async () => {
    const older = (await place(ORDER)).body as { orderNumber: string };
    harness.stubClock('2026-03-11T12:00:00Z');
    const newer = (await place(ORDER)).body as { orderNumber: string };

    const response = await harness.adminApi().get('/api/orders');

    expect(response.status).toBe(200);
    expect(response.body).toMatchObject({
      page: 1,
      size: 50,
      totalElements: 2,
      totalPages: 1,
    });
    const body = response.body as { orders: { orderNumber: string }[] };
    expect(body.orders.map((o) => o.orderNumber)).toEqual([
      newer.orderNumber,
      older.orderNumber,
    ]);

    const pastEnd = await harness.adminApi().get('/api/orders?page=2&size=2');
    expect(pastEnd.body).toMatchObject({ orders: [], page: 2, size: 2 });
  });

  it('pages coupons newest first', async () => {
    for (const code of ['PAGE-A', 'PAGE-B', 'PAGE-C']) {
      const published = await harness
        .adminApi()
        .post('/api/coupons')
        .send({ code, discountRate: '0.1' });
      expect(published.status).toBe(204);
    }

    const response = await harness.adminApi().get('/api/coupons?page=1&size=2');

    expect(response.status).toBe(200);
    expect(response.body).toMatchObject({
      page: 1,
      size: 2,
      totalElements: 3,
      totalPages: 2,
    });
    const body = response.body as { coupons: { code: string }[] };
    expect(body.coupons.map((c) => c.code)).toEqual(['PAGE-C', 'PAGE-B']);
  });

  it.each([
    ['/api/orders?page=0', 'page', 'Page must be 1 or greater'],
    ['/api/orders?size=101', 'size', 'Page size must be between 1 and 100'],
    ['/api/coupons?page=0', 'page', 'Page must be 1 or greater'],
    ['/api/coupons?size=101', 'size', 'Page size must be between 1 and 100'],
  ])('rejects %s on field %s', async (path, field, message) => {
    const response = await harness.adminApi().get(path);

    expect(response.status).toBe(422);
    expect(response.body).toMatchObject({ errors: [{ field, message }] });
  });
});
