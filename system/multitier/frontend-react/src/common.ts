// Shared fetch helpers that turn HTTP responses into typed Results.

import { isProblemDetail, type Guard } from './types/api.guards';
import type { Result } from './types/result.types';
import type { ApiError } from './types/error.types';

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
