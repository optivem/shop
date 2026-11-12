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
  /** No shipping address is saved: the street, postal code, city and country are empty. */
  hasNoSavedAddress(): this;
  /** The email is shown but cannot be edited: it comes from the sign-in account (UI only). */
  hasEmailThatCannotBeChanged(): this;
  /** The country select offers exactly these countries, in this order (UI only). */
  hasCountryChoices(...expected: string[]): this;
  /** The profile's note on how sales tax is charged (UI only). */
  hasTaxNotice(expected: string): this;
  /** The customer was told this after saving (UI only). */
  hasMessage(expected: string): this;
}
