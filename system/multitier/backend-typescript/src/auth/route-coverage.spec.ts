import { RequestMethod } from '@nestjs/common';
import { METHOD_METADATA, PATH_METADATA } from '@nestjs/common/constants';
import { AdminController } from '../api/controller/admin.controller';
import { CouponController } from '../api/controller/coupon.controller';
import { HealthController } from '../api/controller/health.controller';
import { OrderController } from '../api/controller/order.controller';
import { AppController } from '../app.controller';
import { accessFor, type Access } from './route-policy';

// Every controller route must appear here with the access it is meant to have. Adding a route
// without adding it here fails this test, so a new endpoint gets a deliberate access decision
// instead of silently inheriting the default.
const EXPECTED: Record<string, Access> = {
  'GET /': 'authenticated',
  'GET /health': 'public',
  'GET /api/orders': 'authenticated',
  'POST /api/orders': 'authenticated',
  'GET /api/orders/:orderNumber': 'authenticated',
  'POST /api/orders/:orderNumber/cancel': 'authenticated',
  'POST /api/orders/:orderNumber/deliver': 'admin',
  'GET /api/coupons': 'admin',
  'POST /api/coupons': 'admin',
  'POST /api/admin/recall/:sku': 'admin',
};

type Ctor = new (...args: never[]) => object;

function routesOf(controller: Ctor): string[] {
  const base = Reflect.getMetadata(PATH_METADATA, controller) as string;
  const proto = controller.prototype as Record<string, unknown>;
  return Object.getOwnPropertyNames(proto)
    .filter((name) => name !== 'constructor')
    .flatMap((name) => {
      const handler = proto[name] as object;
      const method = Reflect.getMetadata(METHOD_METADATA, handler) as
        | RequestMethod
        | undefined;
      if (method === undefined) return [];
      const sub = Reflect.getMetadata(PATH_METADATA, handler) as string;
      const path = `/${[base, sub].filter((p) => p && p !== '/').join('/')}`;
      return [`${RequestMethod[method]} ${path}`];
    });
}

describe('route policy covers every controller route', () => {
  const discovered = [
    AppController,
    HealthController,
    OrderController,
    CouponController,
    AdminController,
  ].flatMap((c) => routesOf(c as unknown as Ctor));

  it('has an expected access for each discovered route and nothing stale', () => {
    expect([...discovered].sort()).toEqual(Object.keys(EXPECTED).sort());
  });

  it.each(Object.entries(EXPECTED))('%s is %s', (route, access) => {
    const [method = '', pattern = ''] = route.split(' ');
    const concrete = pattern.replace(/:\w+/g, 'X');
    expect(accessFor(method, concrete)).toBe(access);
  });
});
