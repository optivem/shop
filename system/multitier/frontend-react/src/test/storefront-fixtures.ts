// Shared fixtures for the storefront unit and interaction tests.
import { vi } from 'vitest';
import type { CatalogContextType } from '../contexts/catalog-context';
import type { Product } from '../types/api.types';

export const CATALOG: Product[] = [
  { sku: 'HP-15', title: 'HP Laptop 15', brand: 'HP', category: 'Laptops', price: '699.99', description: 'Everyday 15-inch laptop' },
  { sku: 'DELL-XPS', title: 'Dell XPS 13', brand: 'Dell', category: 'Laptops', price: '1299.99', description: 'Compact 13-inch ultrabook' },
  { sku: 'LENOVO-T14', title: 'Lenovo ThinkPad T14', brand: 'Lenovo', category: 'Laptops', price: '999.99', description: 'Business laptop' },
  { sku: 'DELL-U2723QE', title: 'Dell UltraSharp 27 4K', brand: 'Dell', category: 'Monitors', price: '579.99', description: '27-inch USB-C monitor' },
  { sku: 'LG-27UL500', title: 'LG 27-inch 4K UHD Monitor', brand: 'LG', category: 'Monitors', price: '299.99', description: 'HDR10 monitor' },
  { sku: 'LOGI-MX3S', title: 'Logitech MX Master 3S', brand: 'Logitech', category: 'Accessories', price: '99.99', description: 'Wireless mouse' },
];

export function catalogValue(overrides: Partial<CatalogContextType> = {}): CatalogContextType {
  return { products: CATALOG, isLoading: false, error: null, reload: vi.fn(), ...overrides };
}
