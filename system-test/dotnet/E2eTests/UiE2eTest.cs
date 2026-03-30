using Microsoft.Playwright;

namespace Optivem.Starter.SystemTest.E2eTests
{
    public class UiE2eTest
    {
        [Fact]
        public async Task FetchTodo_ShouldDisplayTodoDataInUI()
        {
            // DISCLAIMER: This is an example of a badly written test
            // which unfortunately simulates real-life software test projects.
            // This is the starting point for our Starter exercises.

            using var playwright = await Playwright.CreateAsync();
            await using var browser = await playwright.Chromium.LaunchAsync();
            var page = await browser.NewPageAsync();

            // Navigate to the todo page
            await page.GotoAsync("http://localhost:8080/todos");

            // 1. Check there's a textbox with id
            var todoIdInput = page.Locator("#todoId");
            await Assertions.Expect(todoIdInput).ToBeVisibleAsync();

            // 2. Input value 4 into that textbox
            await todoIdInput.FillAsync("4");

            // 3. Click "Fetch Todo" button (this will submit the form)
            var fetchButton = page.Locator("#fetchTodo");
            await fetchButton.ClickAsync();

            // 4. Wait for the page to reload and result to appear
            await page.WaitForURLAsync("**/todos");

            // Wait for the result div to appear
            var todoResult = page.Locator("#todoResult");
            await todoResult.WaitForAsync(new LocatorWaitForOptions { Timeout = 10000 });

            // Wait for the actual content to load (not just "Loading...")
            await Assertions.Expect(todoResult).ToContainTextAsync("ID", new() { Timeout = 10000 });

            var resultText = await todoResult.TextContentAsync();

            Assert.NotNull(resultText);

            // Verify the todo data is displayed with correct values
            Assert.Contains("ID", resultText);
            Assert.Contains("User ID", resultText);
            Assert.Contains("Title", resultText);
            Assert.Contains("Completed", resultText);

            // Verify the specific values
            Assert.True(resultText.Contains("ID") && resultText.Contains("4"),
                       $"Result should contain 'ID' with value 4. Actual text: {resultText}");
            Assert.True(resultText.Contains("User ID") && resultText.Contains("1"),
                       $"Result should contain 'User ID' with value 1. Actual text: {resultText}");
            Assert.True(resultText.Contains("Title") && !string.IsNullOrWhiteSpace(resultText),
                       $"Result should contain 'Title' with a value. Actual text: {resultText}");
            Assert.True(resultText.Contains("Completed") && (resultText.Contains("Yes") || resultText.Contains("No")),
                       $"Result should contain 'Completed' with Yes or No. Actual text: {resultText}");

            await browser.CloseAsync();
        }
    }
}
