import { i18n } from '../../i18n';
import type { ShippingAddress } from '../../types/api.types';

export type AddressLabels = Record<keyof ShippingAddress, string>;

/** Selectors of the checkout's "ship to a different address" form (storefront-ux §4.4). */
export const SHIPPING_LABELS: AddressLabels = {
  // Getters, so the text is looked up at use time and follows the active language.
  get firstName() {
    return i18n.t('checkout:address.shippingFirstName');
  },
  get lastName() {
    return i18n.t('checkout:address.shippingLastName');
  },
  get street() {
    return i18n.t('checkout:address.shippingStreet');
  },
  get postalCode() {
    return i18n.t('checkout:address.shippingPostalCode');
  },
  get city() {
    return i18n.t('checkout:address.shippingCity');
  },
  get country() {
    return i18n.t('checkout:address.shippingCountry');
  },
};
