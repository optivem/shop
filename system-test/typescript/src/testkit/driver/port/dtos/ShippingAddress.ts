/** The address an order ships to: sent with PlaceOrderRequest and returned with ViewOrderResponse. */
export interface ShippingAddress {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}
