import request from 'supertest';
import { ComponentHarness } from '../support/component-harness';
import { TEST_ISSUER } from '../support/test-idp';

/**
 * Authorization through the real app: real JWT validation against a real (in-process) JWKS
 * endpoint, real guard, real error filter. Tokens are signed by the test identity provider.
 */
describe('Authorization (component)', () => {
  const harness = new ComponentHarness();

  beforeAll(async () => {
    await harness.start();
  }, 120_000);

  afterAll(async () => {
    await harness.stop();
  }, 60_000);

  const call = (method: 'get' | 'post', path: string, token?: string) => {
    const req = request(harness.httpServer())[method](path);
    return token ? req.set('Authorization', `Bearer ${token}`) : req;
  };

  it('health is public', async () => {
    expect((await call('get', '/health')).status).toBe(200);
  });

  it('orders without a token returns 401 as a problem detail', async () => {
    const response = await call('get', '/api/orders');

    expect(response.status).toBe(401);
    expect(response.headers['content-type']).toContain(
      'application/problem+json',
    );
    expect(response.headers['www-authenticate']).toBe('Bearer');
    expect(response.body).toMatchObject({ status: 401, title: 'Unauthorized' });
  });

  it('orders with an invalid token returns 401', async () => {
    expect((await call('get', '/api/orders', 'garbage')).status).toBe(401);
  });

  it('rejects a token from another issuer', async () => {
    const token = await harness.idp.token(['ADMIN'], {
      issuer: `${TEST_ISSUER}-evil`,
    });
    expect((await call('get', '/api/orders', token)).status).toBe(401);
  });

  it('rejects a token for another audience', async () => {
    const token = await harness.idp.token(['ADMIN'], { audience: 'other-api' });
    expect((await call('get', '/api/orders', token)).status).toBe(401);
  });

  it('rejects an expired token', async () => {
    const token = await harness.idp.token(['ADMIN'], { expiresIn: '-1m' });
    expect((await call('get', '/api/orders', token)).status).toBe(401);
  });

  it('customer can browse orders', async () => {
    const token = await harness.idp.customerToken();
    expect((await call('get', '/api/orders', token)).status).toBe(200);
  });

  it('customer cannot deliver an order', async () => {
    const token = await harness.idp.customerToken();
    const response = await call('post', '/api/orders/ORD-1/deliver', token);

    expect(response.status).toBe(403);
    expect(response.body).toMatchObject({ status: 403, title: 'Forbidden' });
  });

  it('customer cannot publish or list coupons', async () => {
    const token = await harness.idp.customerToken();
    expect((await call('post', '/api/coupons', token)).status).toBe(403);
    expect((await call('get', '/api/coupons', token)).status).toBe(403);
  });

  it('customer cannot use admin endpoints', async () => {
    const token = await harness.idp.customerToken();
    expect(
      (await call('post', '/api/admin/recall/BOOK-123', token)).status,
    ).toBe(403);
  });

  it('a token without any role cannot reach admin endpoints', async () => {
    const token = await harness.idp.token([]);
    expect(
      (await call('post', '/api/admin/recall/BOOK-123', token)).status,
    ).toBe(403);
  });

  it('admin passes authorization on an admin endpoint', async () => {
    // The order does not exist, so the request is authorized and then rejected by the service.
    const response = await call(
      'post',
      '/api/orders/ORD-MISSING/deliver',
      await harness.idp.adminToken(),
    );
    expect(response.status).toBe(404);
  });
});
