import type { Page } from '@playwright/test';

const DEFAULT_TIMEOUT_MS = 5000;

export class PageClient {
  constructor(
    private readonly page: Page,
    private readonly timeoutMs: number = DEFAULT_TIMEOUT_MS
  ) {}

  async goto(url: string): Promise<void> {
    await this.page.goto(url);
  }

  async fill(selector: string, value: string): Promise<void> {
    await this.page.fill(selector, value, { timeout: this.timeoutMs });
  }

  async click(selector: string): Promise<void> {
    await this.page.click(selector, { timeout: this.timeoutMs });
  }

  async readTextContent(selector: string): Promise<string> {
    const text = await this.page.textContent(selector, { timeout: this.timeoutMs });
    return text ?? '';
  }

  async readAttribute(selector: string, attribute: string): Promise<string | null> {
    return this.page.getAttribute(selector, attribute, { timeout: this.timeoutMs });
  }

  async readAllTextContents(selector: string): Promise<string[]> {
    await this.page.waitForSelector(selector, { timeout: this.timeoutMs });
    return this.page.locator(selector).allTextContents();
  }

  async readAllTextContentsNoWait(selector: string): Promise<string[]> {
    return this.page.locator(selector).allTextContents();
  }

  async waitForVisible(selector: string): Promise<void> {
    await this.page.waitForSelector(selector, { state: 'visible', timeout: this.timeoutMs });
  }

  async isVisible(selector: string): Promise<boolean> {
    return this.page.locator(selector).isVisible();
  }

  async isHidden(selector: string): Promise<boolean> {
    return this.page.locator(selector).isHidden();
  }

  getPage(): Page {
    return this.page;
  }
}
