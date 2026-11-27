using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

/// <summary>The catalog is public: every route is reachable without signing in.</summary>
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

    /// <summary>The product photo, proxied from the ERP so an img tag can load it from the shop's own origin.</summary>
    [HttpGet("{sku}/image")]
    public async Task<IActionResult> ViewProductImage(string sku)
    {
        var image = await _productService.GetProductImageAsync(sku);
        return File(image.Content, image.ContentType);
    }
}
