// Expected fields on an order-history row. showsOrder runs at BOTH the UI and gateway levels, so this
// is SEMANTIC: totalPrice is the decimal string the backend returns ('22.00') — the UI driver formats it
// to '$22.00', the gateway compares it as-is. Only the fields a spec names are asserted.
export interface OrderHistoryRowExpectation {
  totalPrice?: string;
  status?: string;
}
