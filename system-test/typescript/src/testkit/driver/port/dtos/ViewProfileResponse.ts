/** The signed-in user's profile; the email comes from the sign-in account. Unsaved fields are null. */
export interface ViewProfileResponse {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
  /** UI only: whether the email field can be edited (unset on the API, which has no email input). */
  emailChangeable?: boolean | null;
  /** UI only: the countries the country select offers, as codes, without its empty "Select a country" choice. */
  countryChoices?: string[] | null;
  /** UI only: the note on how sales tax is charged. */
  taxNotice?: string | null;
  /** UI only: the notification shown after saving, such as "Your profile has been saved.". */
  message?: string | null;
}
