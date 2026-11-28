// The languages the storefront can show. English is the default and the only one the e2e suites run in.

export const LANGUAGES = [
  { code: 'en', locale: 'en-US', label: 'EN', nativeName: 'English' },
  { code: 'sr', locale: 'sr-Cyrl-RS', label: 'СР', nativeName: 'Српски' },
  { code: 'de', locale: 'de-DE', label: 'DE', nativeName: 'Deutsch' },
  { code: 'fr', locale: 'fr-FR', label: 'FR', nativeName: 'Français' },
] as const;

export type Language = (typeof LANGUAGES)[number]['code'];

export const DEFAULT_LANGUAGE: Language = 'en';

export const LANGUAGE_STORAGE_KEY = 'shop.language';

export function isLanguage(value: unknown): value is Language {
  return LANGUAGES.some((language) => language.code === value);
}

/** The BCP 47 locale used for number and date formatting in a language. */
export function localeOf(language: string): string {
  return LANGUAGES.find((l) => l.code === language)?.locale ?? 'en-US';
}
