// Quantity rules shared by the product page and the cart (UC-007 BR-2, UC-008 BR-3).

export const MIN_QUANTITY = 1;
export const MAX_QUANTITY = 99;
export const QUANTITY_ERROR = 'Quantity must be a whole number from 1 to 99.';

/** The quantity typed by the visitor, or undefined when it is not a whole number from 1 to 99. */
export function parseQuantity(value: string): number | undefined {
  const trimmed = value.trim();
  if (!/^\d+$/.test(trimmed)) return undefined;
  const quantity = Number(trimmed);
  return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY ? quantity : undefined;
}

export function clampQuantity(quantity: number): number {
  return Math.min(MAX_QUANTITY, Math.max(MIN_QUANTITY, quantity));
}
