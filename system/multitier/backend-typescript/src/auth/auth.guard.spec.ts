import {
  ForbiddenException,
  UnauthorizedException,
  type ExecutionContext,
} from '@nestjs/common';
import { AuthGuard } from './auth.guard';
import type { JwtVerifier } from './jwt-verifier';

function contextFor(method: string, url: string, authorization?: string) {
  const request = { method, url, headers: { authorization } };
  const context = {
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext;
  return { context, request };
}

describe('AuthGuard', () => {
  const verify = jest.fn();
  const guard = new AuthGuard({ verify } as unknown as JwtVerifier);

  beforeEach(() => {
    verify.mockReset();
    verify.mockImplementation((token: string) => {
      if (token === 'admin-token') {
        return Promise.resolve({ subject: 'a', roles: ['ADMIN'] });
      }
      if (token === 'customer-token') {
        return Promise.resolve({ subject: 'c', roles: ['CUSTOMER'] });
      }
      return Promise.reject(new Error('invalid'));
    });
  });

  it('lets public routes through without a token', async () => {
    const { context } = contextFor('GET', '/health');
    await expect(guard.canActivate(context)).resolves.toBe(true);
    expect(verify).not.toHaveBeenCalled();
  });

  it('rejects a missing token with 401', async () => {
    const { context } = contextFor('GET', '/api/orders');
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('rejects a non-bearer scheme with 401', async () => {
    const { context } = contextFor('GET', '/api/orders', 'Basic abc');
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('rejects an invalid token with 401', async () => {
    const { context } = contextFor('GET', '/api/orders', 'Bearer garbage');
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('rejects an invalid token with 401 even on an admin route', async () => {
    const { context } = contextFor(
      'POST',
      '/api/admin/recall/X',
      'Bearer garbage',
    );
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('lets a customer use authenticated routes and attaches the user', async () => {
    const { context, request } = contextFor(
      'GET',
      '/api/orders',
      'Bearer customer-token',
    );
    await expect(guard.canActivate(context)).resolves.toBe(true);
    expect((request as { user?: unknown }).user).toEqual({
      subject: 'c',
      roles: ['CUSTOMER'],
    });
  });

  it.each([
    ['POST', '/api/orders/ORD-1/deliver'],
    ['POST', '/api/coupons'],
    ['GET', '/api/coupons'],
    ['POST', '/api/admin/recall/BOOK-123'],
  ])('rejects a customer on %s %s with 403', async (method, url) => {
    const { context } = contextFor(method, url, 'Bearer customer-token');
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      ForbiddenException,
    );
  });

  it('lets an admin use admin routes', async () => {
    const { context } = contextFor(
      'POST',
      '/api/orders/ORD-1/deliver',
      'Bearer admin-token',
    );
    await expect(guard.canActivate(context)).resolves.toBe(true);
  });

  it('requires authentication for unlisted routes', async () => {
    const { context } = contextFor('GET', '/api/unknown');
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });
});
