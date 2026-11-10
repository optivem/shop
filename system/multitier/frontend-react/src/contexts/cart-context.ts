import { createContext } from 'react';

export interface CartLine {
  sku: string;
  quantity: number;
}

export interface CartContextType {
  lines: CartLine[];
  /** Sum of all line quantities: what the header badge shows. */
  totalQuantity: number;
  /** Adds to the line for `sku` (or appends a new line), capped at 99. */
  add: (sku: string, quantity: number) => void;
  setQuantity: (sku: string, quantity: number) => void;
  remove: (sku: string) => void;
  /** Removes several lines at once (checkout removes the lines it placed). */
  removeMany: (skus: string[]) => void;
  clear: () => void;
}

const noop = () => undefined;

/** Without a provider (isolated page tests) the cart is empty and ignores changes. */
export const EMPTY_CART: CartContextType = {
  lines: [],
  totalQuantity: 0,
  add: noop,
  setQuantity: noop,
  remove: noop,
  removeMany: noop,
  clear: noop,
};

export const CartContext = createContext<CartContextType>(EMPTY_CART);
