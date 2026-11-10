using Driver.Port.External.Tax;
using Dsl.Core.External.Tax.UseCases;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Tax;

public class TaxDsl : IDisposable, IAsyncDisposable
{
    private readonly ITaxDriver _driver;
    private readonly UseCaseContext _context;
    private bool _disposed;

    public TaxDsl(ITaxDriver driver, UseCaseContext context)
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

    public GoToTax GoToTax() => new(_driver, _context);
    public ReturnsTaxRate ReturnsTaxRate() => new(_driver, _context);
    public GetTaxRate GetTaxRate() => new(_driver, _context);
}
