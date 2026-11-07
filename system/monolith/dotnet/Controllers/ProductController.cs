using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Monolith.Core.Services;

namespace MyCompany.MyShop.Monolith.Controllers;

/// <summary>The catalog is public: both routes are reachable without signing in.</summary>
[ApiController]
[AllowAnonymous]
[Route("api/products")]
public class ProductController : ControllerBase
{
    private readonly ProductService _productService;

    public ProductController(ProductService productService)
    {
        _productService = productService;
    }

    [HttpGet]
    public async Task<IActionResult> BrowseProducts()
    {
        var response = await _productService.BrowseProductsAsync();
        return Ok(response);
    }

    [HttpGet("{sku}")]
    public async Task<IActionResult> ViewProduct(string sku)
    {
        var response = await _productService.ViewProductAsync(sku);
        return Ok(response);
    }
}
