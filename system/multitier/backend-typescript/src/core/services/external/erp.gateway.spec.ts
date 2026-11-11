import * as http from 'node:http';
import type { AddressInfo } from 'node:net';
import type { ConfigService } from '@nestjs/config';
import { ErpGateway } from './erp.gateway';
import { ErpUnavailableException } from '../../exceptions/erp-unavailable.exception';

// A real HTTP server stands in for the ERP, so the gateway's fetch, timeout, retry and breaker
// all run for real.
type Handler = (res: http.ServerResponse) => void;

describe('ErpGateway resilience', () => {
  let server: http.Server;
  let handler: Handler;
  let requests: number;
  let erpGateway: ErpGateway;

  beforeEach(async () => {
    requests = 0;
    server = http.createServer((_req, res) => {
      requests++;
      handler(res);
    });
    await new Promise<void>((resolve) => server.listen(0, resolve));
    const { port } = server.address() as AddressInfo;
    const config: Record<string, string> = {
      ERP_API_URL: `http://localhost:${port}`,
      EXTERNAL_TIMEOUT_MS: '100',
    };
    const configService = {
      get: (key: string, defaultValue?: string) => config[key] ?? defaultValue,
    } as unknown as ConfigService;
    erpGateway = new ErpGateway(configService);
  });

  afterEach(async () => {
    server.closeAllConnections();
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  const respond =
    (status: number, body: unknown = {}): Handler =>
    (res) => {
      res.writeHead(status, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(body));
    };

  it('returns null for a missing product without retrying', async () => {
    handler = respond(404);
    await expect(erpGateway.getProductDetails('UNKNOWN')).resolves.toBeNull();
    expect(requests).toBe(1);
  });

  it('retries a 5xx and succeeds when the ERP recovers', async () => {
    let calls = 0;
    handler = (res) =>
      ++calls < 3
        ? respond(503)(res)
        : respond(200, { id: 'HP-15', price: 699.99 })(res);
    await expect(erpGateway.getProductDetails('HP-15')).resolves.toMatchObject({
      id: 'HP-15',
      price: 699.99,
    });
    expect(requests).toBe(3);
  });

  it('reports the ERP as unavailable after 3 failed attempts', async () => {
    handler = respond(503);
    await expect(erpGateway.getProductDetails('HP-15')).rejects.toThrow(
      ErpUnavailableException,
    );
    expect(requests).toBe(3);
  });

  it('times out a slow ERP and reports it as unavailable', async () => {
    handler = () => undefined; // never answers
    const started = Date.now();
    await expect(erpGateway.getProductDetails('HP-15')).rejects.toThrow(
      ErpUnavailableException,
    );
    expect(requests).toBe(3);
    expect(Date.now() - started).toBeLessThan(3000);
  });

  it('opens the breaker and then fails fast without calling the ERP', async () => {
    handler = respond(503);
    await expect(erpGateway.getProducts()).rejects.toThrow(
      ErpUnavailableException,
    );
    await expect(erpGateway.getProducts()).rejects.toThrow(
      ErpUnavailableException,
    );
    const requestsWhenOpened = requests;

    await expect(erpGateway.getProducts()).rejects.toThrow(
      ErpUnavailableException,
    );
    expect(requests).toBe(requestsWhenOpened);
  });
});
