/** `whenAskedToSignIn`: a visitor who is asked to sign in signs in as the customer (UI only). */
export const SIGN_IN = 'sign-in';

export interface ViewOrderRequest {
  orderNumber: string;
  /** What a visitor does when the order details ask to sign in: SIGN_IN, or nothing (unset). */
  whenAskedToSignIn?: string | null;
}
