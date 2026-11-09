/** GET/PUT /api/account/profile: the saved profile, with the email from the sign-in account. */
export class ViewProfileResponse {
  firstName!: string | null;
  lastName!: string | null;
  email!: string | null;
  street!: string | null;
  postalCode!: string | null;
  city!: string | null;
  country!: string | null;
}
