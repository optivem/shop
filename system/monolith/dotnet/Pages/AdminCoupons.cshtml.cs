using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;

namespace MyCompany.MyShop.Monolith.Pages
{
    [Authorize(Roles = Roles.Admin)]
    public class AdminCouponsModel : PageModel
    {
        public void OnGet() { }
    }
}
