import Decimal from 'decimal.js';
import { ComponentHarness } from '../support/component-harness';

/**
 * Drives the real place-order flow end-to-end through the in-process API: ClockGateway ->
 * ErpGateway (price + promotion) -> CouponService (DB) -> TaxGateway, all behind POST /api/orders
 * and read back via GET /api/orders/{n}. Externals are stubbed by in-process HTTP servers; DB is
 * Testcontainers Postgres.
 * Mirrors the Java PlaceOrderPositiveComponentTest / PlaceOrderNegativeComponentTest.
 */
describe('Place Order (component)', () => {
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

  interface OrderRequest {
    sku: string;
    quantity: number;
    country: string;
    couponCode?: string;
  }

  interface OrderDetails {
    basePrice: string;
    subtotalPrice: string;
    discountAmount: string;
    taxAmount: string;
    totalPrice: string;
    status: string;
    appliedCouponCode: string | null;
  }

  const placeAndFetch = async (body: OrderRequest): Promise<OrderDetails> => {
    const placed = await (await harness.customerApi())
      .post('/api/orders')
      .send(body);
    expect(placed.status).toBe(201);
    const { orderNumber } = placed.body as { orderNumber: string };
    expect(orderNumber).toBeDefined();

    const details = await (
      await harness.customerApi()
    ).get(`/api/orders/${orderNumber}`);
    expect(details.status).toBe(200);
    return details.body as OrderDetails;
  };

  it('computes totals from price, promotion and tax', async () => {
    harness.returnsTime('2026-03-10T12:00:00Z');
    harness.returnsProduct('BOOK-123', 10.0);
    harness.returnsPromotion(false, 1.0);
    harness.returnsTaxRate('US', 0.1);

    const order = await placeAndFetch({
      sku: 'BOOK-123',
      quantity: 2,
      country: 'US',
    });

    expect(order.basePrice).toBe('20.00'); // 10.00 x 2
    expect(order.subtotalPrice).toBe('20.00'); // no promo, no coupon
    expect(order.taxAmount).toBe('2.00'); // 20.00 x 0.10
    expect(order.totalPrice).toBe('22.00'); // 20.00 + 2.00
    expect(order.status).toBe('PLACED');
    expect(order.appliedCouponCode).toBeNull();
  });

  it('applies an active promotion discount', async () => {
    harness.returnsTime('2026-03-10T12:00:00Z');
    harness.returnsProduct('BOOK-123', 10.0);
    harness.returnsPromotion(true, 0.9);
    harness.returnsTaxRate('US', 0.1);

    const order = await placeAndFetch({
      sku: 'BOOK-123',
      quantity: 2,
      country: 'US',
    });

    expect(order.subtotalPrice).toBe('18.00'); // 20.00 x 0.9
    expect(order.taxAmount).toBe('1.80'); // 18.00 x 0.10
    expect(order.totalPrice).toBe('19.80');
  });

  it('applies a coupon discount', async () => {
    await harness.couponRepo.save(
      harness.couponRepo.create({
        code: 'SAVE20',
        discountRate: new Decimal('0.2000'),
        usageLimit: 100,
        usedCount: 0,
        validFrom: null,
        validTo: null,
      }),
    );

    harness.returnsTime('2026-03-10T12:00:00Z');
    harness.returnsProduct('BOOK-123', 10.0);
    harness.returnsPromotion(false, 1.0);
    harness.returnsTaxRate('US', 0.1);

    const order = await placeAndFetch({
      sku: 'BOOK-123',
      quantity: 2,
      country: 'US',
      couponCode: 'SAVE20',
    });

    expect(order.discountAmount).toBe('4.00'); // 20.00 x 0.20
    expect(order.subtotalPrice).toBe('16.00');
    expect(order.taxAmount).toBe('1.60'); // 16.00 x 0.10
    expect(order.totalPrice).toBe('17.60');
    expect(order.appliedCouponCode).toBe('SAVE20');
  });

  it('rejects an order during the New Year blackout', async () => {
    harness.returnsTime('2026-12-31T23:59:00Z');

    const customer = await harness.customerApi();
    const response = await customer
      .post('/api/orders')
      .send({ sku: 'BOOK-123', quantity: 2, country: 'US' });

    expect(response.status).toBe(422);
  });

  it('rejects an unknown product', async () => {
    harness.returnsTime('2026-03-10T12:00:00Z');
    harness.returnsNoProduct('MISSING-1');

    const customer = await harness.customerApi();
    const response = await customer
      .post('/api/orders')
      .send({ sku: 'MISSING-1', quantity: 1, country: 'US' });

    expect(response.status).toBe(422);
  });

  it('never lets concurrent orders exceed a coupon usage limit', async () => {
    await harness.couponRepo.save(
      harness.couponRepo.create({
        code: 'LIMITED2',
        discountRate: new Decimal('0.10'),
        validFrom: null,
        validTo: null,
        usageLimit: 2,
        usedCount: 0,
      }),
    );
    harness.returnsTime('2026-03-10T12:00:00Z');
    harness.returnsProduct('BOOK-123', 10.0);
    harness.returnsPromotion(false, 1.0);
    harness.returnsTaxRate('US', 0.1);

    const customer = await harness.customerApi();
    const responses = await Promise.all(
      Array.from({ length: 10 }, () =>
        customer.post('/api/orders').send({
          sku: 'BOOK-123',
          quantity: 1,
          country: 'US',
          couponCode: 'LIMITED2',
        }),
      ),
    );

    expect(responses.filter((r) => r.status === 201)).toHaveLength(2);
    expect(responses.filter((r) => r.status === 422)).toHaveLength(8);
    const coupon = await harness.couponRepo.findOneByOrFail({
      code: 'LIMITED2',
    });
    expect(coupon.usedCount).toBe(2);
    expect(
      await harness.orderRepo.countBy({ appliedCouponCode: 'LIMITED2' }),
    ).toBe(2);
  });

  it('rejects an invalid quantity with a field error instead of a server error', async () => {
    const customer = await harness.customerApi();
    const response = await customer
      .post('/api/orders')
      .send({ sku: 'BOOK-123', quantity: 'lala', country: 'US' });

    expect(response.status).toBe(422);
    expect(response.body).toMatchObject({
      errors: [
        {
          field: 'quantity',
          message: 'Quantity must be an integer',
          code: 'TYPE_MISMATCH',
        },
      ],
    });
  });
});
