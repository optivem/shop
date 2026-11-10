// Shipping address rules shared by checkout and the profile page (UC-009 BR-3, UC-010 BR-2/BR-3).

import type { ShippingAddress } from './types';

export const SHIPPING_COUNTRIES = ['US', 'DE', 'GB'] as const;
export const COUNTRY_NOT_SUPPORTED = 'Country must be US, DE or GB';

/**
 * Estimated sales tax per shipping country, for the checkout review only (the same rates as the tax system's
 * test data). Each order is taxed by the backend when it is placed.
 */
export const ESTIMATED_TAX_RATES: Partial<Record<string, number>> = { US: 0, DE: 0.19, GB: 0.2 };

export const EMPTY_ADDRESS: ShippingAddress = { firstName: '', lastName: '', street: '', postalCode: '', city: '', country: '' };

const REQUIRED_FIELDS: { field: keyof ShippingAddress; label: string }[] = [
  { field: 'firstName', label: 'First name' },
  { field: 'lastName', label: 'Last name' },
  { field: 'street', label: 'Street' },
  { field: 'postalCode', label: 'Postal code' },
  { field: 'city', label: 'City' },
  { field: 'country', label: 'Country' },
];

export type AddressErrors = Partial<Record<keyof ShippingAddress, string>>;

/** The profile as GET /api/account/profile returns it; every field may be empty. */
export interface ProfileData {
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  street?: string | null;
  postalCode?: string | null;
  city?: string | null;
  country?: string | null;
}

/** Every field is required (spaces only count as empty) and the country must be a shipping country. */
export function validateAddress(address: ShippingAddress): AddressErrors {
  const errors: AddressErrors = {};
  for (const { field, label } of REQUIRED_FIELDS) {
    if (address[field].trim() === '') errors[field] = `${label} is required`;
  }
  if (!errors.country && !(SHIPPING_COUNTRIES as readonly string[]).includes(address.country.trim())) {
    errors.country = COUNTRY_NOT_SUPPORTED;
  }
  return errors;
}

export function trimAddress(address: ShippingAddress): ShippingAddress {
  return {
    firstName: address.firstName.trim(),
    lastName: address.lastName.trim(),
    street: address.street.trim(),
    postalCode: address.postalCode.trim(),
    city: address.city.trim(),
    country: address.country.trim(),
  };
}

export function addressOf(profile: ProfileData): ShippingAddress {
  return {
    firstName: profile.firstName ?? '',
    lastName: profile.lastName ?? '',
    street: profile.street ?? '',
    postalCode: profile.postalCode ?? '',
    city: profile.city ?? '',
    country: profile.country ?? '',
  };
}

/** The profile's saved address, or undefined when the customer has not saved a complete one yet. */
export function savedAddressOf(profile: ProfileData): ShippingAddress | undefined {
  const address = addressOf(profile);
  return Object.keys(validateAddress(address)).length === 0 ? address : undefined;
}

/** "Ana Novak, Main St 1, 10115 Berlin, DE" */
export function formatAddress(address: ShippingAddress): string {
  return `${address.firstName} ${address.lastName}, ${address.street}, ${address.postalCode} ${address.city}, ${address.country}`;
}

export function formatTaxRate(rate: number): string {
  return `${Math.round(rate * 100)}%`;
}
