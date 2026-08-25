using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

[ApiController]
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
