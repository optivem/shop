import { CircleAlert } from 'lucide-react';

interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
}

/**
 * Error message component with optional retry functionality.
 * Drivers read `.alert-danger` and strip the "Error:" prefix, so the icon stays text-free and aria-hidden.
 * @param message - Error message to display
 * @param onRetry - Optional callback function to retry the failed operation
 */
export function ErrorMessage({ message, onRetry }: Readonly<ErrorMessageProps>) {
  return (
    <div className="alert alert-danger d-flex flex-wrap justify-content-between align-items-center gap-2" role="alert">
      <div className="d-flex align-items-start gap-2">
        <CircleAlert className="flex-shrink-0 mt-1" size={18} aria-hidden="true" />
        <div>
          <strong>Error:</strong> {message}
        </div>
      </div>
      {onRetry && (
        <button type="button" className="btn btn-outline-danger" onClick={onRetry}>
          Try Again
        </button>
      )}
    </div>
  );
}
