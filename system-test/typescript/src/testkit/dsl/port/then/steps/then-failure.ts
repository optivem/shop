export interface ThenFailure extends PromiseLike<void> {
  errorMessage(expectedMessage: string): this;
  fieldErrorMessage(expectedField: string, expectedMessage: string): this;
  and(): this;
}
