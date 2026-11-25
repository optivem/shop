/** A sign-in account (a customer's or an admin's) created for one scenario: what the drivers sign in with. */
export interface UserAccount {
  readonly username: string;
  readonly password: string;
}
