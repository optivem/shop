import Decimal from 'decimal.js';
import request from 'supertest';
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
    basePrice: number;
    subtotalPrice: number;
    discountAmount: number;
    taxAmount: number;
    totalPrice: number;
    status: string;
    appliedCouponCode: string | null;
  }

  const placeAndFetch = async (body: OrderRequest): Promise<OrderDetails> => {
    const placed = await request(harness.httpServer())
      .post('/api/orders')
      .send(body);
    expect(placed.status).toBe(201);
    const { orderNumber } = placed.body as { orderNumber: string };
    expect(orderNumber).toBeDefined();

    const details = await request(harness.httpServer()).get(
      `/api/orders/${orderNumber}`,
    );
    expect(details.status).toBe(200);
    return details.body as OrderDetails;
  };

  it('computes totals from price, promotion and tax', async () => {
    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProduct('BOOK-123', 10.0);
    harness.stubPromotion(false, 1.0);
    harness.stubTax('US', 0.1);

    const order = await placeAndFetch({
      sku: 'BOOK-123',
      quantity: 2,
      country: 'US',
    });

    expect(Number(order.basePrice)).toBeCloseTo(20.0); // 10.00 x 2
    expect(Number(order.subtotalPrice)).toBeCloseTo(20.0); // no promo, no coupon
    expect(Number(order.taxAmount)).toBeCloseTo(2.0); // 20.00 x 0.10
    expect(Number(order.totalPrice)).toBeCloseTo(22.0); // 20.00 + 2.00
    expect(order.status).toBe('PLACED');
    expect(order.appliedCouponCode).toBeNull();
  });

  it('applies an active promotion discount', async () => {
    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProduct('BOOK-123', 10.0);
    harness.stubPromotion(true, 0.9);
    harness.stubTax('US', 0.1);

    const order = await placeAndFetch({
      sku: 'BOOK-123',
      quantity: 2,
      country: 'US',
    });

    expect(Number(order.subtotalPrice)).toBeCloseTo(18.0); // 20.00 x 0.9
    expect(Number(order.taxAmount)).toBeCloseTo(1.8); // 18.00 x 0.10
    expect(Number(order.totalPrice)).toBeCloseTo(19.8);
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

    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProduct('BOOK-123', 10.0);
    harness.stubPromotion(false, 1.0);
    harness.stubTax('US', 0.1);

    const order = await placeAndFetch({
      sku: 'BOOK-123',
      quantity: 2,
      country: 'US',
      couponCode: 'SAVE20',
    });

    expect(Number(order.discountAmount)).toBeCloseTo(4.0); // 20.00 x 0.20
    expect(Number(order.subtotalPrice)).toBeCloseTo(16.0);
    expect(Number(order.taxAmount)).toBeCloseTo(1.6); // 16.00 x 0.10
    expect(Number(order.totalPrice)).toBeCloseTo(17.6);
    expect(order.appliedCouponCode).toBe('SAVE20');
  });

  it('rejects an order during the New Year blackout', async () => {
    harness.stubClock('2026-12-31T23:59:00Z');

    const response = await request(harness.httpServer())
      .post('/api/orders')
      .send({ sku: 'BOOK-123', quantity: 2, country: 'US' });

    expect(response.status).toBe(422);
  });

  it('rejects an unknown product', async () => {
    harness.stubClock('2026-03-10T12:00:00Z');
    harness.stubProductMissing('MISSING-1');

    const response = await request(harness.httpServer())
      .post('/api/orders')
      .send({ sku: 'MISSING-1', quantity: 1, country: 'US' });

    expect(response.status).toBe(422);
  });
});
