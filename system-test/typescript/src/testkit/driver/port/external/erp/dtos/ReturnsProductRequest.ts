export interface ReturnsProductRequest {
  sku: string;
  price: string;
  /** Unset → the driver's default title, so stub and real ERP stay in parity. */
  title?: string;
  description?: string;
  category?: string;
  brand?: string;
}
