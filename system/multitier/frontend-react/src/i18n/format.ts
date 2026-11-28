// Locale-aware formatting. The currency never changes (the backend has no conversion): only separators,
// symbol position and date order follow the active language. English output is byte-identical to before,
// which the e2e suites rely on.

import { useTranslation } from 'react-i18next';
import { i18n } from './index';
import { localeOf } from './languages';

const CURRENCY = 'USD';

function locale(): string {
  return localeOf(i18n.language);
}

export function formatMoneyAmount(amount: number): string {
  return new Intl.NumberFormat(locale(), {
    style: 'currency',
    currency: CURRENCY,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

/** "0.125" -> "12.50%" (en) / "12,50 %" (fr). */
export function formatPercent(rate: string | number): string {
  return new Intl.NumberFormat(locale(), {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(rate));
}

/** Timestamps are shown in UTC, as before; only the layout follows the language. */
export function formatDateTime(value: string | number | Date): string {
  return new Date(value).toLocaleString(locale(), { timeZone: 'UTC' });
}

/**
 * For components that show formatted values: subscribing to the language makes them re-render on a switch
 * even when they have no other translated text.
 */
export function useFormat() {
  const { i18n: instance } = useTranslation();
  return { language: instance.language, formatMoneyAmount, formatPercent, formatDateTime };
}
