import { INestApplication } from '@nestjs/common';
import Decimal from 'decimal.js';
import { Test } from '@nestjs/testing';
import { DataSource, Repository } from 'typeorm';
import {
  PostgreSqlContainer,
  StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import { AppModule } from '../../app.module';
import { Order } from '../entities/order.entity';
import { OrderStatus } from '../entities/order-status.enum';
import { applyMigrations } from '../../../test/support/migrations';

describe('OrderRepository [integration]', () => {
  let app: INestApplication;
  let postgres: StartedPostgreSqlContainer;
  let orderRepo: Repository<Order>;

  beforeAll(async () => {
    postgres = await new PostgreSqlContainer('postgres:16-alpine')
      .withDatabase('app')
      .withUsername('app')
      .withPassword('app')
      .start();

    await applyMigrations(postgres);

    // Point the real AppModule factory at the container — no TypeORM override.
    // The existing forRootAsync useFactory reads these env vars verbatim.
    process.env.POSTGRES_DB_HOST = postgres.getHost();
    process.env.POSTGRES_DB_PORT = String(postgres.getPort());
    process.env.POSTGRES_DB_NAME = postgres.getDatabase();
    process.env.POSTGRES_DB_USER = postgres.getUsername();
    process.env.POSTGRES_DB_PASSWORD = postgres.getPassword();

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();

    orderRepo = moduleRef.get(DataSource).getRepository(Order);
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
  });

  it('saves an order and reads it back', async () => {
    const saved = await orderRepo.save(
      orderRepo.create({
        orderNumber: 'ORD-001',
        orderTimestamp: new Date('2026-01-01T00:00:00Z'),
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
      }),
    );

    const found = await orderRepo.findOne({
      where: { orderNumber: 'ORD-001' },
    });

    expect(found).not.toBeNull();
    expect(found!.id).toBe(saved.id);
    expect(found!.sku).toBe('BOOK-123');
    // numeric columns round-trip exactly: node-postgres returns a string, the column transformer
    // reads it into a Decimal.
    expect(found!.totalPrice).toBeInstanceOf(Decimal);
    expect(found!.totalPrice.toFixed(2)).toBe('22.00');
    expect(found!.status).toBe(OrderStatus.PLACED);
  });
});
