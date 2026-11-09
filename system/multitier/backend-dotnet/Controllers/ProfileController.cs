using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Api.Security;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

/// <summary>The caller's own profile; any signed-in user (the fallback policy rejects anonymous callers).</summary>
[ApiController]
[Route("api/account/profile")]
public class ProfileController : ControllerBase
{
    private readonly ProfileService _profileService;

    public ProfileController(ProfileService profileService)
    {
        _profileService = profileService;
    }

    [HttpGet]
    public async Task<IActionResult> ViewProfile()
    {
        var response = await _profileService.ViewProfileAsync(CurrentUserResolver.Resolve(User));
        return Ok(response);
    }

    [HttpPut]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
    {
        var response = await _profileService.UpdateProfileAsync(request, CurrentUserResolver.Resolve(User));
        return Ok(response);
    }
}
