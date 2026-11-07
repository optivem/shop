/**
 * Product details used when a scenario does not set them. Single source for the stub and the real ERP
 * (driver and client), so both stage identical products.
 */
export const ErpProductDefaults = {
  TITLE: 'Test Product Title',
  DESCRIPTION: 'Test Product Description',
  CATEGORY: 'Test Category',
  BRAND: 'Test Brand',
} as const;
