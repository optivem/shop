import { useState, useCallback, useMemo, useRef, type ReactNode } from 'react';
import type { ApiError } from '../types/error.types';
import type { Result } from '../types/result.types';
import { match } from '../types/result.types';
import { NotificationContext } from './notification-context';

export function NotificationProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [notificationId, setNotificationId] = useState<number>(0);
  const notificationCounterRef = useRef<number>(0);

  const getNextNotificationId = useCallback(() => {
    notificationCounterRef.current += 1;
    setNotificationId(notificationCounterRef.current);
    return notificationCounterRef.current;
  }, []);

  const clearNotification = useCallback(() => {
    setSuccessMessage(null);
    setError(null);
  }, []);

  const setSuccess = useCallback((message: string) => {
    setSuccessMessage(message);
    setError(null);
    getNextNotificationId();
  }, [getNextNotificationId]);

  const setErrorMessage = useCallback((errorObj: ApiError) => {
    setError(errorObj);
    setSuccessMessage(null);
    getNextNotificationId();
  }, [getNextNotificationId]);

  const handleResult = useCallback(<T,>(
    result: Result<T>,
    onSuccess: (data: T) => void
  ) => {
    clearNotification();
    match(result, {
      success: onSuccess,
      error: setErrorMessage
    });
  }, [clearNotification, setErrorMessage]);

  const value = useMemo(() => ({
    successMessage,
    error,
    notificationId,
    clearNotification,
    setSuccess,
    setError: setErrorMessage,
    handleResult
  }), [successMessage, error, notificationId, clearNotification, setSuccess, setErrorMessage, handleResult]);

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}
