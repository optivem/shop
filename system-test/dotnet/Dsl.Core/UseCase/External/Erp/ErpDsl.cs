using Driver.Port.External.Erp;
using Dsl.Core.External.Erp.UseCases;
using Dsl.Core.Shared;


namespace Dsl.Core.External.Erp;

public class ErpDsl : IDisposable, IAsyncDisposable
{
    private readonly IErpDriver _driver;
    private readonly UseCaseContext _context;
    private bool _disposed;

    public ErpDsl(IErpDriver driver, UseCaseContext context)
    {
        _driver = driver;
        _context = context;
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    public async ValueTask DisposeAsync()
    {
        await _driver.DisposeAsync();
        _disposed = true;
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (_disposed) return;
        if (disposing)
            _driver?.Dispose();
        _disposed = true;
    }

    public GoToErp GoToErp() => new(_driver, _context);

    public ReturnsProduct ReturnsProduct() => new(_driver, _context);

    public ReturnsPromotion ReturnsPromotion() => new(_driver, _context);

    public ReturnsUnavailable ReturnsUnavailable() => new(_driver, _context);

    public ReturnsSlow ReturnsSlow() => new(_driver, _context);

    public ReturnsProductNotFound ReturnsProductNotFound() => new(_driver, _context);

    public ReturnsAvailable ReturnsAvailable() => new(_driver, _context);

    public GetProduct GetProduct() => new(_driver, _context);

    public ReturnsProducts ReturnsProducts() => new(_driver, _context);

    public GetProducts GetProducts() => new(_driver, _context);
}



