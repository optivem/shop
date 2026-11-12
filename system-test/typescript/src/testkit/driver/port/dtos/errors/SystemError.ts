export interface SystemError {
  message: string;
  fieldErrors: FieldError[];
  /** HTTP status of the failed response, when the failure came from an HTTP call. */
  status?: number;
  /** The Retry-After header of the failed response, when it came from an HTTP call that sent one. */
  retryAfter?: string;
  /** The Content-Type of the failed response, when the failure came from an HTTP call. */
  contentType?: string;
}

export interface FieldError {
  field: string;
  message: string;
}
