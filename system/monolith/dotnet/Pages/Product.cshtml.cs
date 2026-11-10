using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>`/products/{sku}` (storefront-ux §4.2, UC-007): public; an unknown SKU is a 404 page.</summary>
    [AllowAnonymous]
    public class ProductModel : PageModel
    {
        private readonly ProductService _productService;
        private readonly ILogger<ProductModel> _logger;

        public ProductModel(ProductService productService, ILogger<ProductModel> logger)
        {
            _productService = productService;
            _logger = logger;
        }

        public ViewProductDetailsResponse? Product { get; private set; }

        public bool IsNotFound { get; private set; }

        public async Task<IActionResult> OnGetAsync(string sku)
        {
            try
            {
                Product = await _productService.ViewProductAsync(sku);
            }
            catch (NotExistValidationException)
            {
                IsNotFound = true;
                Response.StatusCode = StatusCodes.Status404NotFound;
            }
            catch (Exception e)
            {
                _logger.LogWarning("Product {Sku} unavailable: {Message}", sku, e.Message);
            }

            return Page();
        }
    }
}
