import type { ReactNode } from 'react';
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
  if (isLoading) {
    return <LoadingSpinner message={loadingMessage} />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={onRetry} />;
  }

  if (isEmpty) {
    return (
      <div className="text-center py-5 text-muted">
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return <>{children}</>;
}
