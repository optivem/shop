import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { CartProvider } from '../../contexts/CartContext';
import { useCart } from '../../contexts/useCart';
import { CART_STORAGE_KEY, loadCart, saveCart } from '../../contexts/cart-storage';

const wrapper = ({ children }: { children: ReactNode }) => <CartProvider>{children}</CartProvider>;

describe('cart (UC-008)', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('adds a product, and raises the quantity of a product already in the cart', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => result.current.add('LG-27UL500', 1));
    act(() => result.current.add('HP-15', 2));
    act(() => result.current.add('LG-27UL500', 1));
    expect(result.current.lines).toEqual([
      { sku: 'LG-27UL500', quantity: 2 },
      { sku: 'HP-15', quantity: 2 },
    ]);
    expect(result.current.totalQuantity).toBe(4);
  });

  it.each([
    [97, 2, 99],
    [98, 5, 99],
    [99, 1, 99],
  ])('caps a line at 99 when adding (%i + %i)', (before, added, after) => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => result.current.add('HP-15', before));
    act(() => result.current.add('HP-15', added));
    expect(result.current.lines).toEqual([{ sku: 'HP-15', quantity: after }]);
  });

  it('changes a quantity, removes lines and clears', () => {
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => {
      result.current.add('HP-15', 2);
      result.current.add('DELL-XPS', 1);
      result.current.add('LOGI-MX3S', 1);
    });
    act(() => result.current.setQuantity('HP-15', 3));
    act(() => result.current.remove('DELL-XPS'));
    expect(result.current.lines).toEqual([
      { sku: 'HP-15', quantity: 3 },
      { sku: 'LOGI-MX3S', quantity: 1 },
    ]);
    act(() => result.current.removeMany(['HP-15']));
    expect(result.current.lines).toEqual([{ sku: 'LOGI-MX3S', quantity: 1 }]);
    act(() => result.current.clear());
    expect(result.current.totalQuantity).toBe(0);
  });

  it('keeps the cart in localStorage across visits', () => {
    const first = renderHook(() => useCart(), { wrapper });
    act(() => first.result.current.add('HP-15', 2));
    expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? '[]')).toEqual([{ sku: 'HP-15', quantity: 2 }]);
    first.unmount();

    const second = renderHook(() => useCart(), { wrapper });
    expect(second.result.current.lines).toEqual([{ sku: 'HP-15', quantity: 2 }]);
  });

  it('ignores malformed stored data', () => {
    localStorage.setItem(CART_STORAGE_KEY, '{not json');
    expect(loadCart()).toEqual([]);
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 500 }, { sku: 3 }, { sku: 'HP-15', quantity: 1 }]));
    expect(loadCart()).toEqual([{ sku: 'HP-15', quantity: 99 }]);
  });

  it('falls back to memory when storage throws', () => {
    const broken = {
      getItem: () => {
        throw new Error('denied');
      },
      setItem: () => {
        throw new Error('quota');
      },
    } as unknown as Storage;
    expect(loadCart(broken)).toEqual([]);
    expect(() => saveCart([{ sku: 'HP-15', quantity: 1 }], broken)).not.toThrow();

    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota');
    });
    const { result } = renderHook(() => useCart(), { wrapper });
    act(() => result.current.add('HP-15', 1));
    expect(result.current.lines).toEqual([{ sku: 'HP-15', quantity: 1 }]);
  });
});
