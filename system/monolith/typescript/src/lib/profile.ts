import type { CustomerProfileRow } from './db';
import type { Caller } from './auth/caller';

/** GET/PUT /api/account/profile body: the saved profile, with the email from the sign-in account. */
export interface ProfileResponse {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
}

/** Without a saved profile the name comes from the token and the address is empty. */
export function toProfileResponse(profile: CustomerProfileRow | null, caller: Caller): ProfileResponse {
  if (!profile) {
    return {
      firstName: caller.firstName,
      lastName: caller.lastName,
      email: caller.email,
      street: null,
      postalCode: null,
      city: null,
      country: null,
    };
  }
  return {
    firstName: profile.first_name,
    lastName: profile.last_name,
    email: caller.email,
    street: profile.street,
    postalCode: profile.postal_code,
    city: profile.city,
    country: profile.country,
  };
}
