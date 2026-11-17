import { expect } from '@playwright/test';
import type { AppContext } from '../app-context.js';

/**
 * Runs axe on the page the When step left open, on the same channel, and fails on any serious or critical violation
 * (FT Step 4). A dedicated assertion: scenarios ask for it, it never runs after every step.
 */
export async function expectPageIsAccessible(app: AppContext): Promise<void> {
  const result = await app.myShop('dynamic').checkAccessibility({});
  expect(result.success, JSON.stringify(result)).toBe(true);
  if (result.success) {
    expect(result.value.violations, 'serious or critical axe violations on the page').toEqual([]);
  }
}
