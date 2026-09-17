import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import * as http from 'http';
import { AddressInfo } from 'net';
import {
  PostgreSqlContainer,
  StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import { DataSource, Repository } from 'typeorm';
import { AppModule } from '../../src/app.module';
import { configureApp } from '../../src/configure-app';
import { Order } from '../../src/core/entities/order.entity';
import { Coupon } from '../../src/core/entities/coupon.entity';
import { applyMigrations } from './migrations';

/**
 * A tiny in-process HTTP stub server (the Node analogue of WireMock used by the Java harness).
 * Routes are registered per test/provider-state and matched by `METHOD path`; everything else 404s.
 * Used instead of nock because the gateways call out via global `fetch` (undici), which nock's
 * `http.ClientRequest` patching does not intercept — a real socket the app can fetch from does.
 */
class StubServer {
  private readonly server: http.Server;
  private readonly routes = new Map<
    string,
    { status: number; body?: unknown }
  >();
  url = '';

  constructor() {
    this.server = http.createServer((req, res) => {
      const route = this.routes.get(`${req.method ?? 'GET'} ${req.url ?? ''}`);
      const status = route?.status ?? 404;
      res.writeHead(status, { 'Content-Type': 'application/json' });
      res.end(route?.body === undefined ? '' : JSON.stringify(route.body));
    });
  }

  async start(): Promise<void> {
    await new Promise<void>((resolve) =>
      this.server.listen(0, '127.0.0.1', resolve),
    );
    const port = (this.server.address() as AddressInfo).port;
    this.url = `http://127.0.0.1:${port}`;
  }

  stub(method: string, path: string, status: number, body?: unknown): void {
    this.routes.set(`${method} ${path}`, { status, body });
  }

  reset(): void {
    this.routes.clear();
  }

  async stop(): Promise<void> {
    await new Promise<void>((resolve, reject) =>
      this.server.close((err) => (err ? reject(err) : resolve())),
    );
  }
}

/**
 * In-process component-test harness: boots the Nest app on a random port (real HTTP over a real
 * socket), backs it with a Testcontainers-managed Postgres (real dialect/numeric semantics) and
 * stubs the ERP / Tax / Clock external HTTP systems with in-process stub servers on 127.0.0.1.
 * No docker compose, no deployment. Mirrors the Java `BaseComponentTest` and is shared by both
 * the Component suite and the Pact provider-verification test.
 */
export class ComponentHarness {
  app!: INestApplication;
  port!: number;
  dataSource!: DataSource;
  orderRepo!: Repository<Order>;
  couponRepo!: Repository<Coupon>;
  private postgres!: StartedPostgreSqlContainer;
  private readonly erp = new StubServer();
  private readonly tax = new StubServer();
  private readonly clock = new StubServer();

  async start(): Promise<void> {
    this.postgres = await new PostgreSqlContainer('postgres:16-alpine')
      .withDatabase('app')
      .withUsername('app')
      .withPassword('app')
      .start();

    await Promise.all([this.erp.start(), this.tax.start(), this.clock.start()]);

    await applyMigrations(this.postgres);

    // The real AppModule; only configuration is replaced, pointing it at the container and the stubs.
    const config: Record<string, string> = {
      POSTGRES_DB_HOST: this.postgres.getHost(),
      POSTGRES_DB_PORT: String(this.postgres.getPort()),
      POSTGRES_DB_NAME: this.postgres.getDatabase(),
      POSTGRES_DB_USER: this.postgres.getUsername(),
      POSTGRES_DB_PASSWORD: this.postgres.getPassword(),
      NODE_ENV: 'production', // turns off TypeORM query logging
      ERP_API_URL: this.erp.url,
      TAX_API_URL: this.tax.url,
      CLOCK_API_URL: this.clock.url,
      EXTERNAL_SYSTEM_MODE: 'stub',
    };
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(ConfigService)
      .useValue({
        get: (key: string, defaultValue?: unknown) =>
          config[key] ?? defaultValue,
      })
      .compile();

    this.app = moduleRef.createNestApplication();
    configureApp(this.app);
    await this.app.listen(0);

    const server = this.app.getHttpServer() as http.Server;
    this.port = (server.address() as AddressInfo).port;

    this.dataSource = moduleRef.get(DataSource);
    this.orderRepo = this.dataSource.getRepository(Order);
    this.couponRepo = this.dataSource.getRepository(Coupon);
  }

  async stop(): Promise<void> {
    await this.app.close();
    await Promise.all([this.erp.stop(), this.tax.stop(), this.clock.stop()]);
    await this.postgres.stop();
  }

  /** Clears external stubs and empties the DB — call before each test/provider state. */
  async resetState(): Promise<void> {
    this.erp.reset();
    this.tax.reset();
    this.clock.reset();
    await this.orderRepo.clear();
    await this.couponRepo.clear();
  }

  baseUrl(): string {
    return `http://127.0.0.1:${this.port}`;
  }

  httpServer(): http.Server {
    return this.app.getHttpServer() as http.Server;
  }

  // --- External-system stub helpers (shared by component tests and provider states) ---

  stubClock(isoInstant: string): void {
    this.clock.stub('GET', '/api/time', 200, { time: isoInstant });
  }

  stubProduct(sku: string, price: number): void {
    this.erp.stub('GET', `/api/products/${sku}`, 200, { id: sku, price });
  }

  stubProductMissing(sku: string): void {
    this.erp.stub('GET', `/api/products/${sku}`, 404);
  }

  stubPromotion(active: boolean, discount: number): void {
    this.erp.stub('GET', '/api/promotion', 200, {
      promotionActive: active,
      discount,
    });
  }

  stubTax(country: string, rate: number): void {
    this.tax.stub('GET', `/api/countries/${country}`, 200, {
      id: country,
      countryName: country,
      taxRate: rate,
    });
  }

  stubTaxMissing(country: string): void {
    this.tax.stub('GET', `/api/countries/${country}`, 404);
  }
}
