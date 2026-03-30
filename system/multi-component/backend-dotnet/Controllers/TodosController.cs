using Microsoft.AspNetCore.Mvc;

namespace Optivem.Starter.Backend.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TodosController : ControllerBase
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IConfiguration _configuration;

    public TodosController(IHttpClientFactory httpClientFactory, IConfiguration configuration)
    {
        _httpClientFactory = httpClientFactory;
        _configuration = configuration;
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> GetTodo(int id)
    {
        var baseUrl = _configuration["ExternalApis:JsonPlaceholder"] ?? "https://jsonplaceholder.typicode.com";
        var url = $"{baseUrl}/todos/{id}";

        const int maxRetries = 3;
        const int retryDelayMs = 1000;

        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            try
            {
                var httpClient = _httpClientFactory.CreateClient();
                var response = await httpClient.GetAsync(url);

                if (response.IsSuccessStatusCode)
                {
                    var jsonContent = await response.Content.ReadAsStringAsync();
                    return Content(jsonContent, "application/json");
                }

                if (response.StatusCode == System.Net.HttpStatusCode.NotFound)
                {
                    return NotFound(new { message = "Todo not found" });
                }

                // For other non-success status codes, return them directly without retrying
                return StatusCode((int)response.StatusCode, new { message = "Error retrieving todo" });
            }
            catch (HttpRequestException e)
            {
                // Only retry on network/connection errors (HttpRequestException)
                if (attempt == maxRetries - 1)
                {
                    return StatusCode(
                        StatusCodes.Status503ServiceUnavailable,
                        new { message = $"External API is unavailable after {maxRetries} attempts: {e.Message}" }
                    );
                }

                try
                {
                    await Task.Delay(retryDelayMs);
                }
                catch (TaskCanceledException)
                {
                    return StatusCode(
                        StatusCodes.Status500InternalServerError,
                        new { message = "Retry interrupted" }
                    );
                }
            }
            catch (TaskCanceledException)
            {
                return StatusCode(
                    StatusCodes.Status500InternalServerError,
                    new { message = "Request cancelled" }
                );
            }
        }

        return StatusCode(
            StatusCodes.Status500InternalServerError,
            new { message = "Unexpected error" }
        );
    }
}
