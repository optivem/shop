export const OrderStatus = {
  PLACED: 'PLACED',
  CANCELLED: 'CANCELLED',
  DELIVERED: 'DELIVERED',
} as const;

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];
