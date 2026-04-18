export interface SystemError {
  message: string;
  fieldErrors: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}
