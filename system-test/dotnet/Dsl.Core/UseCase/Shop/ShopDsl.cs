using Driver.Port.Shop;
using Dsl.Core.Shop.UseCases;
using Dsl.Core.Shared;

namespace Dsl.Core.Shop;

public class ShopDsl : IAsyncDisposable
{
    private readonly IShopDriver _driver;
    private readonly UseCaseContext _context;

    private ShopDsl(IShopDriver driver, UseCaseContext context)
    {
        _driver = driver;
        _context = context;
    }

    public static Task<ShopDsl> CreateAsync(IShopDriver driver, UseCaseContext context)
    {
        return Task.FromResult(new ShopDsl(driver, context));
    }

    public async ValueTask DisposeAsync()
    {
        if (_driver != null)
            await _driver.DisposeAsync();
    }

    public GoToShop GoToShop() => new(_driver, _context);

    public PlaceOrder PlaceOrder() => new(_driver, _context);

    public ViewOrder ViewOrder() => new(_driver, _context);

    public PublishCoupon PublishCoupon() => new(_driver, _context);

    public BrowseCoupons BrowseCoupons() => new(_driver, _context);
}



