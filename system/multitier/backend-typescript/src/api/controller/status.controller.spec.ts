import type { INestApplication } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Test } from '@nestjs/testing';
import * as http from 'http';
import type { AddressInfo } from 'net';
import request from 'supertest';
import { DataSource } from 'typeorm';
import { StatusService } from '../../core/services/status.service';
import { StatusController } from './status.controller';

// Real local HTTP servers, not nock: the probe uses global fetch, which nock cannot intercept.
function startServer(handler: http.RequestListener): Promise<http.Server> {
  const server = http.createServer(handler);
  return new Promise((resolve) =>
    server.listen(0, '127.0.0.1', () => resolve(server)),
  );
}

const urlOf = (server: http.Server) =>
  `http://127.0.0.1:${(server.address() as AddressInfo).port}`;

describe('StatusController', () => {
  let app: INestApplication;
  let healthStatus = 200;
  let healthCalls: string[] = [];
  let external: http.Server;
  let dbUp = true;
  let config: Record<string, string>;

  beforeAll(async () => {
    external = await startServer((req, res) => {
      healthCalls.push(`${req.method} ${req.url}`);
      res.writeHead(healthStatus).end();
    });
  });

  afterAll(async () => {
    await new Promise((resolve) => external.close(resolve));
  });

  beforeEach(async () => {
    healthStatus = 200;
    healthCalls = [];
    dbUp = true;
    config = {
      ERP_API_URL: `${urlOf(external)}/erp`,
      TAX_API_URL: `${urlOf(external)}/tax`,
      CLOCK_API_URL: `${urlOf(external)}/clock`,
      EXTERNAL_SYSTEM_MODE: 'real',
    };
    const moduleRef = await Test.createTestingModule({
      controllers: [StatusController],
      providers: [
        StatusService,
        {
          provide: ConfigService,
          useValue: {
            get: (key: string, fallback?: unknown) => config[key] ?? fallback,
          },
        },
        {
          provide: DataSource,
          useValue: {
            query: () =>
              dbUp ? Promise.resolve([]) : Promise.reject(new Error('down')),
          },
        },
      ],
    }).compile();
    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  const get = (system: string) =>
    request(app.getHttpServer() as http.Server).get(`/api/status/${system}`);

  it('backend is UP when the database answers', async () => {
    const response = await get('backend');
    expect(response.status).toBe(200);
    expect(response.body).toEqual({ status: 'UP' });
  });

  it('backend is DOWN when the database fails', async () => {
    dbUp = false;
    const response = await get('backend');
    expect(response.status).toBe(503);
    expect(response.body).toEqual({ status: 'DOWN' });
  });

  it.each(['erp', 'tax'])(
    '%s is UP when its /health answers 200',
    async (s) => {
      const response = await get(s);
      expect(response.status).toBe(200);
      expect(response.body).toEqual({ status: 'UP' });
      expect(healthCalls).toEqual([`GET /${s}/health`]);
    },
  );

  it.each(['erp', 'tax'])(
    '%s is DOWN when its /health answers 503',
    async (s) => {
      healthStatus = 503;
      const response = await get(s);
      expect(response.status).toBe(503);
      expect(response.body).toEqual({ status: 'DOWN' });
    },
  );

  it('erp is DOWN when the connection fails', async () => {
    config.ERP_API_URL = 'http://127.0.0.1:1/erp';
    const response = await get('erp');
    expect(response.status).toBe(503);
    expect(response.body).toEqual({ status: 'DOWN' });
  });

  it('clock is UP in real mode without calling anything', async () => {
    const response = await get('clock');
    expect(response.status).toBe(200);
    expect(response.body).toEqual({ status: 'UP' });
    expect(healthCalls).toEqual([]);
  });

  it('clock probes its /health in stub mode', async () => {
    config.EXTERNAL_SYSTEM_MODE = 'stub';
    expect((await get('clock')).status).toBe(200);
    expect(healthCalls).toEqual(['GET /clock/health']);

    healthStatus = 503;
    expect((await get('clock')).status).toBe(503);
  });

  it('an unknown system is 404', async () => {
    expect((await get('mail')).status).toBe(404);
  });
});
