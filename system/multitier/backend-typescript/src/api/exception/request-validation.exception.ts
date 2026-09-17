export interface FieldError {
  field: string;
  message: string;
  code: string | null;
  rejectedValue: null;
}

export class RequestValidationException extends Error {
  constructor(readonly fieldErrors: FieldError[]) {
    super('The request contains one or more validation errors');
  }
}
