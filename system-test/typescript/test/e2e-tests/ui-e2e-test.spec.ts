import { chromium, Browser, Page, Locator } from 'playwright';

describe('UI E2E Test', () => {
  it('fetchTodo_shouldDisplayTodoDataInUI', async () => {
    // DISCLAIMER: This is an example of a badly written test
    // which unfortunately simulates real-life software test projects.
    // This is the starting point for our Starter exercises.

    const browser: Browser = await chromium.launch();
    const page: Page = await browser.newPage();

    // Navigate to the todo page
    await page.goto('http://localhost:8080/todos');

    // 1. Check there's a textbox with id
    const todoIdInput: Locator = page.locator('#todoId');
    const isVisible = await todoIdInput.isVisible();
    expect(isVisible).toBe(true);

    // 2. Input value 4 into that textbox
    await todoIdInput.fill('4');

    // 3. Click "Fetch Todo" button
    const fetchButton: Locator = page.locator('#fetchTodo');
    await fetchButton.click();

    // 4. Wait for the result to appear and contain actual data
    const todoResult: Locator = page.locator('#todoResult');

    // Wait for the result div to become visible first
    await todoResult.waitFor({ timeout: 5000 });

    // Wait a bit more for the API call to complete and content to load
    await page.waitForTimeout(3000);

    const resultText = await todoResult.textContent();

    // Verify the todo data is displayed (more flexible checking)
    expect(resultText).toMatch(/User ID.*1/);
    expect(resultText).toMatch(/ID.*4/);
    expect(resultText).toMatch(/Title/);
    expect(resultText).toMatch(/Completed/);

    // Verify Completed field contains either Yes or No
    expect(resultText).toMatch(/Completed.*(?:Yes|No)/i);

    await browser.close();
  });
});
