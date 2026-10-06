using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;

namespace MyCompany.MyShop.Monolith.Pages
{
    [Authorize(Roles = Roles.Customer)]
    public class NewOrderModel : PageModel
    {
        public void OnGet()
        {
        }
    }
}
