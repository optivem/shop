import { en } from './locales/en';
import { sr } from './locales/sr';
import { de } from './locales/de';
import { fr } from './locales/fr';

export const NAMESPACES = ['common', 'catalog', 'checkout', 'account'] as const;

export const resources = { en, sr, de, fr };
