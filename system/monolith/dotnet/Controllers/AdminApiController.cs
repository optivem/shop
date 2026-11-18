using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Services;

namespace MyCompany.MyShop.Monolith.Controllers;

[ApiController]
[Authorize(Roles = Roles.Admin)]
[Route("api/admin")]
public class AdminApiController : ControllerBase
{
    private readonly OrderService _orderService;

    public AdminApiController(OrderService orderService)
    {
        _orderService = orderService;
    }

    /// <summary>The usernames that have placed an order, distinct, A to Z (UC-011 customer filter).</summary>
    [HttpGet("customers")]
    public async Task<ActionResult<List<string>>> BrowseCustomers()
    {
        return Ok(await _orderService.BrowseCustomersAsync());
    }
}
