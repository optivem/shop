import { createContext, useContext, useState, useCallback, useMemo, useRef, ReactNode } from 'react';
import type { ApiError } from '../types/error.types';
import type { Result } from '../types/result.types';
import { match } from '../types/result.types';

interface NotificationContextType {
  successMessage: string | null;
  error: ApiError | null;
  notificationId: number;
  clearNotification: () => void;
  setSuccess: (message: string) => void;
  setError: (error: ApiError) => void;
  handleResult: <T>(result: Result<T>, onSuccess: (data: T) => void) => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export function NotificationProvider({ children }: { children: ReactNode }) {
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

export function useNotificationContext() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotificationContext must be used within NotificationProvider');
  }
  return context;
}
