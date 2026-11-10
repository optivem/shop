// Money display: one currency for every shipping country, with thousands separators and 2 decimals.

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** "1299.99" -> "$1,299.99". Works on cents so a line total never shows binary-float noise. */
export function formatMoney(amount: string | number): string {
  return usd.format(toCents(amount) / 100);
}

/** Decimal amount -> integer cents. */
export function toCents(amount: string | number): number {
  return Math.round(Number(amount) * 100);
}

/** unit price x quantity, in cents. */
export function lineTotalCents(unitPrice: string, quantity: number): number {
  return toCents(unitPrice) * quantity;
}

export function formatCents(cents: number): string {
  return usd.format(cents / 100);
}
