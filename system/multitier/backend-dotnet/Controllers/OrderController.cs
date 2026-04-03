using Microsoft.AspNetCore.Mvc;
using Optivem.Shop.Backend.Core.Dtos;
using Optivem.Shop.Backend.Core.Services;

namespace Optivem.Shop.Backend.Controllers;

[ApiController]
[Route("api/orders")]
public class OrderController : ControllerBase
{
    private readonly OrderService _orderService;

    public OrderController(OrderService orderService)
    {
        _orderService = orderService;
    }

    [HttpGet]
    public async Task<IActionResult> BrowseOrderHistory([FromQuery] string? orderNumber)
    {
        var response = await _orderService.BrowseOrderHistoryAsync(orderNumber);
        return Ok(response);
    }

    [HttpPost]
    public async Task<IActionResult> PlaceOrder([FromBody] PlaceOrderRequest request)
    {
        var response = await _orderService.PlaceOrderAsync(request);
        return Created($"/api/orders/{response.OrderNumber}", response);
    }

    [HttpGet("{orderNumber}")]
    public async Task<IActionResult> GetOrder(string orderNumber)
    {
        var response = await _orderService.GetOrderAsync(orderNumber);
        return Ok(response);
    }
}
