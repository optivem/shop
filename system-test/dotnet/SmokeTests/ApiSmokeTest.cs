using System.Net;

namespace Optivem.Starter.SystemTest.SmokeTests
{
    public class ApiSmokeTest
    {
        [Fact]
        public async Task Echo_ShouldReturn200OK()
        {
            // DISCLAIMER: This is an example of a badly written test
            // which unfortunately simulates real-life software test projects.
            // This is the starting point for our Starter exercises.

            using var client = new HttpClient();
            var response = await client.GetAsync("http://localhost:8080/api/echo");

            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        }
    }
}
