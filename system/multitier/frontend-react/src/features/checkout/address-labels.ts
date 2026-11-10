import type { ShippingAddress } from '../../types/api.types';

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
