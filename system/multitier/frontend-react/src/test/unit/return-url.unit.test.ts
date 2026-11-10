import { describe, expect, it } from 'vitest';
import { returnUrlFromState, safeReturnUrl } from '../../auth/return-url';

describe('sign-in return URL', () => {
  it('follows paths inside the app only', () => {
    expect(safeReturnUrl('/checkout')).toBe('/checkout');
    expect(safeReturnUrl('/?category=Laptops')).toBe('/?category=Laptops');
    expect(safeReturnUrl('https://evil.example')).toBe('/');
    expect(safeReturnUrl('//evil.example')).toBe('/');
    expect(safeReturnUrl(undefined)).toBe('/');
  });

  it('reads the return URL from the login state', () => {
    expect(returnUrlFromState({ returnUrl: '/account' })).toBe('/account');
    expect(returnUrlFromState(undefined)).toBeUndefined();
    expect(returnUrlFromState({ returnUrl: 42 })).toBeUndefined();
  });
});
