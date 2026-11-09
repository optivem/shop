import { toProfileResponse } from '../lib/profile';
import type { Caller } from '../lib/auth/caller';

const caller: Caller = {
  subject: 'sub-customer',
  username: 'customer1',
  admin: false,
  email: 'customer1@example.com',
  firstName: 'Token',
  lastName: 'Name',
};

describe('toProfileResponse', () => {
  it('defaults the name from the token and leaves the address empty when nothing is saved', () => {
    expect(toProfileResponse(null, caller)).toEqual({
      firstName: 'Token',
      lastName: 'Name',
      email: 'customer1@example.com',
      street: null,
      postalCode: null,
      city: null,
      country: null,
    });
  });

  it('shows the saved profile with the email from the token', () => {
    const saved = {
      owner: 'sub-customer',
      first_name: 'Ana',
      last_name: 'Novak',
      street: 'Main St 1',
      postal_code: '10115',
      city: 'Berlin',
      country: 'DE',
      updated_at: new Date(),
    };
    expect(toProfileResponse(saved, caller)).toEqual({
      firstName: 'Ana',
      lastName: 'Novak',
      email: 'customer1@example.com',
      street: 'Main St 1',
      postalCode: '10115',
      city: 'Berlin',
      country: 'DE',
    });
  });
});
