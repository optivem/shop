// quantity is `number | string` because the user types free text: "3.5" and "lala" are
// gestures a real person can make, and the frontend has its own rules about them.
export interface PlaceOrderGesture {
  sku: string;
  quantity: number | string;
  country: string;
  couponCode?: string;
}
