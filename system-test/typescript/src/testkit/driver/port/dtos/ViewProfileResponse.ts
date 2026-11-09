/** The signed-in user's profile; the email comes from the sign-in account. Unsaved fields are null. */
export interface ViewProfileResponse {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
}
