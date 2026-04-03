import type { ApiError } from '../types/error.types';

interface NotificationProps {
  successMessage?: string | null;
  error?: ApiError | null;
  notificationId?: number;
}

/**
 * Notification component for displaying success or error messages
 * @param successMessage - Success message string to display
 * @param error - ApiError object with message and optional field-level errors
 */
export function Notification({ successMessage, error, notificationId }: NotificationProps) {
  // Handle error messages
  if (error) {
    return (
      <div className="notification error" role="alert" data-notification-id={notificationId}>
        <div className="error-message">{error.message}</div>
        {error.fieldErrors?.map((fieldError) => (
          <div key={fieldError} className="field-error">{fieldError}</div>
        ))}
      </div>
    );
  }

  // Handle success messages
  if (successMessage) {
    return (
      <div className="notification success" role="alert" data-notification-id={notificationId}>
        {successMessage}
      </div>
    );
  }

  return null;
}
