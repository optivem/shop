using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Text.Json;

namespace Optivem.Starter.Monolith.Pages
{
    public class TodosModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly IConfiguration _configuration;

        public TodosModel(IHttpClientFactory httpClientFactory, IConfiguration configuration)
        {
            _httpClientFactory = httpClientFactory;
            _configuration = configuration;
        }

        [BindProperty]
        public int? TodoId { get; set; }

        public TodoItem? TodoResult { get; set; }
        public bool HasError { get; set; }
        public string? ErrorMessage { get; set; }

        public void OnGet()
        {
            // Just display the form
        }

        public async Task<IActionResult> OnPostAsync()
        {
            if (!TodoId.HasValue || TodoId <= 0)
            {
                HasError = true;
                ErrorMessage = "Please enter a valid todo ID.";
                return Page();
            }

            try
            {
                var httpClient = _httpClientFactory.CreateClient();
                var baseUrl = _configuration["ExternalApis:JsonPlaceholder"] ?? "https://jsonplaceholder.typicode.com";
                var response = await httpClient.GetAsync($"{baseUrl}/todos/{TodoId}");

                if (response.IsSuccessStatusCode)
                {
                    var jsonContent = await response.Content.ReadAsStringAsync();
                    TodoResult = JsonSerializer.Deserialize<TodoItem>(jsonContent, new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    });
                }
                else
                {
                    HasError = true;
                    ErrorMessage = $"Todo with ID {TodoId} not found.";
                }
            }
            catch (Exception ex)
            {
                HasError = true;
                ErrorMessage = $"Error fetching todo: {ex.Message}";
            }

            return Page();
        }
    }

    public class TodoItem
    {
        public int UserId { get; set; }
        public int Id { get; set; }
        public string Title { get; set; } = string.Empty;
        public bool Completed { get; set; }
    }
}