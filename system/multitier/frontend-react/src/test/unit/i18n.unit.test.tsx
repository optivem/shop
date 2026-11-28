// The language switcher, the translation files' key parity, and locale-aware formatting.
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '../../components/LanguageSwitcher';
import { i18n } from '../../i18n';
import { LANGUAGE_STORAGE_KEY } from '../../i18n/languages';
import { resources } from '../../i18n/resources';
import { formatDateTime, formatPercent } from '../../i18n/format';
import { formatMoney } from '../../utils/money';

interface Tree { [key: string]: string | Tree }

function leaves(tree: Tree, prefix = ''): [string, string][] {
  return Object.entries(tree).flatMap(([key, value]) =>
    typeof value === 'string' ? [[`${prefix}${key}`, value] as [string, string]] : leaves(value, `${prefix}${key}.`),
  );
}

function Probe() {
  const { t } = useTranslation();
  return <p data-testid="probe">{t('header.cart')}</p>;
}

describe('translations', () => {
  const english = leaves(resources.en);

  it.each(['sr', 'de', 'fr'] as const)('%s has exactly the English keys, none of them empty', (language) => {
    const translated = leaves(resources[language]);
    expect(translated.map(([key]) => key).sort()).toEqual(english.map(([key]) => key).sort());
    expect(translated.filter(([, text]) => text.trim() === '')).toEqual([]);
  });

  it('Serbian is written in Cyrillic', () => {
    const latinOnly = leaves(resources.sr as unknown as Tree).filter(
      ([, text]) => /[A-Za-z]/.test(text.replace(/\{\{[^}]*\}\}/g, '')) && !/[Ѐ-ӿ]/.test(text),
    );
    expect(latinOnly).toEqual([]);
  });
});

describe('LanguageSwitcher', () => {
  it('starts in English', () => {
    render(<><LanguageSwitcher /><Probe /></>);
    expect(screen.getByTestId('probe')).toHaveTextContent('Cart');
    expect(screen.getByTestId('language-switcher')).toHaveValue('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('switches the UI, remembers the choice and sets the page language', async () => {
    render(<><LanguageSwitcher /><Probe /></>);
    await userEvent.selectOptions(screen.getByTestId('language-switcher'), 'sr');
    expect(screen.getByTestId('probe')).toHaveTextContent('Корпа');
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('sr');
    expect(document.documentElement.lang).toBe('sr');

    await userEvent.selectOptions(screen.getByTestId('language-switcher'), 'de');
    expect(screen.getByTestId('probe')).toHaveTextContent('Warenkorb');
  });
});

describe('formatting', () => {
  const normalize = (text: string) => text.replace(/\s/g, ' ');

  it('English output is unchanged', () => {
    expect(formatMoney('1299.99')).toBe('$1,299.99');
    expect(formatPercent('0.125')).toBe('12.50%');
    expect(formatDateTime('2026-11-28T10:05:00Z')).toBe('11/28/2026, 10:05:00 AM');
  });

  it('follows the active language but keeps the currency', async () => {
    await i18n.changeLanguage('de');
    expect(normalize(formatMoney('1299.99'))).toBe('1.299,99 $');
    await i18n.changeLanguage('fr');
    expect(normalize(formatMoney('1299.99'))).toBe('1 299,99 $US');
    expect(normalize(formatPercent('0.125'))).toBe('12,50 %');
    await i18n.changeLanguage('sr');
    expect(normalize(formatMoney('1299.99'))).toMatch(/^1\.299,99 (US)?\$$/);
    expect(formatDateTime('2026-11-28T10:05:00Z')).toMatch(/^28\. ?11\. ?2026\./);
  });
});
