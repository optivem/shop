import { useContext } from 'react';
import { CartContext } from './cart-context';

export function useCart() {
  return useContext(CartContext);
}
