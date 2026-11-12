import type { Page } from '@playwright/test';
import type { Result } from '../../../../../common/result.js';
import { success, failure } from '../../../../../common/result.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';

export const PAGE_TIMEOUT_MS = 30_000;

const NOTIFICATION_SUCCESS_SELECTOR = "[role='alert'].notification.success";
const NOTIFICATION_ERROR_MESSAGE_SELECTOR = "[role='alert'].notification.error .error-message";
const CART_ITEM_COUNT_SELECTOR = "[aria-label='Cart Item Count']";

/** The parts of a DOM select the option check uses (the test project has no DOM typings). */
interface SelectControl {
  options: ArrayLike<{ value: string }>;
  add(option: unknown): void;
  ownerDocument: { createElement(tagName: string): { value: string; text: string } };
}

export abstract class BasePage {
  constructor(protected readonly page: Page) {}

  async getResult(previousNotificationId?: string): Promise<Result<string, SystemError>> {
    const baseSelector = previousNotificationId
      ? `[role='alert'].notification:not([data-notification-id='${previousNotificationId}'])`
      : "[role='alert'].notification";

    await this.page.locator(baseSelector).waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });

    const successNotification = this.page.locator(`[role='alert'].notification.success`);
    if ((await successNotification.count()) > 0) {
      const text = (await successNotification.textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? '';
      return success(text);
    }

    const errorNotification = this.page.locator(`[role='alert'].notification.error`);
    const errorMessage =
      (await errorNotification.locator('.error-message').textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? '';
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

  /** The header's cart badge; 0 when it is not shown. */
  async readCartItemCount(): Promise<number> {
    const badge = this.page.locator(CART_ITEM_COUNT_SELECTOR);
    if ((await badge.count()) === 0) return 0;
    return Number.parseInt(((await badge.textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim(), 10);
  }

  /** The notification showing now, without waiting for one: the success text or the error's message; else null. */
  protected async readShownNotification(): Promise<string | null> {
    for (const selector of [NOTIFICATION_SUCCESS_SELECTOR, NOTIFICATION_ERROR_MESSAGE_SELECTOR]) {
      const texts = await this.page.locator(selector).allTextContents();
      if (texts.length > 0) return (texts[0] ?? '').trim();
    }
    return null;
  }

  /** Selects the option with this value; a value the select does not offer is added first, as an edited form would send it. */
  protected async selectOptionByValueAddingIfMissing(selector: string, value: string): Promise<void> {
    const select = this.page.locator(selector);
    await select.evaluate(
      (element: SelectControl, optionValue: string): void => {
        if (!Array.from(element.options).some((option) => option.value === optionValue)) {
          const option = element.ownerDocument.createElement('option');
          option.value = optionValue;
          option.text = optionValue;
          element.add(option);
        }
      },
      value,
      { timeout: PAGE_TIMEOUT_MS },
    );
    await select.selectOption(value, { timeout: PAGE_TIMEOUT_MS });
  }
}
