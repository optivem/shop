import type { ThenStep } from './then-step.js';

export interface ThenFailure extends ThenStep<ThenFailure> {
  errorMessage(expectedMessage: string): ThenFailure;
  fieldErrorMessage(expectedField: string, expectedMessage: string): ThenFailure;
}
