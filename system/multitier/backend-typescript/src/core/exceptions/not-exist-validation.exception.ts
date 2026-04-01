import { ValidationException } from './validation.exception';

export class NotExistValidationException extends ValidationException {
  constructor(message: string) {
    super(message);
  }
}
