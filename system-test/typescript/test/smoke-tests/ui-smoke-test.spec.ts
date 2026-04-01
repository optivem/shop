import { chromium, Browser, Page, Response } from 'playwright';
import { loadConfiguration } from '../../config/configuration-loader';

describe('UI Smoke Test', () => {
  it('home_shouldReturnHtmlContent', async () => {
    // DISCLAIMER: This is an example of a badly written test
    // which unfortunately simulates real-life software test projects.
    // This is the starting point for our Starter exercises.

    const config = loadConfiguration();

    const browser: Browser = await chromium.launch();
    const page: Page = await browser.newPage();

    // Navigate and get response
    const response: Response | null = await page.goto(config.shop.frontendUrl);

    // Assert
    expect(response?.status()).toBe(200);

    // Check content type is HTML
    const contentType = response?.headers()['content-type'];
    expect(contentType).toBeDefined();
    expect(contentType).toContain('text/html');

    // Check HTML structure using Playwright's content method
    const pageContent = await page.content();
    expect(pageContent).toContain('<html');
    expect(pageContent).toContain('</html>');

    await browser.close();
  });
});
