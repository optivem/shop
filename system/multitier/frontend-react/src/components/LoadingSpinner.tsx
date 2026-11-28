import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

/** How long a load may run before the spinner explains the demo site's cold start. */
const SLOW_LOADING_DELAY_MS = 3000;

interface LoadingSpinnerProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
}

/**
 * Loading spinner component for displaying loading states
 * @param message - Optional loading message to display
 * @param size - Size variant (sm, md, lg)
 */
export function LoadingSpinner({ message, size = 'md' }: Readonly<LoadingSpinnerProps>) {
  const { t } = useTranslation();
  const shownMessage = message ?? t('states.loading');
  const sizeClass = size === 'sm' ? 'spinner-border-sm' : '';
  const [isSlow, setIsSlow] = useState(false);

  // The demo deployment scales to zero, so a first request can wait on a cold start.
  useEffect(() => {
    const timer = setTimeout(() => setIsSlow(true), SLOW_LOADING_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="text-center py-5">
      <output>
        <div className={`spinner-border text-primary ${sizeClass}`}>
          <span className="visually-hidden">{t('states.loading')}</span>
        </div>
      </output>
      {shownMessage && <p className="mt-3 mb-0 text-body-secondary fw-semibold">{shownMessage}</p>}
      {isSlow && (
        <p className="mt-2 mb-0 text-body-secondary small" aria-label={t('states.slowLoadingHintLabel')}>
          {t('messages.slowLoadingHint')}
        </p>
      )}
    </div>
  );
}
