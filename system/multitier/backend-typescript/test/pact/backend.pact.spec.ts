import { Verifier } from '@pact-foundation/pact';
import Decimal from 'decimal.js';
import * as path from 'path';
import { ComponentHarness } from '../support/component-harness';
import { Order } from '../../src/core/entities/order.entity';
import { OrderStatus } from '../../src/core/entities/order-status.enum';

describe('Backend Pact Provider Verification', () => {
  const harness = new ComponentHarness();

  beforeAll(async () => {
    await harness.start();
  }, 120_000);

  afterAll(async () => {
    await harness.stop();
  }, 60_000);

  const sampleOrder = (): Partial<Order> => ({
    orderTimestamp: new Date('2026-03-10T12:00:00Z'),
    country: 'US',
    sku: 'BOOK-123',
    quantity: 2,
    unitPrice: new Decimal('10.00'),
    basePrice: new Decimal('20.00'),
    discountRate: new Decimal('0.0000'),
    discountAmount: new Decimal('0.00'),
    subtotalPrice: new Decimal('20.00'),
    taxRate: new Decimal('0.1000'),
    taxAmount: new Decimal('2.00'),
    totalPrice: new Decimal('22.00'),
    status: OrderStatus.PLACED,
    appliedCouponCode: null,
  });

  it('verifies the frontend consumer contract', async () => {
    // __dirname is system/multitier/backend-typescript/test/pact — five levels below the repo
    // root, where the consumer-owned contracts/ folder lives (same neutral location Java's
    // @PactFolder and the .NET verifier read from).
    const pactFile = path.resolve(
      __dirname,
      '../../../../../contracts/frontend-backend.json',
    );

    await new Verifier({
      provider: 'backend',
      providerBaseUrl: harness.baseUrl(),
      pactUrls: [pactFile],
      stateHandlers: {
        'product BOOK-123 exists and US is taxable': async () => {
          await harness.resetState();
          harness.stubClock('2026-03-10T12:00:00Z');
          harness.stubProduct('BOOK-123', 10.0);
          harness.stubPromotion(false, 1.0);
          harness.stubTax('US', 0.1);
        },

        'order placement is blocked by the New Year blackout': async () => {
          await harness.resetState();
          harness.stubClock('2026-12-31T23:59:00Z');
        },

        // Everything the order needs is in place — the coupon is the one thing missing, so the
        // rejection the frontend renders is the coupon's and not something else failing first.
        // resetState empties the coupon table, so nothing has to be un-seeded.
        'coupon INVALIDCOUPON does not exist': async () => {
          await harness.resetState();
          harness.stubClock('2026-03-10T12:00:00Z');
          harness.stubProduct('BOOK-123', 10.0);
          harness.stubPromotion(false, 1.0);
          harness.stubTax('US', 0.1);
        },

        // ERP is the one that says a SKU is not a product. The clock still has to answer: it is
        // consulted before the product is looked up.
        'product NON-EXISTENT-SKU-12345 does not exist': async () => {
          await harness.resetState();
          harness.stubClock('2026-03-10T12:00:00Z');
          harness.stubProductMissing('NON-EXISTENT-SKU-12345');
        },

        // The product must resolve for the country to be the thing that fails — Tax is consulted
        // only after ERP has answered.
        'product BOOK-123 exists and XX is not taxable': async () => {
          await harness.resetState();
          harness.stubClock('2026-03-10T12:00:00Z');
          harness.stubProduct('BOOK-123', 10.0);
          harness.stubPromotion(false, 1.0);
          harness.stubTaxMissing('XX');
        },

        // 22:15 on December 31st — inside the cancellation blackout, with a cancellable order to
        // aim at.
        'order cancellation is blocked by the New Year blackout': async () => {
          await harness.resetState();
          harness.stubClock('2026-12-31T22:15:00Z');
          await harness.orderRepo.save(
            harness.orderRepo.create({
              ...sampleOrder(),
              orderNumber: 'ORD-1',
            }),
          );
        },

        'at least one order exists': async () => {
          await harness.resetState();
          await harness.orderRepo.save(
            harness.orderRepo.create({
              ...sampleOrder(),
              orderNumber: 'ORD-HIST-1',
            }),
          );
        },

        // Shared by viewing ORD-1 and by cancelling it. Cancelling asks the clock before it touches
        // the order (the blackout is decided first), so the clock is stubbed here too — at a date
        // well clear of the blackout, which is what makes the cancellation succeed.
        'order ORD-1 is placed': async () => {
          await harness.resetState();
          harness.stubClock('2026-03-10T12:00:00Z');
          await harness.orderRepo.save(
            harness.orderRepo.create({
              ...sampleOrder(),
              orderNumber: 'ORD-1',
            }),
          );
        },

        'order ORD-1 is cancelled': async () => {
          await harness.resetState();
          await harness.orderRepo.save(
            harness.orderRepo.create({
              ...sampleOrder(),
              orderNumber: 'ORD-1',
              status: OrderStatus.CANCELLED,
            }),
          );
        },

        'order ORD-1 is delivered': async () => {
          await harness.resetState();
          await harness.orderRepo.save(
            harness.orderRepo.create({
              ...sampleOrder(),
              orderNumber: 'ORD-1',
              status: OrderStatus.DELIVERED,
            }),
          );
        },

        'no order UNKNOWN exists': async () => {
          await harness.resetState();
          // DB is empty after resetState.
        },

        'at least one coupon exists': async () => {
          await harness.resetState();
          await harness.couponRepo.save(
            harness.couponRepo.create({
              code: 'SAVE10',
              discountRate: new Decimal('0.2000'),
              usageLimit: 100,
              usedCount: 0,
              validFrom: null,
              validTo: null,
            }),
          );
        },

        'coupon SAVE10 exists': async () => {
          await harness.resetState();
          harness.stubClock('2026-03-10T12:00:00Z');
          harness.stubProduct('BOOK-123', 10.0);
          harness.stubPromotion(false, 1.0);
          harness.stubTax('US', 0.1);
          await harness.couponRepo.save(
            harness.couponRepo.create({
              code: 'SAVE10',
              discountRate: new Decimal('0.2000'),
              usageLimit: 100,
              usedCount: 0,
              validFrom: null,
              validTo: null,
            }),
          );
        },

        'no coupon SAVE10 exists yet': async () => {
          await harness.resetState();
          // DB is empty after resetState.
        },
      },
    }).verifyProvider();
  });
});
