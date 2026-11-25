/** Who the API client calls as, short of a given user (MyShopApiClient.asUser). */
export const ApiIdentity = {
  /** Call without any Authorization header. */
  ANONYMOUS: 'ANONYMOUS',
  /** The shared test customer customer1. */
  CUSTOMER: 'CUSTOMER',
} as const;

export type ApiIdentityValue = (typeof ApiIdentity)[keyof typeof ApiIdentity];
