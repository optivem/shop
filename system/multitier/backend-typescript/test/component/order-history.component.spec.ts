import { ComponentHarness } from '../support/component-harness';

/**
 * Browse order history and view-details flows, including the 404 path for a missing order.
 * Mirrors the Java OrderHistoryComponentTest.
 */
describe('Order History (component)', () => {
  const harness = new ComponentHarness();

  beforeAll(async () => {
    await harness.start();
  }, 120_000);

  afterAll(async () => {
    await harness.stop();
  }, 60_000);

  beforeEach(async () => {
    await harness.resetState();
  });

  const placeOrder = async (): Promise<string> => {
    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProduct('BOOK-123', 10.0);
    harness.stubPromotion(false, 1.0);
    harness.stubTax('US', 0.1);

    const placed = await harness
      .adminApi()
      .post('/api/orders')
      .send({ sku: 'BOOK-123', quantity: 2, country: 'US' });
    expect(placed.status).toBe(201);
    return (placed.body as { orderNumber: string }).orderNumber;
  };

  it('browse returns placed orders', async () => {
    const orderNumber = await placeOrder();

    const response = await harness.adminApi().get('/api/orders');

    expect(response.status).toBe(200);
    const body = response.body as { orders: { orderNumber: string }[] };
    const orderNumbers = body.orders.map((o) => o.orderNumber);
    expect(orderNumbers).toContain(orderNumber);
  });

  it('view missing order returns Not Found', async () => {
    const response = await harness.adminApi().get('/api/orders/UNKNOWN');

    expect(response.status).toBe(404);
  });
});
