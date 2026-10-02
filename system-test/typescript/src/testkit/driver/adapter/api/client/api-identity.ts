export const ApiIdentity = {
  /** Admin for admin-only operations, customer otherwise. */
  DEFAULT: 'DEFAULT',
  /** Call without any Authorization header. */
  ANONYMOUS: 'ANONYMOUS',
  CUSTOMER: 'CUSTOMER',
  ADMIN: 'ADMIN',
} as const;

export type ApiIdentityValue = (typeof ApiIdentity)[keyof typeof ApiIdentity];
