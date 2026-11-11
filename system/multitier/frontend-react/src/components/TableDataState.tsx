import { LoadingSpinner } from './LoadingSpinner';
import { ErrorMessage } from './ErrorMessage';

interface TableDataStateProps {
  isLoading: boolean;
  error?: string | null;
  isEmpty?: boolean;
  colSpan: number;
  loadingMessage?: string;
  emptyMessage?: string;
  onRetry?: () => void;
}

/**
 * Standardized component for handling data loading states within tables
 * Renders proper table rows with correct colspan for loading, error, and empty states
 */
export function TableDataState({
  isLoading,
  error,
  isEmpty = false,
  colSpan,
  loadingMessage = 'Loading...',
  emptyMessage = 'No data available',
  onRetry,
}: Readonly<TableDataStateProps>) {
  if (isLoading) {
    return (
      <tr>
        <td colSpan={colSpan} className="text-center">
          <LoadingSpinner message={loadingMessage} />
        </td>
      </tr>
    );
  }

  if (error) {
    return (
      <tr>
        <td colSpan={colSpan} className="text-center">
          <ErrorMessage message={error} onRetry={onRetry} />
        </td>
      </tr>
    );
  }

  if (isEmpty) {
    return (
      <tr>
        <td colSpan={colSpan} className="text-center py-5 text-body-secondary fw-semibold">
          {emptyMessage}
        </td>
      </tr>
    );
  }

  return null;
}
