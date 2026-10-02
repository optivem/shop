using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MyCompany.MyShop.Monolith.Controllers;

[ApiController]
[Route("")]
public class HealthController : ControllerBase
{
    [AllowAnonymous]
    [HttpGet("health")]
    public IActionResult CheckHealth()
    {
        return Ok(new { status = "UP" });
    }
}
