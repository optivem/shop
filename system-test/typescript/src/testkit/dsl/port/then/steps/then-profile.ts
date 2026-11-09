export interface ThenProfileResultStage {
  shouldSucceed(): ThenProfileSuccess;
  shouldFail(): ThenProfileFailure;
}

export interface ThenProfileSuccess extends PromiseLike<void> {
  and(): this;
  profile(): ThenProfile;
}

export interface ThenProfileFailure extends PromiseLike<void> {
  errorMessage(expectedMessage: string): this;
  fieldErrorMessage(expectedField: string, expectedMessage: string): this;
  and(): this;
  /** The profile as saved, read back as the acting user. */
  profile(): ThenProfile;
}

export interface ThenProfile extends PromiseLike<void> {
  and(): this;
  hasFirstName(expected: string): this;
  hasLastName(expected: string): this;
  hasEmail(expected: string): this;
  hasStreet(expected: string): this;
  hasPostalCode(expected: string): this;
  hasCity(expected: string): this;
  hasCountry(expected: string): this;
  doesNotHaveStreet(unexpected: string): this;
}
