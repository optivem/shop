"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { usePathname } from "next/navigation";
import { parseJson, productsResponseSchema } from "@/lib/api-types";
import { saveCartCookie } from "@/lib/storefront/cart-cookie";
import { clampQuantity } from "@/lib/storefront/quantity";
import type { CartLine, Product } from "@/lib/storefront/types";

// The storefront state every page shares: who is signed in, the catalog, the cart and the notification banner.
// The server renders the first view from the session cookie, the catalog and the cart cookie; the browser keeps
// them from there.

/** The signed-in user as the server read it from the session cookie; null for an anonymous visitor. */
export interface SessionUser {
  username: string;
  isAdmin: boolean;
  /** Placing orders (checkout, Quick order) needs the CUSTOMER role. */
  canPlaceOrder: boolean;
}

const SessionContext = createContext<SessionUser | null>(null);

export const useSession = () => useContext(SessionContext);

export interface CatalogState {
  products: Product[];
  /** The ERP could not be reached when the catalog was last read. */
  unavailable: boolean;
  isLoading: boolean;
  /** Reads the catalog again (the cart re-reads prices every time it is shown). */
  reload: () => void;
}

const CatalogContext = createContext<CatalogState>({ products: [], unavailable: false, isLoading: false, reload: () => undefined });

export const useCatalog = () => useContext(CatalogContext);

export interface CartState {
  lines: CartLine[];
  /** Sum of all line quantities: what the header badge shows. */
  totalQuantity: number;
  /** Adds to the line for `sku` (or appends a new line), capped at 99. */
  add: (sku: string, quantity: number) => void;
  setQuantity: (sku: string, quantity: number) => void;
  remove: (sku: string) => void;
  /** Removes several lines at once (checkout removes the lines it placed). */
  removeMany: (skus: string[]) => void;
}

const noop = () => undefined;

const CartContext = createContext<CartState>({ lines: [], totalQuantity: 0, add: noop, setQuantity: noop, remove: noop, removeMany: noop });

export const useCart = () => useContext(CartContext);

export interface NotificationError {
  message: string;
  fieldErrors?: string[];
}

export interface NotificationState {
  successMessage: string | null;
  error: NotificationError | null;
  notificationId: number;
  setSuccess: (message: string) => void;
  setError: (error: NotificationError) => void;
  clearNotification: () => void;
}

const NotificationContext = createContext<NotificationState>({
  successMessage: null,
  error: null,
  notificationId: 0,
  setSuccess: noop,
  setError: noop,
  clearNotification: noop,
});

export const useNotification = () => useContext(NotificationContext);

function toProduct(product: { sku: string; title?: string | null; description?: string | null; brand?: string | null; category?: string | null; price: string }): Product {
  return {
    sku: product.sku,
    title: product.title ?? product.sku,
    description: product.description ?? "",
    brand: product.brand ?? "",
    category: product.category ?? "",
    price: product.price,
  };
}

function useCatalogState(initialProducts: Product[], initiallyUnavailable: boolean): CatalogState {
  const [products, setProducts] = useState(initialProducts);
  const [unavailable, setUnavailable] = useState(initiallyUnavailable);
  const [isLoading, setIsLoading] = useState(false);
  const requestRef = useRef(0);

  const reload = useCallback(() => {
    const request = ++requestRef.current;
    setIsLoading(true);
    void fetch("/api/products", { cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return (await parseJson(response, productsResponseSchema)).map(toProduct);
      })
      .then(
        (loaded) => {
          if (request !== requestRef.current) return;
          setProducts(loaded);
          setUnavailable(false);
        },
        () => {
          if (request !== requestRef.current) return;
          setProducts([]);
          setUnavailable(true);
        },
      )
      .finally(() => {
        if (request === requestRef.current) setIsLoading(false);
      });
  }, []);

  return useMemo(() => ({ products, unavailable, isLoading, reload }), [products, unavailable, isLoading, reload]);
}

function useCartState(initialLines: CartLine[]): CartState {
  const [lines, setLines] = useState(initialLines);
  const firstRender = useRef(true);

  useEffect(() => {
    // The first render shows the cookie as the server read it: nothing to write back.
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    saveCartCookie(lines);
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

  return useMemo(
    () => ({ lines, totalQuantity: lines.reduce((sum, line) => sum + line.quantity, 0), add, setQuantity, remove, removeMany }),
    [lines, add, setQuantity, remove, removeMany],
  );
}

function useNotificationState(): NotificationState {
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setErrorState] = useState<NotificationError | null>(null);
  const [notificationId, setNotificationId] = useState(0);
  const counterRef = useRef(0);

  const nextId = useCallback(() => {
    counterRef.current += 1;
    setNotificationId(counterRef.current);
  }, []);

  const clearNotification = useCallback(() => {
    setSuccessMessage(null);
    setErrorState(null);
  }, []);

  const setSuccess = useCallback(
    (message: string) => {
      setSuccessMessage(message);
      setErrorState(null);
      nextId();
    },
    [nextId],
  );

  const setError = useCallback(
    (value: NotificationError) => {
      setErrorState(value);
      setSuccessMessage(null);
      nextId();
    },
    [nextId],
  );

  return useMemo(
    () => ({ successMessage, error, notificationId, setSuccess, setError, clearNotification }),
    [successMessage, error, notificationId, setSuccess, setError, clearNotification],
  );
}

/** A new page starts without the previous page's notification. */
function ClearNotificationOnRouteChange({ clear }: Readonly<{ clear: () => void }>) {
  const pathname = usePathname();
  useEffect(() => {
    clear();
  }, [pathname, clear]);
  return null;
}

export interface StorefrontProvidersProps {
  children: ReactNode;
  session: SessionUser | null;
  products: Product[];
  catalogUnavailable: boolean;
  cart: CartLine[];
}

export function StorefrontProviders({ children, session, products, catalogUnavailable, cart }: Readonly<StorefrontProvidersProps>) {
  const catalog = useCatalogState(products, catalogUnavailable);
  const cartState = useCartState(cart);
  const notification = useNotificationState();

  return (
    <SessionContext.Provider value={session}>
      <CatalogContext.Provider value={catalog}>
        <CartContext.Provider value={cartState}>
          <NotificationContext.Provider value={notification}>
            <ClearNotificationOnRouteChange clear={notification.clearNotification} />
            {children}
          </NotificationContext.Provider>
        </CartContext.Provider>
      </CatalogContext.Provider>
    </SessionContext.Provider>
  );
}
