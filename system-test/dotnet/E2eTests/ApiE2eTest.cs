using System.Net;

namespace Optivem.Starter.SystemTest.E2eTests
{
    public class ApiE2eTest
    {
        [Fact]
        public async Task GetTodos_ShouldReturnTodoWithExpectedFormat()
        {
            // DISCLAIMER: This is an example of a badly written test
            // which unfortunately simulates real-life software test projects.
            // This is the starting point for our Starter exercises.

            // Arrange
            using var client = new HttpClient();

            // Act
            var response = await client.GetAsync("http://localhost:8080/api/todos/4");

            // Assert
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);

            var responseBody = await response.Content.ReadAsStringAsync();

            // Verify JSON structure contains expected fields
            Assert.Contains("\"userId\"", responseBody);
            Assert.Contains("\"id\"", responseBody);
            Assert.Contains("\"title\"", responseBody);
            Assert.Contains("\"completed\"", responseBody);

            // Verify the specific todo has id 4
            Assert.True(responseBody.Contains("\"id\":4") || responseBody.Contains("\"id\": 4"),
                       "Response should contain id with value 4");
        }
    }
}
