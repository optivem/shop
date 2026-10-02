export interface SystemError {
  message: string;
  fieldErrors: FieldError[];
  /** HTTP status of the failed response, when the failure came from an HTTP call. */
  status?: number;
}

export interface FieldError {
  field: string;
  message: string;
}
