import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { LoadingSpinner } from './LoadingSpinner';
import { ErrorMessage } from './ErrorMessage';

interface DataStateProps {
  isLoading: boolean;
  error?: string | null;
  isEmpty?: boolean;
  loadingMessage?: string;
  emptyMessage?: string;
  onRetry?: () => void;
  children: ReactNode;
}

/**
 * Standardized component for handling data loading states
 * Manages loading, error, empty, and success states in a consistent way
 */
export function DataState({
  isLoading,
  error,
  isEmpty = false,
  loadingMessage = 'Loading...',
  emptyMessage = 'No data available',
  onRetry,
  children,
}: Readonly<DataStateProps>) {
  const { t } = useTranslation();
  if (isLoading) {
    return <LoadingSpinner message={loadingMessage ?? t('states.loading')} />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={onRetry} />;
  }

  if (isEmpty) {
    return (
      <div className="market-empty text-center py-5 px-3">
        <p className="mb-0">{emptyMessage ?? t('states.noData')}</p>
      </div>
    );
  }

  return <>{children}</>;
}
