/** A visitor's sign-up (UC-013): the new sign-in account and the shipping country chosen on the registration form. */
export interface SignUpRequest {
  username: string;
  password: string;
  email: string;
  firstName: string;
  lastName: string;
  country: string;
}
