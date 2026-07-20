// Expected fields on an order-history row. showsOrder runs at BOTH the UI and gateway levels, so this
// is SEMANTIC: totalPrice is the number the backend returns — the UI driver formats it to '$22.00',
// the gateway compares it numerically. Only the fields a spec names are asserted.
export interface OrderHistoryRowExpectation {
  totalPrice?: number;
  status?: string;
}
