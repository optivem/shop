// Shipping address rules shared by checkout and the profile page (UC-009 BR-3, UC-010 BR-2/BR-3).

import { i18n } from '../../i18n';
import { localeOf } from '../../i18n/languages';
import type { ProfileResponse, ShippingAddress } from '../../types/api.types';

export const SHIPPING_COUNTRIES = ['US', 'DE', 'GB'] as const;

/**
 * Estimated sales tax per shipping country, for the checkout review only (the same rates as the tax system's
 * test data). Each order is taxed by the backend when it is placed.
 */
export const ESTIMATED_TAX_RATES: Record<string, number> = { US: 0, DE: 0.19, GB: 0.2 };

export const EMPTY_ADDRESS: ShippingAddress = { firstName: '', lastName: '', street: '', postalCode: '', city: '', country: '' };

const REQUIRED_FIELDS: (keyof ShippingAddress)[] = ['firstName', 'lastName', 'street', 'postalCode', 'city', 'country'];

export type AddressErrors = Partial<Record<keyof ShippingAddress, string>>;

/** Every field is required (spaces only count as empty) and the country must be a shipping country. */
export function validateAddress(address: ShippingAddress): AddressErrors {
  const errors: AddressErrors = {};
  for (const field of REQUIRED_FIELDS) {
    if (address[field].trim() === '') errors[field] = i18n.t(`checkout:address.${field}Required`);
  }
  if (!errors.country && !(SHIPPING_COUNTRIES as readonly string[]).includes(address.country.trim())) {
    errors.country = i18n.t('checkout:address.countryNotSupported');
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

/** The profile's saved address, or undefined when the customer has not saved a complete one yet. */
export function savedAddressOf(profile: ProfileResponse): ShippingAddress | undefined {
  const address: ShippingAddress = {
    firstName: profile.firstName ?? '',
    lastName: profile.lastName ?? '',
    street: profile.street ?? '',
    postalCode: profile.postalCode ?? '',
    city: profile.city ?? '',
    country: profile.country ?? '',
  };
  return Object.keys(validateAddress(address)).length === 0 ? address : undefined;
}

/** "Ana Novak, Main St 1, 10115 Berlin, DE" */
export function formatAddress(address: ShippingAddress): string {
  return `${address.firstName} ${address.lastName}, ${address.street}, ${address.postalCode} ${address.city}, ${address.country}`;
}

export function formatTaxRate(rate: number): string {
  return new Intl.NumberFormat(localeOf(i18n.language), { style: 'percent', maximumFractionDigits: 0 }).format(Math.round(rate * 100) / 100);
}
