export const ORDER_STATUSES = ['PLACED', 'CANCELLED', 'DELIVERED'] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];
