import { FieldError } from './errors';

export function validatePlaceOrderRequest(body: Record<string, unknown>): FieldError[] {
  const errors: FieldError[] = [];

  const sku = body.sku;
  if (sku === undefined || sku === null || (typeof sku === 'string' && sku.trim() === '')) {
    errors.push({ field: 'sku', message: 'SKU must not be empty' });
  }

  const quantity = body.quantity;
  if (quantity === undefined || quantity === null) {
    errors.push({ field: 'quantity', message: 'Quantity must not be empty' });
  } else if (typeof quantity === 'string' || typeof quantity === 'boolean' || !Number.isInteger(quantity as number)) {
    errors.push({ field: 'quantity', message: 'Quantity must be an integer', code: 'TYPE_MISMATCH' });
  } else if ((quantity as number) <= 0) {
    errors.push({ field: 'quantity', message: 'Quantity must be positive' });
  }

  return errors;
}
