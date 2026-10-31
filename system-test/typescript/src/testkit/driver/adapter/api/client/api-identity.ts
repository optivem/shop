export const ApiIdentity = {
  /** Call without any Authorization header. */
  ANONYMOUS: 'ANONYMOUS',
  CUSTOMER: 'CUSTOMER',
  ADMIN: 'ADMIN',
} as const;

export type ApiIdentityValue = (typeof ApiIdentity)[keyof typeof ApiIdentity];
