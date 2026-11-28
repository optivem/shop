using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

/// <summary>Public per-system reachability for the storefront's first-load "waking up" dialog.</summary>
[ApiController]
[AllowAnonymous]
[Route("api/status")]
public class StatusController(StatusService statusService) : ControllerBase
{
    [HttpGet("{system}")]
    public async Task<IActionResult> GetStatus(string system, CancellationToken cancellationToken)
    {
        var up = await statusService.IsUpAsync(system, cancellationToken);
        if (up is null)
        {
            return NotFound();
        }

        return StatusCode(up.Value ? StatusCodes.Status200OK : StatusCodes.Status503ServiceUnavailable,
            new { status = up.Value ? "UP" : "DOWN" });
    }
}
