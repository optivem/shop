using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Services;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>
    /// `/account` (My account › Profile &amp; address, UC-010): name and default shipping address. The page saves
    /// through <c>PUT /api/account/profile</c>, which validates and reports each field to correct.
    /// </summary>
    public class AccountModel : PageModel
    {
        private readonly ProfileService _profileService;

        public AccountModel(ProfileService profileService) => _profileService = profileService;

        public ViewProfileResponse Profile { get; private set; } = new();

        public async Task OnGetAsync() =>
            Profile = await _profileService.ViewProfileAsync(CurrentUserResolver.Resolve(User));
    }
}
