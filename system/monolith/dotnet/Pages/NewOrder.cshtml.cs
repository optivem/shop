using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Services;

namespace MyCompany.MyShop.Monolith.Pages
{
    [Authorize(Roles = Roles.Customer)]
    public class NewOrderModel : PageModel
    {
        public const string FallbackCountry = "US";

        private readonly ProfileService _profileService;
        private readonly ILogger<NewOrderModel> _logger;

        public NewOrderModel(ProfileService profileService, ILogger<NewOrderModel> logger)
        {
            _profileService = profileService;
            _logger = logger;
        }

        /// <summary>Quick order's Country, prefilled from the customer's saved profile (UC-001 1a, UC-010 BR-5).</summary>
        public string DefaultCountry { get; private set; } = FallbackCountry;

        public async Task OnGetAsync()
        {
            try
            {
                var profile = await _profileService.ViewProfileAsync(CurrentUserResolver.Resolve(User));
                var saved = profile.Country?.Trim();
                DefaultCountry = string.IsNullOrEmpty(saved) ? FallbackCountry : saved;
            }
            catch (Exception e)
            {
                _logger.LogWarning("Profile unavailable for Quick order: {Message}", e.Message);
            }
        }
    }
}
