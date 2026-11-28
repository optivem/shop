// Global test setup — registers jest-dom matchers. Testing Library unmounts rendered trees after
// each test by itself, because vite.config.ts enables vitest globals (afterEach is global).
import '@testing-library/jest-dom/vitest';

// Every test starts in English (the default), whatever an earlier test switched to.
import { beforeEach } from 'vitest';
import { i18n } from '../i18n';
import { LANGUAGE_STORAGE_KEY } from '../i18n/languages';

beforeEach(async () => {
  try {
    localStorage.removeItem(LANGUAGE_STORAGE_KEY);
  } catch {
    // no storage in this environment
  }
  await i18n.changeLanguage('en');
});
