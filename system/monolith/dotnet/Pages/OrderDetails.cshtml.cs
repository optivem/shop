using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace MyCompany.MyShop.Monolith.Pages
{
    public class OrderDetailsModel : PageModel
    {
        /// <summary>The order this page shows, from the `/order-details/{orderNumber}` route.</summary>
        [BindProperty(SupportsGet = true)]
        public string OrderNumber { get; set; } = string.Empty;

        public void OnGet()
        {
        }
    }
}
