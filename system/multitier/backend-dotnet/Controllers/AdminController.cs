using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Api.Security;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

[ApiController]
[Authorize(Roles = Roles.Admin)]
[Route("api/admin")]
public class AdminController : ControllerBase
{
    private readonly OrderService _orderService;

    public AdminController(OrderService orderService)
    {
        _orderService = orderService;
    }

    [HttpPost("recall/{sku}")]
    public async Task<IActionResult> RecallSku(string sku)
    {
        var response = await _orderService.RecallSkuAsync(sku);
        return Ok(response);
    }
}
