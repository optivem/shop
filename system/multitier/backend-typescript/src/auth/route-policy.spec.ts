import { accessFor, normalisePath } from './route-policy';

describe('route policy', () => {
  it.each([
    ['GET', '/health', 'public'],
    ['OPTIONS', '/api/orders', 'public'],
    ['GET', '/api/orders', 'authenticated'],
    ['POST', '/api/orders', 'authenticated'],
    ['GET', '/api/orders/ORD-1', 'authenticated'],
    ['POST', '/api/orders/ORD-1/cancel', 'authenticated'],
    ['POST', '/api/orders/ORD-1/deliver', 'admin'],
    ['GET', '/api/coupons', 'admin'],
    ['POST', '/api/coupons', 'admin'],
    ['POST', '/api/admin/recall/BOOK-123', 'admin'],
    ['GET', '/api/admin/anything', 'admin'],
    ['DELETE', '/api/admin', 'admin'],
  ])('%s %s is %s', (method, path, expected) => {
    expect(accessFor(method, path)).toBe(expected);
  });

  it('requires authentication for routes that are not listed', () => {
    expect(accessFor('GET', '/api/unknown')).toBe('authenticated');
    expect(accessFor('GET', '/')).toBe('authenticated');
    expect(accessFor('POST', '/health')).toBe('authenticated');
  });

  it.each([
    '/API/ADMIN/recall/BOOK-123',
    '/api/admin/recall/BOOK-123/',
    '//api//admin/recall/BOOK-123',
    '/api/admin/recall/BOOK-123?x=1',
    '/api/%61dmin/recall/BOOK-123',
  ])('cannot bypass an admin rule with %s', (path) => {
    expect(accessFor('POST', path)).toBe('admin');
  });

  it('cannot bypass the deliver rule by changing case or adding a slash', () => {
    expect(accessFor('POST', '/API/Orders/ORD-1/DELIVER/')).toBe('admin');
  });

  it('normalises paths', () => {
    expect(normalisePath('/Api/Orders/?a=b')).toBe('/api/orders');
    expect(normalisePath('/')).toBe('/');
  });
});
