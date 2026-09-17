import { GenericContainer, StartedTestContainer, Wait } from 'testcontainers';
import { ConfigService } from '@nestjs/config';
import { ErpGateway } from './erp.gateway';

describe('ErpGateway [integration]', () => {
  let container: StartedTestContainer;
  let erpGateway: ErpGateway;
  let wireMockUrl: string;

  beforeAll(async () => {
    container = await new GenericContainer('wiremock/wiremock:3.9.0')
      .withExposedPorts(8080)
      .withWaitStrategy(Wait.forHttp('/__admin/mappings', 8080))
      .start();
    wireMockUrl = `http://${container.getHost()}:${container.getMappedPort(8080)}`;
  }, 60_000);

  afterAll(async () => {
    await container.stop();
  }, 30_000);

  beforeEach(async () => {
    await fetch(`${wireMockUrl}/__admin/mappings`, { method: 'DELETE' });
    const configService = {
      get: jest.fn().mockReturnValue(wireMockUrl),
    } as unknown as ConfigService;
    erpGateway = new ErpGateway(configService);
  });

  async function stubGetJson(path: string, status: number, body: object) {
    await fetch(`${wireMockUrl}/__admin/mappings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        request: { method: 'GET', url: path },
        response: { status, jsonBody: body },
      }),
    });
  }

  async function stubGetStatus(path: string, status: number) {
    await fetch(`${wireMockUrl}/__admin/mappings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        request: { method: 'GET', url: path },
        response: { status },
      }),
    });
  }

  it('getProductDetails returns details when found', async () => {
    await stubGetJson('/api/products/BOOK-123', 200, {
      id: 'BOOK-123',
      price: 10.0,
    });

    const result = await erpGateway.getProductDetails('BOOK-123');

    expect(result).not.toBeNull();
    expect(result!.id).toBe('BOOK-123');
    expect(result!.price).toBe(10.0);
  });

  it('getProductDetails returns null when not found', async () => {
    await stubGetStatus('/api/products/UNKNOWN', 404);

    const result = await erpGateway.getProductDetails('UNKNOWN');

    expect(result).toBeNull();
  });

  it('getProductDetails throws on server error', async () => {
    await stubGetStatus('/api/products/BAD-SKU', 500);

    await expect(erpGateway.getProductDetails('BAD-SKU')).rejects.toThrow(
      '500',
    );
  });

  it('getPromotionDetails returns promotion', async () => {
    await stubGetJson('/api/promotion', 200, {
      promotionActive: true,
      discount: 0.15,
    });

    const result = await erpGateway.getPromotionDetails();

    expect(result.promotionActive).toBe(true);
    expect(result.discount).toBe(0.15);
  });

  it('getPromotionDetails throws on server error', async () => {
    await stubGetStatus('/api/promotion', 503);

    await expect(erpGateway.getPromotionDetails()).rejects.toThrow('503');
  });
});
