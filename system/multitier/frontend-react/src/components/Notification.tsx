import { CircleAlert, CircleCheck } from 'lucide-react';
import type { ApiError } from '../types/error.types';

interface NotificationProps {
  successMessage?: string | null;
  error?: ApiError | null;
  notificationId?: number;
}

/**
 * Notification component for displaying success or error messages.
 * The UI drivers read the success text as the element's whole textContent, so the only extra child is a
 * text-free, aria-hidden SVG icon.
 * @param successMessage - Success message string to display
 * @param error - ApiError object with message and optional field-level errors
 */
export function Notification({ successMessage, error, notificationId }: Readonly<NotificationProps>) {
  // Handle error messages
  if (error) {
    return (
      <div className="notification error" role="alert" data-notification-id={notificationId}>
        <CircleAlert className="notification-icon" size={20} aria-hidden="true" />
        <div className="notification-body">
          <div className="error-message">{error.message}</div>
          {error.fieldErrors?.map((fieldError) => (
            <div key={fieldError} className="field-error">{fieldError}</div>
          ))}
        </div>
      </div>
    );
  }

  // Handle success messages
  if (successMessage) {
    return (
      <div className="notification success" role="alert" data-notification-id={notificationId}>
        <CircleCheck className="notification-icon" size={20} aria-hidden="true" />
        {successMessage}
      </div>
    );
  }

  return null;
}
