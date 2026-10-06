import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import type { Request } from 'express';
import { JwtVerifier, type AuthenticatedUser } from './jwt-verifier';
import { ADMIN_ROLE, CUSTOMER_ROLE, accessFor } from './route-policy';

export type AuthenticatedRequest = Request & { user?: AuthenticatedUser };

/** Registered as a global guard: every route is checked against the route policy table. */
@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private readonly verifier: JwtVerifier) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    const access = accessFor(
      request.method,
      request.originalUrl ?? request.url,
    );
    if (access === 'public') return true;

    const token = bearerToken(request);
    if (!token) throw new UnauthorizedException();

    let user: AuthenticatedUser;
    try {
      user = await this.verifier.verify(token);
    } catch {
      throw new UnauthorizedException();
    }
    request.user = user;

    if (access === 'admin' && !user.roles.includes(ADMIN_ROLE)) {
      throw new ForbiddenException();
    }
    if (access === 'customer' && !user.roles.includes(CUSTOMER_ROLE)) {
      throw new ForbiddenException();
    }
    return true;
  }
}

function bearerToken(request: Request): string | null {
  const header = request.headers.authorization;
  if (!header) return null;
  const [scheme, token, ...rest] = header.split(' ');
  if (scheme?.toLowerCase() !== 'bearer' || !token || rest.length > 0)
    return null;
  return token;
}
