// Common notification functions shared across all pages

import type { ApiError } from './types/error.types';
import { isProblemDetail, type Guard } from './types/api.guards';
import type { Result } from './types/result.types';

export function showNotification(
  message: string,
  isError = false,
  containerElementId = 'notifications'
): void {
  const notificationsDiv = document.getElementById(containerElementId);
  if (!notificationsDiv) {
    console.error(`Notification container not found: ${containerElementId}`);
    return;
  }

  notificationsDiv.innerHTML = '';

  const notif = document.createElement('div');
  notif.setAttribute('role', 'alert');
  notif.className = `notification ${isError ? 'error' : 'success'}`;

  if (isError) {
    // For error messages, use structured format matching API errors
    const errorMessageDiv = document.createElement('div');
    errorMessageDiv.className = 'error-message';
    errorMessageDiv.textContent = message;
    notif.appendChild(errorMessageDiv);
  } else {
    // For success messages, use simple text
    notif.textContent = message;
  }

  notificationsDiv.appendChild(notif);
}

/**
 * Displays an error from a failed API Result.
 * Shows general error message first, then field-level errors below if present.
 * Each field error is displayed as "fieldName: error message"
 *
 * @param error The ApiError from a failed Result
 */
export function showApiError(error: ApiError): void {
  const notificationsDiv = document.getElementById('notifications');
  if (!notificationsDiv) {
    console.error('Notification container not found: notifications');
    return;
  }

  notificationsDiv.innerHTML = '';

  const notif = document.createElement('div');
  notif.setAttribute('role', 'alert');
  notif.className = 'notification error';

  // Add general error message
  const generalMessage = document.createElement('div');
  generalMessage.className = 'error-message';
  generalMessage.textContent = error.message;
  notif.appendChild(generalMessage);

  // Add field-level errors if present
  if (error.fieldErrors && error.fieldErrors.length > 0) {
    error.fieldErrors.forEach(fieldError => {
      const fieldErrorDiv = document.createElement('div');
      fieldErrorDiv.className = 'field-error';
      fieldErrorDiv.textContent = fieldError;
      notif.appendChild(fieldErrorDiv);
    });
  }

  notificationsDiv.appendChild(notif);
}

/**
 * Displays a success notification message.
 * Convenience wrapper for showNotification with isError=false.
 *
 * @param message The success message to display
 */
export function showSuccessNotification(message: string): void {
  showNotification(message, false);
}

/**
 * Handles a Result by executing a success callback or showing an error.
 * Encapsulates the common if-else pattern for Result handling.
 *
 * @param result The Result from a service call
 * @param onSuccess Callback to execute on success, receives the data
 */
export function handleResult<T>(
  result: Result<T>,
  onSuccess: (data: T) => void
): void {
  if (result.success) {
    onSuccess(result.data);
  } else {
    showApiError(result.error);
  }
}

/**
 * Performs a fetch request and returns a Result.
 * Handles HTTP errors, network errors, and checks the JSON body against the expected type.
 *
 * @param url The URL to fetch
 * @param isExpected Runtime guard for the expected response body
 * @param options Fetch options (method, headers, body, etc.)
 * @returns Result containing the checked JSON data or error
 */
export async function fetchJson<T>(url: string, isExpected: Guard<T>, options?: RequestInit): Promise<Result<T>> {
  return fetchResult(url, options, async (response) => {
    const data: unknown = await response.json();
    if (!isExpected(data)) {
      return {
        success: false,
        error: { message: `Unexpected response from server. (Status: ${response.status})`, status: response.status }
      };
    }
    return { success: true, data };
  });
}

/**
 * Performs a fetch request whose success response has no body (e.g. 204 No Content).
 */
export async function fetchNoContent(url: string, options?: RequestInit): Promise<Result<void>> {
  return fetchResult(url, options, () => Promise.resolve({ success: true, data: undefined }));
}

async function fetchResult<T>(
  url: string,
  options: RequestInit | undefined,
  onSuccess: (response: Response) => Promise<Result<T>>
): Promise<Result<T>> {
  try {
    const response = await fetch(url, options);
    if (response.ok) {
      return await onSuccess(response);
    }
    const error = await extractApiError(response);
    return { success: false, error };
  } catch (e: unknown) {
    return {
      success: false,
      error: {
        message: `Network error: ${e instanceof Error ? e.message : String(e)}`,
        status: 0
      }
    };
  }
}

async function safeParseJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch (e) {
    console.error('Error parsing JSON response:', e);
    return null;
  }
}

/**
 * Extracts error information from API response without showing notifications.
 * This is a pure function with no UI side effects.
 *
 * @param response The fetch Response object
 * @returns ApiError object with message and optional field errors
 */
export async function extractApiError(response: Response): Promise<ApiError> {
  const body = await safeParseJson(response);
  const errorData = isProblemDetail(body) ? body : null;

  let message = '';
  let fieldErrors: string[] | undefined = undefined;

  if (errorData?.detail) {
    message = errorData.detail;

    if (errorData.errors && Array.isArray(errorData.errors) && errorData.errors.length > 0) {
      fieldErrors = errorData.errors.map(e => `${e.field}: ${e.message}`);
    }
  } else {
    message = `An unexpected error occurred. (Status: ${response.status})`;
  }

  return {
    message,
    fieldErrors,
    status: response.status
  };
}
