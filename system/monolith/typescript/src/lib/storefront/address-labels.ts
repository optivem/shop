import type { ShippingAddress } from './types';

export type AddressLabels = Record<keyof ShippingAddress, string>;

/** Selectors of the checkout's "ship to a different address" form (storefront-ux §4.4). */
export const SHIPPING_LABELS: AddressLabels = {
  firstName: 'Shipping First Name',
  lastName: 'Shipping Last Name',
  street: 'Shipping Street',
  postalCode: 'Shipping Postal Code',
  city: 'Shipping City',
  country: 'Shipping Country',
};

/** Selectors of the profile form (storefront-ux §8). */
export const PROFILE_LABELS: AddressLabels = {
  firstName: 'First Name',
  lastName: 'Last Name',
  street: 'Street',
  postalCode: 'Postal Code',
  city: 'City',
  country: 'Profile Country',
};
