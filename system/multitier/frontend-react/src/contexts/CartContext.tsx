import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { clampQuantity } from '../utils/quantity';
import { CartContext, type CartLine } from './cart-context';
import { loadCart, saveCart } from './cart-storage';

/** Keeps the visitor's cart, persisted in localStorage so it survives page loads and sign-in. */
export function CartProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [lines, setLines] = useState<CartLine[]>(() => loadCart());

  useEffect(() => {
    saveCart(lines);
  }, [lines]);

  const add = useCallback((sku: string, quantity: number) => {
    setLines((current) => {
      const existing = current.find((line) => line.sku === sku);
      if (!existing) return [...current, { sku, quantity: clampQuantity(quantity) }];
      return current.map((line) => (line.sku === sku ? { sku, quantity: clampQuantity(line.quantity + quantity) } : line));
    });
  }, []);

  const setQuantity = useCallback((sku: string, quantity: number) => {
    setLines((current) => current.map((line) => (line.sku === sku ? { sku, quantity: clampQuantity(quantity) } : line)));
  }, []);

  const removeMany = useCallback((skus: string[]) => {
    setLines((current) => current.filter((line) => !skus.includes(line.sku)));
  }, []);

  const remove = useCallback((sku: string) => removeMany([sku]), [removeMany]);
  const clear = useCallback(() => setLines([]), []);

  const value = useMemo(
    () => ({
      lines,
      totalQuantity: lines.reduce((sum, line) => sum + line.quantity, 0),
      add,
      setQuantity,
      remove,
      removeMany,
      clear,
    }),
    [lines, add, setQuantity, remove, removeMany, clear],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
