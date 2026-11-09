import { OrderStatus } from '../../../common/domain/OrderStatus.js';

export const DEFAULTS = {
  SKU: 'DEFAULT-SKU',
  QUANTITY: '1',
  COUNTRY: 'US',
  UNIT_PRICE: '20.00',
  ORDER_NUMBER: 'DEFAULT-ORDER',
  ORDER_STATUS: OrderStatus.PLACED,
  CLOCK_TIME: '2025-12-24T10:00:00Z',
  WEEKDAY_TIME: '2026-01-15T10:30:00Z',
  WEEKEND_TIME: '2026-01-17T10:30:00Z',
  PROMOTION_ACTIVE: false,
  PROMOTION_DISCOUNT: '1.00',
  TAX_RATE: '0.07',
  COUPON_CODE: 'DEFAULT-COUPON',
  DISCOUNT_RATE: '0.10',
  VALID_FROM: '2024-01-01T00:00:00Z',
  VALID_TO: '2024-12-31T23:59:59Z',
  USAGE_LIMIT: '1000',
  EMPTY: null,
  FIRST_NAME: 'Ana',
  LAST_NAME: 'Novak',
  STREET: 'Main St 1',
  POSTAL_CODE: '10115',
  CITY: 'Berlin',
  /** The profile only accepts US, DE or GB, so its country is a literal, not an alias. */
  PROFILE_COUNTRY: 'DE',
};
