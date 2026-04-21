import type { Page } from 'playwright';
import type { Result } from '../../../../../../common/result.js';
import { success, failure } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../port/shop/dtos/errors/SystemError.js';

export const PAGE_TIMEOUT_MS = 30_000;

export abstract class BasePage {
  constructor(protected readonly page: Page) {}

  async getResult(previousNotificationId?: string): Promise<Result<string, SystemError>> {
    const baseSelector = previousNotificationId
      ? `[role='alert'].notification:not([data-notification-id='${previousNotificationId}'])`
      : "[role='alert'].notification";

    await this.page.locator(baseSelector).waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });

    const successNotification = this.page.locator(`[role='alert'].notification.success`);
    if ((await successNotification.count()) > 0) {
      const text = (await successNotification.textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '';
      return success(text);
    }

    const errorNotification = this.page.locator(`[role='alert'].notification.error`);
    const errorMessage =
      (await errorNotification.locator('.error-message').textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '';
    const fieldErrorTexts = await errorNotification.locator('.field-error').allTextContents();
    const fieldErrors = fieldErrorTexts.map((text) => {
      const colonIndex = text.indexOf(':');
      if (colonIndex >= 0) {
        return { field: text.substring(0, colonIndex).trim(), message: text.substring(colonIndex + 1).trim() };
      }
      return { field: '', message: text.trim() };
    });

    return failure({ message: errorMessage, fieldErrors });
  }
}
