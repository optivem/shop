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

  const placeOrder = async (customer = 'customer-a'): Promise<string> => {
    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProduct('BOOK-123', 10.0);
    harness.stubPromotion(false, 1.0);
    harness.stubTax('US', 0.1);

    const placed = await (await harness.customerApi(customer))
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

  it('customer sees only their own orders in history', async () => {
    const mine = await placeOrder('customer-a');
    const theirs = await placeOrder('customer-b');

    const response = await (
      await harness.customerApi('customer-a')
    ).get('/api/orders');

    expect(response.status).toBe(200);
    const body = response.body as { orders: { orderNumber: string }[] };
    const orderNumbers = body.orders.map((o) => o.orderNumber);
    expect(orderNumbers).toContain(mine);
    expect(orderNumbers).not.toContain(theirs);
  });

  it('admin sees all orders with the customer shown', async () => {
    const mine = await placeOrder('customer-a');
    const theirs = await placeOrder('customer-b');

    const response = await harness.adminApi().get('/api/orders');

    const body = response.body as {
      orders: { orderNumber: string; customer: string }[];
    };
    expect(body.orders.find((o) => o.orderNumber === mine)?.customer).toBe(
      'customer-a',
    );
    expect(body.orders.find((o) => o.orderNumber === theirs)?.customer).toBe(
      'customer-b',
    );
  });

  it("another customer's order is Not Found, an admin can view and cancel it", async () => {
    const theirs = await placeOrder('customer-b');
    const other = await harness.customerApi('customer-a');

    const asOther = await other.get(`/api/orders/${theirs}`);
    expect(asOther.status).toBe(404);
    expect(asOther.body).toMatchObject({
      detail: `Order ${theirs} does not exist.`,
    });
    expect((await other.post(`/api/orders/${theirs}/cancel`)).status).toBe(404);

    const asAdmin = await harness.adminApi().get(`/api/orders/${theirs}`);
    expect(asAdmin.status).toBe(200);
    const cancelAsAdmin = await harness
      .adminApi()
      .post(`/api/orders/${theirs}/cancel`);
    expect(cancelAsAdmin.status).toBe(204);
  });

  it('admin cannot place an order', async () => {
    const response = await harness
      .adminApi()
      .post('/api/orders')
      .send({ sku: 'BOOK-123', quantity: 2, country: 'US' });

    expect(response.status).toBe(403);
  });

  it('view missing order returns Not Found', async () => {
    const response = await harness.adminApi().get('/api/orders/UNKNOWN');

    expect(response.status).toBe(404);
  });
});
