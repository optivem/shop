import type { INestApplication } from '@nestjs/common';
import Decimal from 'decimal.js';
import { Test } from '@nestjs/testing';
import { DataSource, type Repository } from 'typeorm';
import {
  PostgreSqlContainer,
  type StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import { AppModule } from '../../app.module';
import { Order } from '../entities/order.entity';
import { Coupon } from '../entities/coupon.entity';
import { IdempotencyKey } from '../entities/idempotency-key.entity';
import { OrderStatus } from '../entities/order-status.enum';
import { OrderService } from './order.service';
import { CouponService } from './coupon.service';
import { ErpGateway } from './external/erp.gateway';
import { TaxGateway } from './external/tax.gateway';
import { ClockGateway } from './external/clock.gateway';
import { PlaceOrderRequest } from '../dtos/place-order-request.dto';
import { applyMigrations } from '../../../test/support/migrations';
import type { CurrentUser } from '../../auth/current-user';

const CUSTOMER: CurrentUser = {
  subject: 'sub-customer',
  username: 'customer1',
  admin: false,
};
const ADMIN: CurrentUser = {
  subject: 'sub-admin',
  username: 'admin1',
  admin: true,
};

/**
 * Races and paging against real Postgres: the unique index on (owner, idempotency_key) is what keeps
 * concurrent same-key requests to one order, and LIMIT/OFFSET ordering is a database behaviour.
 * The external systems are replaced by fixed answers; everything else is the real AppModule.
 */
describe('OrderService [integration]', () => {
  let app: INestApplication;
  let postgres: StartedPostgreSqlContainer;
  let orderRepo: Repository<Order>;
  let couponRepo: Repository<Coupon>;
  let keyRepo: Repository<IdempotencyKey>;
  let orderService: OrderService;
  let couponService: CouponService;

  beforeAll(async () => {
    postgres = await new PostgreSqlContainer('postgres:16-alpine')
      .withDatabase('app')
      .withUsername('app')
      .withPassword('app')
      .start();

    await applyMigrations(postgres);

    process.env.POSTGRES_DB_HOST = postgres.getHost();
    process.env.POSTGRES_DB_PORT = String(postgres.getPort());
    process.env.POSTGRES_DB_NAME = postgres.getDatabase();
    process.env.POSTGRES_DB_USER = postgres.getUsername();
    process.env.POSTGRES_DB_PASSWORD = postgres.getPassword();
    process.env.AUTH_ISSUER_URI = 'http://localhost:0/realms/shop';
    process.env.AUTH_JWK_SET_URI =
      'http://localhost:0/realms/shop/protocol/openid-connect/certs';
    process.env.AUTH_AUDIENCE = 'shop-backend';

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(ErpGateway)
      .useValue({
        getProductDetails: () => Promise.resolve({ id: 'BOOK-123', price: 10 }),
        getPromotionDetails: () =>
          Promise.resolve({ promotionActive: false, discount: 1 }),
      })
      .overrideProvider(TaxGateway)
      .useValue({
        getTaxDetails: () =>
          Promise.resolve({ id: 'US', countryName: 'US', taxRate: 0.1 }),
      })
      .overrideProvider(ClockGateway)
      .useValue({
        getCurrentTime: () => Promise.resolve(new Date('2026-03-10T12:00:00Z')),
      })
      .compile();

    app = moduleRef.createNestApplication();
    await app.init();

    const dataSource = moduleRef.get(DataSource);
    orderRepo = dataSource.getRepository(Order);
    couponRepo = dataSource.getRepository(Coupon);
    keyRepo = dataSource.getRepository(IdempotencyKey);
    orderService = moduleRef.get(OrderService);
    couponService = moduleRef.get(CouponService);
  }, 120_000);

  afterAll(async () => {
    if (app) {
      await app.close();
    }
    if (postgres) {
      await postgres.stop();
    }
  }, 60_000);

  beforeEach(async () => {
    await orderRepo.clear();
    await couponRepo.clear();
    await keyRepo.clear();
  });

  function request(couponCode?: string): PlaceOrderRequest {
    const r = new PlaceOrderRequest();
    r.sku = 'BOOK-123';
    r.quantity = 2;
    r.country = 'US';
    r.couponCode = couponCode;
    return r;
  }

  it('concurrent requests with the same key place exactly one order and use the coupon once', async () => {
    await couponRepo.save(
      couponRepo.create({
        code: 'RACE-KEY',
        discountRate: new Decimal('0.1'),
        usageLimit: 5,
        usedCount: 0,
      }),
    );

    const responses = await Promise.all(
      Array.from({ length: 8 }, () =>
        orderService.placeOrder(request('RACE-KEY'), CUSTOMER, 'race-key'),
      ),
    );

    expect(new Set(responses.map((r) => r.orderNumber)).size).toBe(1);
    expect(await orderRepo.count()).toBe(1);
    expect(await keyRepo.count()).toBe(1);
    expect(
      (await couponRepo.findOneByOrFail({ code: 'RACE-KEY' })).usedCount,
    ).toBe(1);
  });

  it('a replay after the first commit returns the stored response, a different body is rejected', async () => {
    const first = await orderService.placeOrder(request(), CUSTOMER, 'key-1');
    const replay = await orderService.placeOrder(request(), CUSTOMER, 'key-1');

    expect(replay.orderNumber).toBe(first.orderNumber);
    await expect(
      orderService.placeOrder(request('OTHER'), CUSTOMER, 'key-1'),
    ).rejects.toThrow(
      'Idempotency-Key has already been used with a different request',
    );
    expect(await orderRepo.count()).toBe(1);
  });

  it('replaces an expired key on its next use', async () => {
    await keyRepo.insert({
      owner: CUSTOMER.subject,
      key: 'old-key',
      requestHash: 'stale',
      responseStatus: 201,
      responseBody: JSON.stringify({ orderNumber: 'ORD-OLD' }),
      createdAt: new Date(Date.now() - 25 * 60 * 60 * 1000),
    });

    const placed = await orderService.placeOrder(
      request(),
      CUSTOMER,
      'old-key',
    );

    expect(placed.orderNumber).not.toBe('ORD-OLD');
    const row = await keyRepo.findOneByOrFail({ key: 'old-key' });
    expect(JSON.parse(row.responseBody)).toEqual({
      orderNumber: placed.orderNumber,
    });
  });

  it('pages orders newest first in the database, stable across pages, with an empty page past the end', async () => {
    const base = Date.parse('2026-01-01T00:00:00Z');
    for (let i = 0; i < 5; i++) {
      await orderRepo.save(
        orderRepo.create({
          orderNumber: `ORD-PAGE-${i}`,
          // Two orders share a timestamp, so the id tie-break decides their order.
          orderTimestamp: new Date(base + Math.min(i, 3) * 60_000),
          country: 'US',
          sku: 'BOOK-123',
          quantity: 1,
          unitPrice: new Decimal('10.00'),
          totalPrice: new Decimal('11.00'),
          status: OrderStatus.PLACED,
        }),
      );
    }

    const page1 = await orderService.browseOrderHistory(
      undefined,
      ADMIN,
      '1',
      '2',
    );
    const page2 = await orderService.browseOrderHistory(
      undefined,
      ADMIN,
      '2',
      '2',
    );
    const page3 = await orderService.browseOrderHistory(
      undefined,
      ADMIN,
      '3',
      '2',
    );
    const past = await orderService.browseOrderHistory(
      undefined,
      ADMIN,
      '4',
      '2',
    );

    expect(
      [...page1.orders, ...page2.orders, ...page3.orders].map(
        (o) => o.orderNumber,
      ),
    ).toEqual([
      'ORD-PAGE-4',
      'ORD-PAGE-3',
      'ORD-PAGE-2',
      'ORD-PAGE-1',
      'ORD-PAGE-0',
    ]);
    expect(page1).toMatchObject({
      page: 1,
      size: 2,
      totalElements: 5,
      totalPages: 3,
    });
    expect(past.orders).toEqual([]);
    expect(past.totalElements).toBe(5);
  });

  it('pages coupons newest first in the database', async () => {
    for (const code of ['C-1', 'C-2', 'C-3']) {
      await couponRepo.save(
        couponRepo.create({
          code,
          discountRate: new Decimal('0.1'),
          usedCount: 0,
        }),
      );
    }

    const page = await couponService.browseCoupons('2', '2');

    expect(page.coupons.map((c) => c.code)).toEqual(['C-1']);
    expect(page).toMatchObject({
      page: 2,
      size: 2,
      totalElements: 3,
      totalPages: 2,
    });
  });
});
