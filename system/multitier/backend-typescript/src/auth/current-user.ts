import { createParamDecorator, type ExecutionContext } from '@nestjs/common';
import type { AuthenticatedRequest } from './auth.guard';
import { ADMIN_ROLE } from './route-policy';

/**
 * The authenticated caller: token subject, display username, whether they hold the ADMIN role, and the
 * account's email and name claims (null when the token does not carry them).
 */
export interface CurrentUser {
  subject: string;
  username: string;
  admin: boolean;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
}

/** Injects the caller the AuthGuard authenticated; every non-public route has one. */
export const Caller = createParamDecorator(
  (_data: unknown, context: ExecutionContext): CurrentUser => {
    const user = context.switchToHttp().getRequest<AuthenticatedRequest>().user;
    if (!user) throw new Error('No authenticated user on the request');
    return {
      subject: user.subject,
      username: user.username,
      admin: user.roles.includes(ADMIN_ROLE),
      email: user.email,
      firstName: user.givenName,
      lastName: user.familyName,
    };
  },
);
