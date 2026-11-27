import { useEffect, useState } from 'react';
import { SLOW_LOADING_HINT } from '../utils/messages';

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
export function LoadingSpinner({ message = 'Loading...', size = 'md' }: Readonly<LoadingSpinnerProps>) {
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
          <span className="visually-hidden">Loading...</span>
        </div>
      </output>
      {message && <p className="mt-3 mb-0 text-body-secondary fw-semibold">{message}</p>}
      {isSlow && (
        <p className="mt-2 mb-0 text-body-secondary small" aria-label="Slow Loading Hint">
          {SLOW_LOADING_HINT}
        </p>
      )}
    </div>
  );
}
