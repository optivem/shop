import { createContext } from 'react';
import type { ApiError } from '../types/error.types';
import type { Result } from '../types/result.types';

export interface NotificationContextType {
  successMessage: string | null;
  error: ApiError | null;
  notificationId: number;
  clearNotification: () => void;
  setSuccess: (message: string) => void;
  setError: (error: ApiError) => void;
  handleResult: <T>(result: Result<T>, onSuccess: (data: T) => void) => void;
}

export const NotificationContext = createContext<NotificationContextType | undefined>(undefined);
