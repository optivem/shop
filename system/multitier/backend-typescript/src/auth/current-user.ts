import { createParamDecorator, type ExecutionContext } from '@nestjs/common';
import type { AuthenticatedRequest } from './auth.guard';
import { ADMIN_ROLE } from './route-policy';

/** The authenticated caller: token subject, display username, and whether they hold the ADMIN role. */
export interface CurrentUser {
  subject: string;
  username: string;
  admin: boolean;
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
    };
  },
);
