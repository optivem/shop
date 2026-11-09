using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Api.Security;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

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
    public async Task<IActionResult> BrowseOrderHistory(
        [FromQuery] string? orderNumber, [FromQuery] int? page, [FromQuery] int? size, [FromQuery] string? customer)
    {
        var response = await _orderService.BrowseOrderHistoryAsync(
            orderNumber, CurrentUserResolver.Resolve(User), page, size, customer);
        return Ok(response);
    }

    [Authorize(Roles = Roles.Customer)]
    [HttpPost]
    public async Task<IActionResult> PlaceOrder(
        [FromBody] PlaceOrderRequest request,
        [FromHeader(Name = OrderService.IdempotencyKeyField)] string? idempotencyKey)
    {
        // A replay answers exactly like the first call: 201, the same Location and the same body.
        var response = await _orderService.PlaceOrderAsync(request, CurrentUserResolver.Resolve(User), idempotencyKey);
        return Created($"/api/orders/{response.OrderNumber}", response);
    }

    [HttpGet("{orderNumber}")]
    public async Task<IActionResult> GetOrder(string orderNumber)
    {
        var response = await _orderService.GetOrderAsync(orderNumber, CurrentUserResolver.Resolve(User));
        return Ok(response);
    }

    [HttpPost("{orderNumber}/cancel")]
    public async Task<IActionResult> CancelOrder(string orderNumber)
    {
        await _orderService.CancelOrderAsync(orderNumber, CurrentUserResolver.Resolve(User));
        return NoContent();
    }

    [Authorize(Roles = Roles.Admin)]
    [HttpPost("{orderNumber}/deliver")]
    public async Task<IActionResult> DeliverOrder(string orderNumber)
    {
        await _orderService.DeliverOrderAsync(orderNumber);
        return NoContent();
    }
}
