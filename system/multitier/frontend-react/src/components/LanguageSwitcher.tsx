import { Globe } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { setLanguage } from '../i18n';
import { LANGUAGES, isLanguage } from '../i18n/languages';

/** The language picker in the header's utility strip. A native select: keyboard and screen-reader friendly as is. */
export function LanguageSwitcher() {
  const { t, i18n } = useTranslation();
  return (
    <label className="store-language">
      <Globe size={14} aria-hidden="true" />
      <span className="visually-hidden">{t('header.language')}</span>
      <select
        name="language"
        className="store-language-select"
        data-testid="language-switcher"
        value={i18n.language}
        onChange={(e) => {
          if (isLanguage(e.target.value)) setLanguage(e.target.value);
        }}
      >
        {LANGUAGES.map((language) => (
          <option key={language.code} value={language.code} lang={language.code}>
            {language.label} · {language.nativeName}
          </option>
        ))}
      </select>
    </label>
  );
}
