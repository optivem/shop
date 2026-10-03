import { decide, safeReturnTo } from '@/lib/auth/policy';

const customer = { roles: ['CUSTOMER'] };
const admin = { roles: ['ADMIN'] };

describe('decide', () => {
  it.each([
    ['GET', '/health'],
    ['GET', '/login'],
    ['GET', '/login/callback'],
    ['POST', '/logout'],
  ])('%s %s is public', (method, path) => {
    expect(decide(method, path, null)).toBe('allow');
  });

  it.each([
    ['GET', '/'],
    ['GET', '/new-order'],
    ['GET', '/order-history'],
    ['GET', '/api/orders'],
    ['POST', '/api/orders'],
    ['GET', '/api/orders/ORD-1'],
    ['POST', '/api/orders/ORD-1/cancel'],
    ['GET', '/api/unknown-route'],
  ])('%s %s needs authentication but no particular role', (method, path) => {
    expect(decide(method, path, null)).toBe('unauthenticated');
    expect(decide(method, path, customer)).toBe('allow');
    expect(decide(method, path, admin)).toBe('allow');
  });

  it.each([
    ['POST', '/api/orders/ORD-1/deliver'],
    ['GET', '/api/coupons'],
    ['POST', '/api/coupons'],
    ['GET', '/api/admin/recall/ABC'],
    ['GET', '/admin-coupons'],
  ])('%s %s is ADMIN only', (method, path) => {
    expect(decide(method, path, null)).toBe('unauthenticated');
    expect(decide(method, path, customer)).toBe('forbidden');
    expect(decide(method, path, admin)).toBe('allow');
  });
});

describe('safeReturnTo', () => {
  it('keeps same-site paths', () => {
    expect(safeReturnTo('/order-history?x=1')).toBe('/order-history?x=1');
  });

  it.each([null, undefined, '', 'https://evil.example', '//evil.example', String.raw`/\evil.example`])(
    'falls back to / for %s',
    (value) => {
      expect(safeReturnTo(value)).toBe('/');
    }
  );
});
