// Shapes the storefront pages work with (storefront-ux §6.2, §8).

/** One catalog product as served by GET /api/products; the price is a 2-decimal string. */
export interface Product {
  sku: string;
  title: string;
  description: string;
  brand: string;
  category: string;
  price: string;
}

export interface ShippingAddress {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface CartLine {
  sku: string;
  quantity: number;
}
