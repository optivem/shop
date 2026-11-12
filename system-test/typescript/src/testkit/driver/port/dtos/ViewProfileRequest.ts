/** `whenAskedToSignIn`: a visitor who is asked to sign in signs in as the customer (UI only). */
export const SIGN_IN = 'sign-in';

/** Views the acting user's own profile; the identity is the only input, apart from a visitor's sign-in (UI only). */
export interface ViewProfileRequest {
  /** What a visitor does when the profile asks to sign in: SIGN_IN, or nothing (unset). */
  whenAskedToSignIn?: string | null;
}
